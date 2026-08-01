package com.girvikhata.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.girvikhata.app.domain.InterestEngine
import com.girvikhata.app.domain.InterestMode
import com.girvikhata.app.domain.InterestPeriodRule
import com.girvikhata.app.domain.InterestQuote
import com.girvikhata.app.domain.InterestTerms
import kotlin.math.roundToLong

data class InterestUiState(
    val mode: InterestMode = InterestMode.PERCENT_PER_MONTH,
    val monthlyRatePercent: String = "2",
    val flatMonthlyRupees: String = "",
    val periodRule: InterestPeriodRule = InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS,
    val compoundEnabled: Boolean = false,
    val compoundEveryMonths: Int = 1,
)

fun InterestUiState.toTermsOrNull(): InterestTerms? = runCatching {
    when (mode) {
        InterestMode.PERCENT_PER_MONTH -> InterestTerms(
            mode = mode,
            monthlyRateBasisPoints = ((monthlyRatePercent.toDoubleOrNull() ?: return null) * 100).roundToLong().toInt(),
            periodRule = periodRule,
            compoundEveryMonths = compoundEveryMonths.takeIf { compoundEnabled },
        )
        InterestMode.FLAT_PER_MONTH -> InterestTerms(
            mode = mode,
            flatMonthlyChargePaise = ((flatMonthlyRupees.toDoubleOrNull() ?: return null) * 100).roundToLong(),
            periodRule = periodRule,
            compoundEveryMonths = null,
        )
    }
}.getOrNull()

fun interestPreview(
    state: InterestUiState,
    principalRupees: String,
    startAtMillis: Long,
    previewAtMillis: Long,
): InterestQuote? {
    val principalPaise = principalRupees.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: return null
    if (principalPaise < 0) return null
    val terms = state.toTermsOrNull() ?: return null
    return runCatching { InterestEngine.quote(principalPaise, startAtMillis, previewAtMillis, terms) }.getOrNull()
}

@Composable
fun InterestEntrySection(
    state: InterestUiState,
    onStateChange: (InterestUiState) -> Unit,
    principalRupees: String,
    startAtMillis: Long,
    previewAtMillis: Long,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ब्याज का तरीका / Interest Method", fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { onStateChange(state.copy(mode = InterestMode.PERCENT_PER_MONTH)) }) {
                Text(if (state.mode == InterestMode.PERCENT_PER_MONTH) "✓ % प्रति माह" else "% प्रति माह")
            }
            TextButton(onClick = {
                onStateChange(state.copy(mode = InterestMode.FLAT_PER_MONTH, compoundEnabled = false))
            }) {
                Text(if (state.mode == InterestMode.FLAT_PER_MONTH) "✓ Flat / माह" else "Flat / माह")
            }
        }

        if (state.mode == InterestMode.PERCENT_PER_MONTH) {
            OutlinedTextField(
                value = state.monthlyRatePercent,
                onValueChange = { onStateChange(state.copy(monthlyRatePercent = decimalText(it))) },
                label = { Text("मासिक ब्याज % / Monthly %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        } else {
            OutlinedTextField(
                value = state.flatMonthlyRupees,
                onValueChange = { onStateChange(state.copy(flatMonthlyRupees = decimalText(it))) },
                label = { Text("Flat charge ₹ प्रति माह / month") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        Text("अवधि का हिसाब / Period Calculation", fontWeight = FontWeight.Bold)
        periodButton(state, InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS, "पूरा महीना + बाकी दिन / Months + days", onStateChange)
        periodButton(state, InterestPeriodRule.EXACT_DAYS, "हर दिन का सही हिसाब / Exact days", onStateChange)
        periodButton(state, InterestPeriodRule.FULL_MONTH_STARTED, "अधूरा महीना भी पूरा / Started month = full", onStateChange)

        if (state.mode == InterestMode.PERCENT_PER_MONTH) {
            Row {
                Checkbox(
                    checked = state.compoundEnabled,
                    onCheckedChange = { onStateChange(state.copy(compoundEnabled = it)) },
                )
                Column {
                    Text("चक्रवृद्धि ब्याज / Compound interest")
                    Text("Optional advanced setting", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.compoundEnabled) {
                Text("ब्याज मूलधन में कब जुड़े? / Capitalize every", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(1, 2, 3, 6).forEach { months ->
                        TextButton(onClick = { onStateChange(state.copy(compoundEveryMonths = months)) }) {
                            Text(if (state.compoundEveryMonths == months) "✓ ${months}m" else "${months}m")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(12, 24, 36).forEach { months ->
                        TextButton(onClick = { onStateChange(state.copy(compoundEveryMonths = months)) }) {
                            val years = months / 12
                            Text(if (state.compoundEveryMonths == months) "✓ ${years}y" else "${years}y")
                        }
                    }
                }
            }
        }

        val terms = state.toTermsOrNull()
        if (terms != null) {
            val principalPaise = principalRupees.toDoubleOrNull()?.let { (it * 100).roundToLong() }
            if (principalPaise != null && principalPaise >= 0) {
                val monthly = InterestEngine.monthlyChargePaise(principalPaise, terms)
                Text("प्रति माह / Monthly: ${money(monthly)}", fontWeight = FontWeight.Bold)
            }
        }

        val quote = interestPreview(state, principalRupees, startAtMillis, previewAtMillis)
        if (quote != null) {
            Text("Preview: ${quote.elapsedDays} दिन • ब्याज ${money(quote.interestPaise)} • कुल ${money(quote.totalPayablePaise)}")
            if (quote.compoundPeriodsApplied > 0) {
                Text("Compound periods: ${quote.compoundPeriodsApplied}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun periodButton(
    state: InterestUiState,
    rule: InterestPeriodRule,
    label: String,
    onStateChange: (InterestUiState) -> Unit,
) {
    TextButton(onClick = { onStateChange(state.copy(periodRule = rule)) }) {
        Text(if (state.periodRule == rule) "✓ $label" else label)
    }
}

private fun money(paise: Long): String = "₹%.2f".format(paise / 100.0)

private fun decimalText(value: String): String {
    val filtered = value.filter { it.isDigit() || it == '.' }
    val dot = filtered.indexOf('.')
    return if (dot < 0) filtered else filtered.substring(0, dot + 1) + filtered.substring(dot + 1).replace(".", "")
}
