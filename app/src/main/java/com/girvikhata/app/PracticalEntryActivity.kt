package com.girvikhata.app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.ClassicVerifiedWriteGateway
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.VerifiedBusinessWriteCoordinator
import com.girvikhata.app.domain.CustomerCandidate
import com.girvikhata.app.domain.CustomerMatcher
import com.girvikhata.app.domain.GirviInterestMetadata
import com.girvikhata.app.domain.GirviSequence
import com.girvikhata.app.domain.InterestMode
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.math.roundToLong

class PracticalEntryActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val records = EncryptedRecordStore(applicationContext)
        val coordinator = VerifiedBusinessWriteCoordinator(applicationContext, records = records)
        val gateway = ClassicVerifiedWriteGateway(records = records, coordinator = coordinator)
        setContent {
            MaterialTheme {
                var snapshot by remember { mutableStateOf(records.load()) }
                PracticalEntryScreen(
                    snapshot = snapshot,
                    onBack = ::finish,
                    onSave = { customer, girvi ->
                        runCatching {
                            val authoritativeBefore = records.load()
                            val customers = if (authoritativeBefore.customers.any { it.id == customer.id }) {
                                authoritativeBefore.customers.map { if (it.id == customer.id) customer else it }
                            } else authoritativeBefore.customers + customer
                            val saved = gateway.persist(
                                authoritativeBefore,
                                authoritativeBefore.copy(customers = customers, girvis = authoritativeBefore.girvis + girvi),
                            )
                            check(saved.girvis.any { it.id == girvi.id }) { "authoritative reload missing new girvi" }
                            check(saved.customers.any { it.id == customer.id }) { "authoritative reload missing customer" }
                            snapshot = saved
                        }.fold(
                            onSuccess = {
                                setResult(Activity.RESULT_OK)
                                finish()
                                null
                            },
                            onFailure = { failure ->
                                val intent = runCatching { coordinator.latestIntent() }.getOrNull()
                                val intentInfo = intent?.let {
                                    " intent=${it.state}/${it.mutationLabel}/${it.transactionId.take(8)}"
                                }.orEmpty()
                                "SAVE-25-REBUILD: ${failure.message ?: failure::class.java.simpleName}$intentInfo"
                            },
                        )
                    },
                )
            }
        }
    }
}

private data class PracticalItemDraft(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "",
    val name: String = "",
    val quantity: String = "1",
    val weight: String = "",
    val unit: String = "Gram / ग्राम",
    val customUnit: String = "",
    val gross: String = "",
    val deduction: String = "",
    val description: String = "",
    val advancedWeight: Boolean = false,
    val photoPath: String = "",
)

