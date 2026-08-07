package com.girvikhata.app

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.BlueprintKhataRepository

@Composable
internal fun CustomerListPanel(
    snapshot: AppSnapshot,
    repository: BlueprintKhataRepository,
    updateSnapshot: (AppSnapshot) -> Unit,
    openGirvi: (String) -> Unit,
) = CustomerListPage("Customers") {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Naam / Mobile / Item / Girvi search") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    val q = query.trim().lowercase()
    val customerRows = snapshot.customers.filter { customer ->
        q.isBlank() || customer.name.lowercase().contains(q) || customer.mobile.contains(q) || customer.address.lowercase().contains(q) ||
            snapshot.girvis.filter { it.customerId == customer.id }.any { girvi ->
                girvi.girviNumber.lowercase().contains(q) || girvi.effectiveItems.any { it.itemName.lowercase().contains(q) }
            }
    }.sortedBy { it.name.lowercase() }

    if (customerRows.isEmpty()) {
        Card(Modifier.fillMaxWidth()) {
            Text(
                if (query.isBlank()) "Abhi koi customer nahi hai. Naya Girvi banate hi customer yahan aa jayega."
                else "Koi customer nahi mila. Naam, mobile ya Girvi number check karein.",
                Modifier.padding(16.dp),
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(customerRows, key = { it.id }) { customer ->
                val girvis = snapshot.girvis.filter { it.customerId == customer.id }
                val active = girvis.count { it.status == "ACTIVE" }
                Card(
                    Modifier.fillMaxWidth().clickable {
                        context.startActivity(
                            Intent(context, CustomerProfileActivity::class.java)
                                .putExtra(CustomerProfileActivity.EXTRA_CUSTOMER_ID, customer.id),
                        )
                    },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(customer.mobile.ifBlank { "Mobile nahi diya" }, color = Color.Gray)
                            Text("$active Active • ${girvis.size} Total", color = if (active > 0) Color(0xFF138A4A) else Color.Gray)
                        }
                        Text("Khata ›", color = Color(0xFF5146B8), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerListPage(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        content()
    }
}
