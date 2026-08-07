package com.girvikhata.app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.custody.CustodyPlacementStore
import com.girvikhata.app.custody.StorageLocation
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.BlueprintKhataRepository
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.SecureMediaVault
import com.girvikhata.app.domain.CustomerCandidate
import com.girvikhata.app.domain.CustomerMatcher
import com.girvikhata.app.domain.GirviInterestMetadata
import com.girvikhata.app.domain.GirviSequence
import com.girvikhata.app.domain.InterestEngine
import com.girvikhata.app.domain.InterestMode
import com.girvikhata.app.domain.InterestPeriodRule
import com.girvikhata.app.domain.InterestTerms
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

class PracticalEntryActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val records = EncryptedRecordStore(applicationContext)
        val repository = BlueprintKhataRepository(records)
        val mediaVault = SecureMediaVault(applicationContext).also { it.cleanupTemps() }
        val custodyStore = CustodyPlacementStore(applicationContext)
        val journal = DataSafetyJournal(applicationContext)
        setContent {
            MaterialTheme {
                var snapshot by remember { mutableStateOf(repository.snapshot()) }
                val storageLocations = remember { custodyStore.load().locations.filter { it.active } }
                BlueprintEntryScreen(
                    snapshot = snapshot,
                    storageLocations = storageLocations,
                    mediaVault = mediaVault,
                    onBack = ::finish,
                    onSave = { customer, girvi, customerPhoto, itemPhotos, storageLocationId ->
                        runCatching {
                            // Business data is authoritative and must never be blocked by optional media/custody metadata.
                            val saved = repository.createGirvi(customer, girvi)
                            check(saved.girvis.any { it.id == girvi.id }) { "Saved girvi verification failed" }
                            check(saved.customers.any { it.id == customer.id }) { "Saved customer verification failed" }
                            snapshot = saved

                            val warnings = mutableListOf<String>()
                            if (customerPhoto.isNotBlank()) {
                                runCatching {
                                    mediaVault.importPhoto(File(customerPhoto), "customer-${customer.id}")
                                }.onFailure { warnings += "customer photo" }
                            }
                            itemPhotos.forEach { (itemId, path) ->
                                if (path.isNotBlank()) {
                                    runCatching {
                                        mediaVault.importPhoto(File(path), "item-$itemId")
                                    }.onFailure { warnings += "item photo" }
                                }
                            }
                            if (!storageLocationId.isNullOrBlank()) {
                                runCatching {
                                    custodyStore.moveItemsToLocation(
                                        itemRefs = girvi.effectiveItems.map { girvi.id to it.id },
                                        locationId = storageLocationId,
                                        movedAt = girvi.createdAt,
                                        note = "Assigned during new Girvi entry",
                                    )
                                }.onSuccess {
                                    val location = custodyStore.load().locations.firstOrNull { it.id == storageLocationId }?.name ?: "Storage location"
                                    runCatching {
                                        journal.recordNamedEvent(
                                            "GIRVI_STORAGE_ASSIGNED",
                                            "Girvi storage assigned",
                                            "${girvi.girviNumber} • ${girvi.effectiveItems.size} items → $location",
                                        )
                                    }
                                }.onFailure { warnings += "locker assignment" }
                            }
                            if (warnings.isNotEmpty()) {
                                Toast.makeText(
                                    this@PracticalEntryActivity,
                                    "Girvi saved. ${warnings.distinct().joinToString()} save नहीं हुई; financial data safe है.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }.fold(
                            onSuccess = {
                                setResult(Activity.RESULT_OK)
                                finish()
                                null
                            },
                            onFailure = { "SAVE-REBUILD: ${it.message ?: it::class.java.simpleName}" },
                        )
                    },
                )
            }
        }
    }
}

private data class EntryItemDraft(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "",
    val name: String = "",
    val quantity: String = "1",
    val weight: String = "",
    val unit: String = "Gram / ग्राम",
    val gross: String = "",
    val deduction: String = "",
    val description: String = "",
    val advancedWeight: Boolean = false,
    val photoPath: String = "",
)