private enum class PhotoTarget { CUSTOMER, ITEM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticalEntryScreen(
    snapshot: AppSnapshot,
    onBack: () -> Unit,
    onSave: (CustomerRecord, GirviRecord) -> String?,
) {
    val context = LocalContext.current
    var customerName by rememberSaveable { mutableStateOf("") }
    var selectedCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var mobile by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var customerPhotoPath by rememberSaveable { mutableStateOf("") }
    var pledgeDate by rememberSaveable { mutableStateOf(startOfToday()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var interestState by remember { mutableStateOf(InterestUiState()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showReview by rememberSaveable { mutableStateOf(false) }
    var pendingPhotoTarget by remember { mutableStateOf<PhotoTarget?>(null) }
    var pendingItemIndex by remember { mutableStateOf(-1) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoPath by remember { mutableStateOf("") }
    val categories = snapshot.categories.filter { it.active }.map { it.name }
    val firstCategory = categories.firstOrNull().orEmpty()
    val items = remember { mutableStateListOf(PracticalItemDraft(category = firstCategory)) }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            readSelectedPhoneContact(context, uri)?.let { contact ->
                customerName = contact.first
                mobile = contact.second
                selectedCustomerId = CustomerMatcher.findBestMatch(
                    snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) },
                    customerName,
                    mobile,
                )?.id
            }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            when (pendingPhotoTarget) {
                PhotoTarget.CUSTOMER -> customerPhotoPath = pendingPhotoPath
                PhotoTarget.ITEM -> if (pendingItemIndex in items.indices) {
                    items[pendingItemIndex] = items[pendingItemIndex].copy(photoPath = pendingPhotoPath)
                }
                null -> Unit
            }
        } else if (pendingPhotoPath.isNotBlank()) File(pendingPhotoPath).delete()
        pendingPhotoTarget = null
        pendingItemIndex = -1
        pendingPhotoUri = null
        pendingPhotoPath = ""
    }

    fun takePhoto(target: PhotoTarget, itemIndex: Int = -1) {
        runCatching {
            val file = PrivateMediaVault.newPhotoFile(context, if (target == PhotoTarget.CUSTOMER) "customer" else "item")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingPhotoTarget = target
            pendingItemIndex = itemIndex
            pendingPhotoUri = uri
            pendingPhotoPath = file.absolutePath
            camera.launch(uri)
        }.onFailure { error = "PHOTO-25: ${it.message ?: it::class.java.simpleName}" }
    }

    val duplicateMatch = remember(snapshot.customers, customerName, mobile) {
        CustomerMatcher.findBestMatch(
            snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) },
            customerName,
            mobile,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("नया गिरवी / New Girvi") },
                navigationIcon = { TextButton(onClick = onBack) { Text("वापस / Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SectionCard("ग्राहक / Customer") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it; selectedCustomerId = null },
                                label = { Text("नाम / Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            IconButton(onClick = {
                                runCatching {
                                    contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                                }.onFailure { error = "CONTACT-25: ${it.message ?: it::class.java.simpleName}" }
                            }) {
                                Icon(Icons.Default.Contacts, contentDescription = "Contact se customer laayein")
                            }
                            IconButton(onClick = { takePhoto(PhotoTarget.CUSTOMER) }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Customer live photo")
                            }
                        }
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = normalizeMobileInput(it); selectedCustomerId = null },
                            label = { Text("मोबाइल / Mobile") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("पता / Address") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        duplicateMatch?.let {
                            Text(
                                "मौजूदा ग्राहक मिला / Existing customer matched: ${it.name}${it.mobile.takeIf(String::isNotBlank)?.let { m -> " • $m" }.orEmpty()}",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (customerPhotoPath.isNotBlank()) {
                            Text("✓ ग्राहक की live photo सुरक्षित / Customer photo captured", color = MaterialTheme.colorScheme.primary)
                            Row {
                                TextButton(onClick = { takePhoto(PhotoTarget.CUSTOMER) }) { Text("फिर फोटो / Retake") }
                                TextButton(onClick = { File(customerPhotoPath).delete(); customerPhotoPath = "" }) { Text("फोटो हटाएँ / Remove") }
                            }
                        } else Text("फोटो optional है / Photo is optional")
                    }
                }

                item {
                    SectionCard("तारीख, राशि और ब्याज / Date, Amount & Interest") {
                        OutlinedButton(
                            onClick = {
                                showDatePicker(context, pledgeDate) { selected ->
                                    if (selected <= endOfToday()) pledgeDate = selected
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.CalendarMonth, null)
                            Text("  ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(pledgeDate))}")
                        }
                        Text("पुरानी तारीख allowed है; future date नहीं / Back-date allowed; future date blocked")
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = decimalInput(it) },
                            label = { Text("मूल राशि ₹ / Principal ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        InterestEntrySection(
                            state = interestState,
                            onStateChange = { interestState = it },
                            principalRupees = amount,
                            startAtMillis = pledgeDate,
                            previewAtMillis = endOfToday(),
                        )
                    }
                }

                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    PracticalItemEditor(
                        index = index,
                        item = item,
                        categories = categories,
                        removable = items.size > 1,
                        onChange = { items[index] = it },
                        onRemove = {
                            if (item.photoPath.isNotBlank()) File(item.photoPath).delete()
                            items.removeAt(index)
                        },
                        onPhoto = { takePhoto(PhotoTarget.ITEM, index) },
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { items.add(PracticalItemDraft(category = firstCategory)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, null)
                        Text(" एक और सामान / Add Item")
                    }
                    error?.let {
                        Card(Modifier.fillMaxWidth()) {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = {
                            error = validatePracticalEntry(customerName, amount, interestState, pledgeDate, items)
                            if (error == null) showReview = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("जाँचें और सेव करें / Review & Save") }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showReview) {
        val principal = amount.toDoubleOrNull() ?: 0.0
        val terms = interestState.toTermsOrNull()
        val preview = interestPreview(interestState, amount, pledgeDate, endOfToday())
        AlertDialog(
            onDismissRequest = { showReview = false },
            title = { Text("एंट्री जाँचें / Review Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ग्राहक / Customer: ${customerName.trim()}")
                    Text("मोबाइल / Mobile: ${mobile.ifBlank { "—" }}")
                    Text("तारीख / Date: ${DateFormat.getDateInstance().format(Date(pledgeDate))}")
                    Text("सामान / Items: ${items.size}")
                    Text("मूलधन / Principal: ₹${"%.2f".format(principal)}")
                    Text(interestReviewLabel(interestState))
                    preview?.let { Text("आज तक preview / Today: ब्याज ₹${"%.2f".format(it.interestPaise / 100.0)} • कुल ₹${"%.2f".format(it.totalPayablePaise / 100.0)}") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val failureMessage = runCatching {
                        val saveTerms = terms ?: error("Interest terms invalid")
                        val existing = selectedCustomerId?.let { id -> snapshot.customers.firstOrNull { it.id == id } }
                            ?: CustomerMatcher.findBestMatch(
                                snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) },
                                customerName,
                                mobile,
                            )?.let { match -> snapshot.customers.firstOrNull { it.id == match.id } }
                        val customer = (existing ?: CustomerRecord(name = customerName.trim())).copy(
                            name = customerName.trim(),
                            mobile = normalizeMobileInput(mobile),
                            address = address.trim(),
                        )
                        PrivateMediaVault.attachCustomerPhoto(context, customer.id, customerPhotoPath)
                        val records = items.mapIndexed { index, draft ->
                            PrivateMediaVault.attachItemPhoto(context, draft.id, draft.photoPath)
                            val unitText = if (draft.unit == CUSTOM_UNIT) draft.customUnit.trim() else draft.unit
                            val baseDescription = buildString {
                                append(draft.description.trim())
                                if (!draft.advancedWeight && unitText.isNotBlank()) {
                                    if (isNotBlank()) append(" • ")
                                    append("Weight unit: $unitText")
                                }
                            }
                            GirviItemRecord(
                                id = draft.id,
                                categoryName = draft.category,
                                itemName = draft.name.trim(),
                                quantity = draft.quantity.toInt(),
                                grossWeightGrams = if (draft.advancedWeight) draft.gross else draft.weight,
                                deductionWeightGrams = if (draft.advancedWeight) draft.deduction else "",
                                description = if (index == 0) GirviInterestMetadata.attach(baseDescription, saveTerms) else baseDescription,
                            )
                        }
                        val first = records.first()
                        onSave(
                            customer,
                            GirviRecord(
                                girviNumber = GirviSequence.nextNumber(snapshot.girvis.map { it.girviNumber }),
                                customerId = customer.id,
                                customerName = customer.name,
                                categoryName = first.categoryName,
                                itemName = first.itemName,
                                weightGrams = first.grossWeightGrams,
                                principalPaise = (principal * 100).roundToLong(),
                                monthlyRateBasisPoints = if (saveTerms.mode == InterestMode.PERCENT_PER_MONTH) saveTerms.monthlyRateBasisPoints else 0,
                                createdAt = pledgeDate,
                                items = records,
                            ),
                        )
                    }.fold(
                        onSuccess = { it },
                        onFailure = { "SAVE-25-BUILD: ${it.message ?: it::class.java.simpleName}" },
                    )
                    if (failureMessage != null) {
                        error = failureMessage
                        showReview = false
                    }
                }) { Text("पुष्टि और सेव / Confirm Save") }
            },
            dismissButton = { TextButton(onClick = { showReview = false }) { Text("बदलें / Edit") } },
        )
    }
}

@Composable
private fun PracticalItemEditor(
    index: Int,
    item: PracticalItemDraft,
    categories: List<String>,
    removable: Boolean,
    onChange: (PracticalItemDraft) -> Unit,
    onRemove: () -> Unit,
    onPhoto: () -> Unit,
) {
    SectionCard("सामान ${index + 1} / Item ${index + 1}") {
        Row {
            Text("विवरण / Details", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            IconButton(onClick = onPhoto) { Icon(Icons.Default.CameraAlt, contentDescription = "Item live photo") }
            if (removable) IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove item") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            categories.take(4).forEach { category ->
                TextButton(onClick = { onChange(item.copy(category = category)) }) {
                    Text(if (item.category == category) "✓ $category" else category)
                }
            }
        }
        OutlinedTextField(
            value = item.name,
            onValueChange = { onChange(item.copy(name = it)) },
            label = { Text("सामान का नाम / Item name") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = item.quantity,
            onValueChange = { onChange(item.copy(quantity = it.filter(Char::isDigit).take(3))) },
            label = { Text("संख्या / Quantity") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = item.advancedWeight, onCheckedChange = { onChange(item.copy(advancedWeight = it)) })
            Text("वजन का पूरा विवरण / Advanced weight")
        }
        if (item.advancedWeight) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.gross,
                    onValueChange = { onChange(item.copy(gross = decimalInput(it))) },
                    label = { Text("Gross g") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = item.deduction,
                    onValueChange = { onChange(item.copy(deduction = decimalInput(it))) },
                    label = { Text("कटौती / Less g") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            val gross = item.gross.toDoubleOrNull() ?: 0.0
            val deduction = item.deduction.toDoubleOrNull() ?: 0.0
            Text("शुद्ध वजन / Net: ${"%.3f".format((gross - deduction).coerceAtLeast(0.0))} g")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.weight,
                    onValueChange = { onChange(item.copy(weight = decimalInput(it))) },
                    label = { Text("वजन / Weight") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Column(Modifier.weight(1f)) {
                    WEIGHT_UNITS.forEach { unit ->
                        TextButton(onClick = { onChange(item.copy(unit = unit)) }) {
                            Text(if (item.unit == unit) "✓ $unit" else unit)
                        }
                    }
                }
            }
            if (item.unit == CUSTOM_UNIT) {
                OutlinedTextField(
                    value = item.customUnit,
                    onValueChange = { onChange(item.copy(customUnit = it.take(24))) },
                    label = { Text("अपनी unit / Custom unit") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }
        OutlinedTextField(
            value = item.description,
            onValueChange = { onChange(item.copy(description = it)) },
            label = { Text("विवरण / Description") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (item.photoPath.isNotBlank()) {
            Text("✓ सामान की live photo सुरक्षित / Item photo captured")
            Row {
                TextButton(onClick = onPhoto) { Text("फिर फोटो / Retake") }
                TextButton(onClick = {
                    File(item.photoPath).delete()
                    onChange(item.copy(photoPath = ""))
                }) { Text("हटाएँ / Remove") }
            }
        } else Text("सामान की photo optional / Item photo optional")
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

private object PrivateMediaVault {
    fun newPhotoFile(context: android.content.Context, prefix: String): File {
        val directory = File(context.filesDir, "private_media/alpha25a").apply { mkdirs() }
        return File(directory, "$prefix-${System.currentTimeMillis()}-${UUID.randomUUID()}.jpg")
    }

    fun attachCustomerPhoto(context: android.content.Context, customerId: String, temporaryPath: String) {
        attach(context, "customer-$customerId.jpg", temporaryPath)
    }

    fun attachItemPhoto(context: android.content.Context, itemId: String, temporaryPath: String) {
        attach(context, "item-$itemId.jpg", temporaryPath)
    }

    private fun attach(context: android.content.Context, finalName: String, temporaryPath: String) {
        if (temporaryPath.isBlank()) return
        val source = File(temporaryPath)
        if (!source.exists()) return
        val directory = File(context.filesDir, "private_media/alpha25a").apply { mkdirs() }
        val target = File(directory, finalName)
        if (source.absolutePath != target.absolutePath) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
    }
}

private fun readSelectedPhoneContact(context: android.content.Context, uri: Uri): Pair<String, String>? {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
    )
    return try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)).orEmpty()
            val phone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)).orEmpty()
            name to normalizeMobileInput(phone)
        }
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun showDatePicker(context: android.content.Context, current: Long, onSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = current }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            onSelected(selected)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).apply { datePicker.maxDate = endOfToday() }.show()
}

private fun validatePracticalEntry(
    customerName: String,
    amount: String,
    interest: InterestUiState,
    date: Long,
    items: List<PracticalItemDraft>,
): String? = when {
    customerName.trim().length < 2 -> "ग्राहक का नाम जरूरी है / Customer name required"
    amount.toDoubleOrNull()?.let { it <= 0.0 } != false -> "सही मूल राशि डालें / Enter valid principal"
    interest.toTermsOrNull() == null -> "सही ब्याज नियम चुनें / Enter valid interest terms"
    interest.mode == InterestMode.PERCENT_PER_MONTH && interest.monthlyRatePercent.toDoubleOrNull()?.let { it !in 0.0..100.0 } != false -> "सही ब्याज दर डालें / Enter valid interest rate"
    interest.mode == InterestMode.FLAT_PER_MONTH && interest.flatMonthlyRupees.toDoubleOrNull()?.let { it < 0.0 } != false -> "सही flat monthly charge डालें"
    date > endOfToday() -> "भविष्य की तारीख allowed नहीं / Future date not allowed"
    items.isEmpty() -> "कम से कम एक सामान जरूरी / Add at least one item"
    items.any { it.category.isBlank() || it.name.trim().isBlank() } -> "हर सामान का नाम और category जरूरी / Item name and category required"
    items.any { it.quantity.toIntOrNull()?.let { qty -> qty <= 0 } != false } -> "सामान की संख्या सही डालें / Invalid quantity"
    items.any { !it.advancedWeight && it.unit == CUSTOM_UNIT && it.customUnit.isBlank() } -> "Custom weight unit लिखें"
    items.any { it.advancedWeight && (it.deduction.toDoubleOrNull() ?: 0.0) > (it.gross.toDoubleOrNull() ?: 0.0) } -> "कटौती gross weight से अधिक नहीं हो सकती"
    else -> null
}

private fun interestReviewLabel(state: InterestUiState): String = when (state.mode) {
    InterestMode.PERCENT_PER_MONTH -> "ब्याज / Interest: ${state.monthlyRatePercent}% प्रति माह"
    InterestMode.FLAT_PER_MONTH -> "ब्याज / Interest: ₹${state.flatMonthlyRupees.ifBlank { "0" }} flat प्रति माह"
}

private fun normalizeMobileInput(value: String): String {
    val digits = value.filter(Char::isDigit)
    return when {
        digits.length > 10 && digits.startsWith("91") -> digits.takeLast(10)
        else -> digits.take(10)
    }
}

private fun decimalInput(value: String): String {
    val filtered = value.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot < 0) filtered else filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
}

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 12)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun endOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.timeInMillis

private const val CUSTOM_UNIT = "Custom / अन्य"
private val WEIGHT_UNITS = listOf(
    "Gram / ग्राम",
    "mg / मिलीग्राम",
    "Kg / किलो",
    "Tola / तोला",
    "Ratti / रत्ती",
    "Piece / नग",
    CUSTOM_UNIT,
)