private enum class EntryPhotoTarget { CUSTOMER, ITEM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlueprintEntryScreen(
    snapshot: AppSnapshot,
    storageLocations: List<StorageLocation>,
    mediaVault: SecureMediaVault,
    onBack: () -> Unit,
    onSave: (CustomerRecord, GirviRecord, String, Map<String, String>, String?) -> String?,
) {
    val context = LocalContext.current
    var customerName by rememberSaveable { mutableStateOf("") }
    var selectedCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var mobile by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var customerPhotoPath by rememberSaveable { mutableStateOf("") }
    var pledgeDate by rememberSaveable { mutableStateOf(startOfToday()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var interestModeName by rememberSaveable { mutableStateOf(InterestMode.PERCENT_PER_MONTH.name) }
    var monthlyRate by rememberSaveable { mutableStateOf("2") }
    var flatMonthly by rememberSaveable { mutableStateOf("") }
    var periodRuleName by rememberSaveable { mutableStateOf(InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS.name) }
    var compoundEnabled by rememberSaveable { mutableStateOf(false) }
    var compoundMonths by rememberSaveable { mutableStateOf(1) }
    var selectedStorageLocationId by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showReview by rememberSaveable { mutableStateOf(false) }
    var pendingTarget by remember { mutableStateOf<EntryPhotoTarget?>(null) }
    var pendingItemIndex by remember { mutableStateOf(-1) }
    var pendingPath by remember { mutableStateOf("") }

    val categories = snapshot.categories.filter { it.active }.map { it.name }
    val firstCategory = categories.firstOrNull() ?: "Other"
    val items = remember { mutableStateListOf(EntryItemDraft(category = firstCategory)) }

    val matchedCustomer = remember(customerName, mobile, snapshot) {
        CustomerMatcher.findBestMatch(
            snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) },
            customerName,
            mobile,
        )
    }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            runCatching { readSelectedPhoneContact(context, uri) }
                .onSuccess { contact ->
                    if (contact != null) {
                        customerName = contact.first
                        mobile = normalizeMobileInput(contact.second)
                        selectedCustomerId = CustomerMatcher.findBestMatch(
                            snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) },
                            customerName,
                            mobile,
                        )?.id
                    }
                }
                .onFailure { error = "CONTACT: ${it.message ?: "Contact read failed"}" }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            when (pendingTarget) {
                EntryPhotoTarget.CUSTOMER -> customerPhotoPath = pendingPath
                EntryPhotoTarget.ITEM -> if (pendingItemIndex in items.indices) {
                    items[pendingItemIndex] = items[pendingItemIndex].copy(photoPath = pendingPath)
                }
                null -> Unit
            }
        } else if (pendingPath.isNotBlank()) File(pendingPath).delete()
        pendingTarget = null
        pendingItemIndex = -1
        pendingPath = ""
    }

    fun takePhoto(target: EntryPhotoTarget, itemIndex: Int = -1) {
        runCatching {
            val file = mediaVault.newCameraTempFile(if (target == EntryPhotoTarget.CUSTOMER) "customer" else "item")
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingTarget = target
            pendingItemIndex = itemIndex
            pendingPath = file.absolutePath
            camera.launch(uri)
        }.onFailure { error = "PHOTO: ${it.message ?: "Camera unavailable"}" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("नया गिरवी / New Girvi") },
                navigationIcon = { TextButton(onClick = onBack) { Text("वापस / Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard("ग्राहक / Customer") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it; selectedCustomerId = null },
                            label = { Text("नाम / Name") },
                            modifier = Modifier.fillMaxWidth(0.72f),
                            singleLine = true,
                        )
                        IconButton(onClick = {
                            runCatching {
                                contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                            }.onFailure { error = "CONTACT: ${it.message ?: "Picker unavailable"}" }
                        }) { Icon(Icons.Default.Contacts, "Select phone contact") }
                        IconButton(onClick = { takePhoto(EntryPhotoTarget.CUSTOMER) }) {
                            Icon(Icons.Default.CameraAlt, "Customer live photo")
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
                    matchedCustomer?.let { Text("✓ मौजूदा ग्राहक मिला / Existing customer matched", color = MaterialTheme.colorScheme.primary) }
                    if (customerPhotoPath.isNotBlank()) {
                        LocalPhotoPreview(customerPhotoPath, "ग्राहक की फोटो / Customer photo")
                        Text("✓ Live photo captured; save पर encrypted होगी")
                        TextButton(onClick = { File(customerPhotoPath).delete(); customerPhotoPath = "" }) { Text("फोटो हटाएँ / Remove") }
                    } else Text("Photo optional • live camera only")
                }
            }

            item {
                SectionCard("तारीख और राशि / Date & Amount") {
                    OutlinedButton(
                        onClick = { showDatePicker(context, pledgeDate) { if (it <= endOfToday()) pledgeDate = it else error = "Future date allowed नहीं है" } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.CalendarMonth, null)
                        Text("  ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(pledgeDate))}")
                    }
                    Text("आज default • back-date allowed • future blocked")
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = decimalInput(it) },
                        label = { Text("मूल राशि ₹ / Principal ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                SectionCard("लॉकर / Storage Location") {
                    Text("सभी items के लिए अभी एक location चुनें; बाद में item-wise shift कर सकते हैं.")
                    OutlinedButton(
                        onClick = { selectedStorageLocationId = null },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (selectedStorageLocationId == null) "✓ बाद में सेट करें / Assign Later" else "बाद में सेट करें / Assign Later") }
                    storageLocations.forEach { location ->
                        OutlinedButton(
                            onClick = { selectedStorageLocationId = location.id },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (selectedStorageLocationId == location.id) "✓ ${location.name}" else location.name)
                        }
                    }
                    if (storageLocations.isEmpty()) {
                        Text("अभी कोई locker/location नहीं है. More → Masters → Storage/Locker में list बना सकते हैं.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            item {
                SectionCard("ब्याज नियम / Interest Rule") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        InterestMode.entries.forEach { mode ->
                            TextButton(onClick = { interestModeName = mode.name; if (mode == InterestMode.FLAT_PER_MONTH) compoundEnabled = false }) {
                                Text(if (interestModeName == mode.name) "✓ ${modeLabel(mode)}" else modeLabel(mode))
                            }
                        }
                    }
                    if (interestModeName == InterestMode.PERCENT_PER_MONTH.name) {
                        OutlinedTextField(
                            value = monthlyRate,
                            onValueChange = { monthlyRate = decimalInput(it) },
                            label = { Text("मासिक % / Monthly %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        OutlinedTextField(
                            value = flatMonthly,
                            onValueChange = { flatMonthly = decimalInput(it) },
                            label = { Text("हर माह तय ब्याज ₹ / Flat monthly ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text("अवधि नियम / Period rule", fontWeight = FontWeight.Bold)
                    InterestPeriodRule.entries.forEach { rule ->
                        TextButton(onClick = { periodRuleName = rule.name }) {
                            Text(if (periodRuleName == rule.name) "✓ ${periodLabel(rule)}" else periodLabel(rule))
                        }
                    }
                    if (interestModeName == InterestMode.PERCENT_PER_MONTH.name) {
                        Row {
                            Checkbox(checked = compoundEnabled, onCheckedChange = { compoundEnabled = it })
                            Text("Compound / चक्रवृद्धि (Advanced)")
                        }
                        if (compoundEnabled) {
                            Text("Compounding interval")
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                listOf(1, 2, 3, 6, 12).forEach { months ->
                                    TextButton(onClick = { compoundMonths = months }) {
                                        Text(if (compoundMonths == months) "✓ ${months}m" else "${months}m")
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                listOf(24, 36).forEach { months ->
                                    TextButton(onClick = { compoundMonths = months }) {
                                        Text(if (compoundMonths == months) "✓ ${months / 12}y" else "${months / 12}y")
                                    }
                                }
                            }
                        }
                    }
                    val terms = buildInterestTerms(interestModeName, monthlyRate, flatMonthly, periodRuleName, compoundEnabled, compoundMonths)
                    val principal = rupeesToPaise(amount)
                    if (terms != null && principal != null) {
                        val monthly = InterestEngine.monthlyChargePaise(principal, terms)
                        Text("प्रति माह / Monthly: ${money(monthly)}", fontWeight = FontWeight.Bold)
                        runCatching { InterestEngine.quote(principal, pledgeDate, endOfToday(), terms) }.getOrNull()?.let {
                            Text("आज तक अनुमानित ब्याज / Accrued today: ${money(it.interestPaise)}")
                        }
                    }
                }
            }

            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                ItemEditor(
                    index = index,
                    item = item,
                    categories = categories,
                    removable = items.size > 1,
                    onChange = { items[index] = it },
                    onRemove = { File(items[index].photoPath).delete(); items.removeAt(index) },
                    onPhoto = { takePhoto(EntryPhotoTarget.ITEM, index) },
                )
            }

            item {
                OutlinedButton(onClick = { items.add(EntryItemDraft(category = firstCategory)) }, modifier = Modifier.fillMaxWidth()) {
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
                        error = validateEntry(customerName, amount, interestModeName, monthlyRate, flatMonthly, pledgeDate, items)
                        if (error == null) showReview = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("जाँचें और सेव करें / Review & Save") }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showReview) {
        val terms = requireNotNull(buildInterestTerms(interestModeName, monthlyRate, flatMonthly, periodRuleName, compoundEnabled, compoundMonths))
        val principalPaise = requireNotNull(rupeesToPaise(amount))
        val selectedStorageName = storageLocations.firstOrNull { it.id == selectedStorageLocationId }?.name
        AlertDialog(
            onDismissRequest = { showReview = false },
            title = { Text("एंट्री जाँचें / Review Entry") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text("ग्राहक: ${customerName.trim()}", fontWeight = FontWeight.Bold)
                        Text("मोबाइल: ${mobile.ifBlank { "—" }}")
                        Text("तारीख: ${DateFormat.getDateInstance().format(Date(pledgeDate))}")
                        Text("मूलधन: ${money(principalPaise)}")
                        Text("Locker: ${selectedStorageName ?: "Assign later"}")
                        Text("Interest: ${modeLabel(terms.mode)} • ${periodLabel(terms.periodRule)}")
                        Text("Monthly: ${money(InterestEngine.monthlyChargePaise(principalPaise, terms))}")
                        if (terms.compoundEveryMonths != null) Text("Compound every ${terms.compoundEveryMonths} month(s)")
                    }
                    if (customerPhotoPath.isNotBlank()) {
                        item { LocalPhotoPreview(customerPhotoPath, "ग्राहक की फोटो / Customer photo", compact = true) }
                    }
                    item { Text("सामान: ${items.size}", fontWeight = FontWeight.Bold) }
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("${index + 1}. ${item.name}", fontWeight = FontWeight.Bold)
                                Text("${item.category} • Qty ${item.quantity} • ${if (item.advancedWeight) item.gross + " g" else item.weight + " " + item.unit}")
                                if (item.description.isNotBlank()) Text(item.description)
                                if (item.photoPath.isNotBlank()) LocalPhotoPreview(item.photoPath, "Item photo", compact = true)
                                else Text("Photo नहीं ली", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val failure = runCatching {
                        val fresh = snapshot
                        val matchedId = selectedCustomerId ?: CustomerMatcher.findBestMatch(
                            fresh.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) },
                            customerName,
                            mobile,
                        )?.id
                        val existing = matchedId?.let { id -> fresh.customers.firstOrNull { it.id == id } }
                        val customer = (existing ?: CustomerRecord(name = customerName.trim())).copy(
                            name = customerName.trim(),
                            mobile = normalizeMobileInput(mobile),
                            address = address.trim(),
                        )
                        val itemRecords = items.mapIndexed { index, draft ->
                            val baseDescription = buildString {
                                append(draft.description.trim())
                                if (!draft.advancedWeight) {
                                    if (isNotBlank()) append(" • ")
                                    append("Weight unit: ${draft.unit}")
                                }
                            }
                            GirviItemRecord(
                                id = draft.id,
                                categoryName = draft.category.ifBlank { firstCategory },
                                itemName = draft.name.trim(),
                                quantity = draft.quantity.toInt(),
                                grossWeightGrams = if (draft.advancedWeight) draft.gross else draft.weight,
                                deductionWeightGrams = if (draft.advancedWeight) draft.deduction else "",
                                description = if (index == 0) GirviInterestMetadata.attach(baseDescription, terms) else baseDescription,
                            )
                        }
                        val first = itemRecords.first()
                        val girvi = GirviRecord(
                            girviNumber = GirviSequence.nextNumber(fresh.girvis.map { it.girviNumber }),
                            customerId = customer.id,
                            customerName = customer.name,
                            categoryName = first.categoryName,
                            itemName = first.itemName,
                            weightGrams = first.grossWeightGrams,
                            principalPaise = principalPaise,
                            monthlyRateBasisPoints = terms.monthlyRateBasisPoints,
                            createdAt = pledgeDate,
                            items = itemRecords,
                        )
                        val itemPhotos = items.associate { it.id to it.photoPath }
                        onSave(customer, girvi, customerPhotoPath, itemPhotos, selectedStorageLocationId)
                    }.fold(
                        onSuccess = { it },
                        onFailure = { "SAVE-BUILD: ${it.message ?: it::class.java.simpleName}" },
                    )
                    if (failure != null) {
                        error = failure
                        showReview = false
                    }
                }) { Text("पुष्टि और सेव / Confirm Save") }
            },
            dismissButton = { TextButton(onClick = { showReview = false }) { Text("बदलें / Edit") } },
        )
    }
}

@Composable
private fun ItemEditor(
    index: Int,
    item: EntryItemDraft,
    categories: List<String>,
    removable: Boolean,
    onChange: (EntryItemDraft) -> Unit,
    onRemove: () -> Unit,
    onPhoto: () -> Unit,
) {
    SectionCard("सामान ${index + 1} / Item ${index + 1}") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("विवरण / Details", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(0.65f))
            IconButton(onClick = onPhoto) { Icon(Icons.Default.CameraAlt, "Item live photo") }
            if (removable) IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove item") }
        }
        if (categories.isNotEmpty()) {
            categories.take(6).forEach { category ->
                TextButton(onClick = { onChange(item.copy(category = category)) }) {
                    Text(if (item.category == category) "✓ $category" else category)
                }
            }
        }
        OutlinedTextField(item.name, { onChange(item.copy(name = it)) }, label = { Text("सामान का नाम / Item name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            item.quantity,
            { onChange(item.copy(quantity = it.filter(Char::isDigit).take(3))) },
            label = { Text("संख्या / Quantity") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            Checkbox(checked = item.advancedWeight, onCheckedChange = { onChange(item.copy(advancedWeight = it)) })
            Text("वजन का पूरा विवरण / Advanced weight")
        }
        if (item.advancedWeight) {
            OutlinedTextField(item.gross, { onChange(item.copy(gross = decimalInput(it))) }, label = { Text("Gross weight (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(item.deduction, { onChange(item.copy(deduction = decimalInput(it))) }, label = { Text("कटौती / Deduction (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            val gross = item.gross.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val less = item.deduction.toBigDecimalOrNull() ?: BigDecimal.ZERO
            Text("शुद्ध / Net: ${gross.subtract(less).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()} g")
        } else {
            OutlinedTextField(item.weight, { onChange(item.copy(weight = decimalInput(it))) }, label = { Text("वजन / Weight") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            listOf("Gram / ग्राम", "mg / मिलीग्राम", "Kg / किलो", "Tola / तोला", "Ratti / रत्ती", "Piece / नग", "Custom / अन्य").forEach { unit ->
                TextButton(onClick = { onChange(item.copy(unit = unit)) }) { Text(if (item.unit == unit) "✓ $unit" else unit) }
            }
        }
        OutlinedTextField(item.description, { onChange(item.copy(description = it)) }, label = { Text("विवरण / Description") }, modifier = Modifier.fillMaxWidth())
        if (item.photoPath.isNotBlank()) {
            LocalPhotoPreview(item.photoPath, "सामान की फोटो / Item photo")
            Text("✓ Live photo captured; save पर encrypted होगी")
            TextButton(onClick = { File(item.photoPath).delete(); onChange(item.copy(photoPath = "")) }) { Text("फोटो हटाएँ / Remove") }
        } else Text("Item photo optional • live camera only")
    }
}

@Composable
private fun LocalPhotoPreview(path: String, label: String, compact: Boolean = false) {
    val bitmap = remember(path) { decodePreviewBitmap(path) }
    if (bitmap != null) {
        Text(label, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier.fillMaxWidth().height(if (compact) 130.dp else 190.dp),
                contentScale = ContentScale.Crop,
            )
        }
    } else {
        Text("Photo preview उपलब्ध नहीं", color = MaterialTheme.colorScheme.error)
    }
}

private fun decodePreviewBitmap(path: String): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

private fun validateEntry(
    customerName: String,
    amount: String,
    interestModeName: String,
    monthlyRate: String,
    flatMonthly: String,
    pledgeDate: Long,
    items: List<EntryItemDraft>,
): String? {
    if (customerName.trim().isBlank()) return "Customer name जरूरी है"
    if ((rupeesToPaise(amount) ?: 0L) <= 0L) return "Principal amount सही भरें"
    if (pledgeDate > endOfToday()) return "Future date allowed नहीं है"
    if (interestModeName == InterestMode.PERCENT_PER_MONTH.name && basisPoints(monthlyRate) == null) return "Monthly interest % सही भरें"
    if (interestModeName == InterestMode.FLAT_PER_MONTH.name && rupeesToPaise(flatMonthly) == null) return "Flat monthly interest सही भरें"
    if (items.isEmpty()) return "कम से कम एक item जरूरी है"
    items.forEachIndexed { index, item ->
        if (item.name.trim().isBlank()) return "Item ${index + 1} का नाम जरूरी है"
        if ((item.quantity.toIntOrNull() ?: 0) <= 0) return "Item ${index + 1} quantity सही भरें"
        if (item.advancedWeight) {
            val gross = item.gross.toBigDecimalOrNull() ?: return "Item ${index + 1} gross weight सही भरें"
            val less = item.deduction.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (gross < BigDecimal.ZERO || less < BigDecimal.ZERO || less > gross) return "Item ${index + 1} weight/deduction सही भरें"
        } else if (item.unit != "Piece / नग" && (item.weight.toBigDecimalOrNull() ?: BigDecimal.valueOf(-1)) < BigDecimal.ZERO) {
            return "Item ${index + 1} weight सही भरें"
        }
    }
    return null
}

private fun buildInterestTerms(
    modeName: String,
    monthlyRate: String,
    flatMonthly: String,
    ruleName: String,
    compoundEnabled: Boolean,
    compoundMonths: Int,
): InterestTerms? = runCatching {
    val mode = InterestMode.valueOf(modeName)
    val rule = InterestPeriodRule.valueOf(ruleName)
    InterestTerms(
        mode = mode,
        monthlyRateBasisPoints = if (mode == InterestMode.PERCENT_PER_MONTH) basisPoints(monthlyRate) ?: return null else 0,
        flatMonthlyChargePaise = if (mode == InterestMode.FLAT_PER_MONTH) rupeesToPaise(flatMonthly) ?: return null else 0,
        periodRule = rule,
        compoundEveryMonths = if (mode == InterestMode.PERCENT_PER_MONTH && compoundEnabled) compoundMonths else null,
    )
}.getOrNull()

private fun modeLabel(mode: InterestMode): String = when (mode) {
    InterestMode.PERCENT_PER_MONTH -> "% प्रति माह"
    InterestMode.FLAT_PER_MONTH -> "तय ₹ प्रति माह"
}

private fun periodLabel(rule: InterestPeriodRule): String = when (rule) {
    InterestPeriodRule.EXACT_DAYS -> "Exact days / रोज़ के हिसाब से"
    InterestPeriodRule.FULL_MONTH_STARTED -> "Started month = full / अधूरा भी पूरा"
    InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS -> "Months + remaining days"
}

private fun decimalInput(value: String): String {
    val filtered = value.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot < 0) filtered.take(12) else {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "").take(4)
    }.take(16)
}

private fun rupeesToPaise(value: String): Long? = runCatching {
    val number = value.toBigDecimal()
    require(number >= BigDecimal.ZERO)
    number.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
}.getOrNull()

private fun basisPoints(value: String): Int? = runCatching {
    val number = value.toBigDecimal()
    require(number >= BigDecimal.ZERO)
    number.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).intValueExact()
}.getOrNull()

private fun money(paise: Long): String = "₹" + BigDecimal.valueOf(paise, 2).setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun normalizeMobileInput(value: String): String = value.filter { it.isDigit() || it == '+' }.take(16)

private fun readSelectedPhoneContact(context: Context, uri: Uri): Pair<String, String>? {
    return context.contentResolver.query(
        uri,
        arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
        null,
        null,
        null,
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
        val number = if (numberIndex >= 0) cursor.getString(numberIndex).orEmpty() else ""
        name to number
    }
}

private fun showDatePicker(context: Context, current: Long, onSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = current }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onSelected(Calendar.getInstance().apply {
                clear()
                set(year, month, day, 12, 0, 0)
            }.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
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
