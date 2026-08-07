package com.girvikhata.app.custody

import android.content.Context
import com.girvikhata.app.domain.ExternalFundingAdvance
import com.girvikhata.app.domain.ExternalFundingPayment
import com.girvikhata.app.domain.ExternalFundingProjection
import com.girvikhata.app.domain.ExternalInterestRule
import com.girvikhata.app.domain.ExternalPlacementLedger
import com.girvikhata.app.security.DeviceKeyManager
import com.girvikhata.app.security.EncryptedPayload
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class StorageLocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String = "OTHER",
    val detail: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

data class ExternalParty(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mobile: String = "",
    val address: String = "",
    val defaultMonthlyRateBasisPoints: Int = 0,
    val note: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

data class PlacementItem(
    val girviId: String,
    val itemId: String,
    val addedAt: Long,
    val removedAt: Long? = null,
)

data class PlacementLot(
    val id: String = UUID.randomUUID().toString(),
    val lotNumber: String,
    val partyId: String,
    val openedAt: Long,
    val amountReceivedPaise: Long,
    val monthlyRateBasisPoints: Int,
    val note: String = "",
    val status: String = "ACTIVE",
    val items: List<PlacementItem> = emptyList(),
    val fundingAdvances: List<ExternalFundingAdvance> = emptyList(),
    val fundingPayments: List<ExternalFundingPayment> = emptyList(),
    val closedAt: Long? = null,
)

data class CustodyMovement(
    val id: String = UUID.randomUUID().toString(),
    val girviId: String,
    val itemId: String,
    val destinationType: String,
    val destinationId: String,
    val lotId: String? = null,
    val movedAt: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class CustodyPlacementSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val locations: List<StorageLocation> = emptyList(),
    val parties: List<ExternalParty> = emptyList(),
    val lots: List<PlacementLot> = emptyList(),
    val movements: List<CustodyMovement> = emptyList(),
) {
    companion object { const val CURRENT_SCHEMA = 2 }
}

data class CurrentCustody(
    val destinationType: String,
    val destinationId: String,
    val lotId: String?,
    val movedAt: Long,
)

class CustodyPlacementStore(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) {
    private val file = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun load(): CustodyPlacementSnapshot = if (!file.exists()) CustodyPlacementSnapshot() else read(file)

    @Synchronized
    fun save(snapshot: CustodyPlacementSnapshot) {
        val normalized = snapshot.copy(schemaVersion = CustodyPlacementSnapshot.CURRENT_SCHEMA)
        validate(normalized)
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        runCatching {
            write(temp, normalized)
            check(read(temp) == normalized) { "Custody store read-back verify failed" }
            if (file.exists() && !file.delete()) error("Old custody store replace nahi hua")
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                check(temp.delete()) { "Custody temp cleanup failed" }
            }
            check(read(file) == normalized) { "Custody store final verify failed" }
        }.onFailure { temp.delete(); throw it }
    }

    @Synchronized
    fun addLocation(name: String, type: String, detail: String): CustodyPlacementSnapshot {
        val current = load()
        val clean = name.trim().replace(Regex("\\s+"), " ")
        require(clean.length in 2..80) { "Location name 2-80 characters ka ho" }
        require(current.locations.none { it.active && it.name.equals(clean, true) }) { "Ye location pehle se hai" }
        return current.copy(locations = current.locations + StorageLocation(name = clean, type = type, detail = detail.trim().take(200))).also(::save)
    }

    @Synchronized
    fun setLocationActive(id: String, active: Boolean): CustodyPlacementSnapshot {
        val current = load()
        require(current.locations.any { it.id == id }) { "Location nahi mili" }
        return current.copy(locations = current.locations.map { if (it.id == id) it.copy(active = active) else it }).also(::save)
    }

    @Synchronized
    fun addParty(name: String, mobile: String, address: String, defaultRateBps: Int, note: String): CustodyPlacementSnapshot {
        val current = load()
        val clean = name.trim().replace(Regex("\\s+"), " ")
        val digits = mobile.filter(Char::isDigit).let { if (it.length == 12 && it.startsWith("91")) it.drop(2) else it }
        require(clean.length in 2..80) { "Party name required" }
        require(digits.isBlank() || digits.length == 10) { "Mobile 10 digits ka hona chahiye" }
        require(defaultRateBps in 0..100_000) { "Default interest rate invalid" }
        require(current.parties.none { it.active && it.name.equals(clean, true) }) { "Ye party pehle se hai" }
        return current.copy(
            parties = current.parties + ExternalParty(
                name = clean,
                mobile = digits,
                address = address.trim().take(200),
                defaultMonthlyRateBasisPoints = defaultRateBps,
                note = note.trim().take(250),
            ),
        ).also(::save)
    }

    @Synchronized
    fun moveToLocation(girviId: String, itemId: String, locationId: String, movedAt: Long, note: String): CustodyPlacementSnapshot =
        moveItemsToLocation(listOf(girviId to itemId), locationId, movedAt, note)

    @Synchronized
    fun moveItemsToLocation(
        itemRefs: List<Pair<String, String>>,
        locationId: String,
        movedAt: Long,
        note: String,
    ): CustodyPlacementSnapshot {
        val current = load()
        require(current.locations.any { it.id == locationId && it.active }) { "Active location nahi mili" }
        require(itemRefs.isNotEmpty()) { "Items required" }
        require(itemRefs.all { it.first.isNotBlank() && it.second.isNotBlank() }) { "Item reference invalid" }
        require(itemRefs.map { it.second }.distinct().size == itemRefs.size) { "Same item duplicate hai" }
        require(movedAt > 0) { "Movement date invalid" }
        var detachedLots = current.lots
        itemRefs.forEach { (_, itemId) -> detachedLots = detachItemFromActiveLots(detachedLots, itemId, movedAt) }
        val movements = itemRefs.map { (girviId, itemId) ->
            CustodyMovement(
                girviId = girviId,
                itemId = itemId,
                destinationType = "LOCATION",
                destinationId = locationId,
                movedAt = movedAt,
                note = note.trim().take(250),
            )
        }
        return current.copy(lots = detachedLots, movements = current.movements + movements).also(::save)
    }

    @Synchronized
    fun createLot(
        lotNumber: String,
        partyId: String,
        itemRefs: List<Pair<String, String>>,
        openedAt: Long,
        amountReceivedPaise: Long,
        monthlyRateBasisPoints: Int,
        note: String,
        interestRule: ExternalInterestRule = ExternalInterestRule.EXACT_DAYS,
    ): CustodyPlacementSnapshot {
        val current = load()
        val number = lotNumber.trim().uppercase()
        require(number.length in 3..40) { "Lot number required" }
        require(current.lots.none { it.lotNumber.equals(number, true) }) { "Lot number already exists" }
        require(current.parties.any { it.id == partyId && it.active }) { "Active external party nahi mili" }
        require(itemRefs.isNotEmpty()) { "Kam se kam ek item select karein" }
        require(itemRefs.map { it.second }.distinct().size == itemRefs.size) { "Same item lot me do baar select hai" }
        require(openedAt > 0L) { "Placement date invalid" }
        require(amountReceivedPaise >= 0L) { "Amount invalid" }
        require(monthlyRateBasisPoints in 0..100_000) { "Interest rate invalid" }
        val alreadyActive = current.lots.flatMap { lot -> lot.items.filter { it.removedAt == null }.map { it.itemId } }.toSet()
        require(itemRefs.none { it.second in alreadyActive }) { "Selected item pehle se active external lot me hai" }
        val lotId = UUID.randomUUID().toString()
        val initialFunding = if (amountReceivedPaise > 0L) {
            listOf(
                ExternalFundingAdvance(
                    id = "initial-$lotId",
                    amountPaise = amountReceivedPaise,
                    monthlyRateBasisPoints = monthlyRateBasisPoints,
                    createdAt = openedAt,
                    interestRule = interestRule,
                    note = "Initial lot funding${if (note.isBlank()) "" else " • ${note.trim()}"}",
                ),
            )
        } else emptyList()
        val lot = PlacementLot(
            id = lotId,
            lotNumber = number,
            partyId = partyId,
            openedAt = openedAt,
            amountReceivedPaise = amountReceivedPaise,
            monthlyRateBasisPoints = monthlyRateBasisPoints,
            note = note.trim().take(250),
            items = itemRefs.map { (girviId, itemId) -> PlacementItem(girviId, itemId, openedAt) },
            fundingAdvances = initialFunding,
        )
        val movements = itemRefs.map { (girviId, itemId) ->
            CustodyMovement(
                girviId = girviId,
                itemId = itemId,
                destinationType = "EXTERNAL",
                destinationId = partyId,
                lotId = lotId,
                movedAt = openedAt,
                note = "Lot $number${if (note.isBlank()) "" else " • ${note.trim()}"}",
            )
        }
        return current.copy(lots = current.lots + lot, movements = current.movements + movements).also(::save)
    }

    @Synchronized
    fun addItemsToLot(lotId: String, itemRefs: List<Pair<String, String>>, addedAt: Long, note: String): CustodyPlacementSnapshot {
        val current = load()
        val lot = current.lots.firstOrNull { it.id == lotId && it.status == "ACTIVE" } ?: error("Active lot nahi mila")
        require(itemRefs.isNotEmpty()) { "Items select karein" }
        require(addedAt >= lot.openedAt) { "Item date lot opening se pehle nahi ho sakti" }
        require(itemRefs.map { it.second }.distinct().size == itemRefs.size) { "Same item duplicate hai" }
        val activeElsewhere = current.lots.flatMap { candidate -> candidate.items.filter { it.removedAt == null }.map { it.itemId } }.toSet()
        require(itemRefs.none { it.second in activeElsewhere }) { "Selected item active external lot me hai" }
        val updated = current.lots.map {
            if (it.id != lotId) it else it.copy(items = it.items + itemRefs.map { ref -> PlacementItem(ref.first, ref.second, addedAt) })
        }
        val movements = itemRefs.map { (girviId, itemId) ->
            CustodyMovement(girviId = girviId, itemId = itemId, destinationType = "EXTERNAL", destinationId = lot.partyId, lotId = lotId, movedAt = addedAt, note = note.trim().take(250))
        }
        return current.copy(lots = updated, movements = current.movements + movements).also(::save)
    }

    @Synchronized
    fun addExternalAdvance(
        lotId: String,
        amountPaise: Long,
        monthlyRateBasisPoints: Int,
        createdAt: Long,
        interestRule: ExternalInterestRule,
        note: String,
    ): CustodyPlacementSnapshot {
        val current = load()
        val lot = current.lots.firstOrNull { it.id == lotId && it.status == "ACTIVE" } ?: error("Active lot nahi mila")
        require(amountPaise > 0L) { "External advance positive hona chahiye" }
        require(monthlyRateBasisPoints in 0..100_000) { "Interest rate invalid" }
        require(createdAt >= lot.openedAt) { "Advance date lot opening se pehle nahi ho sakti" }
        val advance = ExternalFundingAdvance(
            amountPaise = amountPaise,
            monthlyRateBasisPoints = monthlyRateBasisPoints,
            createdAt = createdAt,
            interestRule = interestRule,
            note = note.trim().take(250),
        )
        return current.copy(lots = current.lots.map { if (it.id == lotId) it.copy(fundingAdvances = it.fundingAdvances + advance) else it }).also(::save)
    }

    @Synchronized
    fun addExternalPayment(lotId: String, amountPaise: Long, createdAt: Long, note: String): CustodyPlacementSnapshot {
        val current = load()
        val lot = current.lots.firstOrNull { it.id == lotId && it.status == "ACTIVE" } ?: error("Active lot nahi mila")
        require(amountPaise > 0L) { "External payment positive hona chahiye" }
        require(createdAt >= lot.openedAt) { "Payment date lot opening se pehle nahi ho sakti" }
        val projection = ExternalPlacementLedger.project(lot.fundingAdvances, lot.fundingPayments, createdAt)
        require(amountPaise <= projection.totalDuePaise) { "Payment external due se zyada hai" }
        val payment = ExternalFundingPayment(amountPaise = amountPaise, createdAt = createdAt, note = note.trim().take(250))
        return current.copy(lots = current.lots.map { if (it.id == lotId) it.copy(fundingPayments = it.fundingPayments + payment) else it }).also(::save)
    }

    @Synchronized
    fun reverseExternalPayment(lotId: String, paymentId: String, createdAt: Long, reason: String): CustodyPlacementSnapshot {
        val current = load()
        val lot = current.lots.firstOrNull { it.id == lotId && it.status == "ACTIVE" } ?: error("Active lot nahi mila")
        val original = lot.fundingPayments.firstOrNull { it.id == paymentId && !it.isReversal } ?: error("External payment nahi mila")
        require(lot.fundingPayments.none { it.isReversal && it.reversedPaymentId == paymentId }) { "Payment already reversed hai" }
        require(reason.trim().length >= 3) { "Reversal reason required" }
        require(createdAt >= original.createdAt) { "Reversal date payment se pehle nahi ho sakti" }
        val reversal = ExternalFundingPayment(
            amountPaise = original.amountPaise,
            createdAt = createdAt,
            note = "Reversal: ${reason.trim().take(220)}",
            isReversal = true,
            reversedPaymentId = original.id,
        )
        return current.copy(lots = current.lots.map { if (it.id == lotId) it.copy(fundingPayments = it.fundingPayments + reversal) else it }).also(::save)
    }

    fun financeProjection(lotId: String, at: Long, snapshot: CustodyPlacementSnapshot = load()): ExternalFundingProjection {
        val lot = snapshot.lots.firstOrNull { it.id == lotId } ?: error("Lot nahi mila")
        return ExternalPlacementLedger.project(lot.fundingAdvances, lot.fundingPayments, at)
    }

    @Synchronized
    fun closeLot(lotId: String, closedAt: Long): CustodyPlacementSnapshot {
        val current = load()
        val lot = current.lots.firstOrNull { it.id == lotId && it.status == "ACTIVE" } ?: error("Active lot nahi mila")
        require(lot.items.none { it.removedAt == null }) { "Lot close se pehle sab items return/transfer karein" }
        val due = ExternalPlacementLedger.project(lot.fundingAdvances, lot.fundingPayments, closedAt).totalDuePaise
        require(due == 0L) { "External due ${due} paise baki hai" }
        return current.copy(lots = current.lots.map { if (it.id == lotId) it.copy(status = "CLOSED", closedAt = closedAt) else it }).also(::save)
    }

    fun currentCustody(itemId: String, snapshot: CustodyPlacementSnapshot = load()): CurrentCustody? =
        snapshot.movements.filter { it.itemId == itemId }.maxWithOrNull(compareBy<CustodyMovement> { it.movedAt }.thenBy { it.createdAt })?.let {
            CurrentCustody(it.destinationType, it.destinationId, it.lotId, it.movedAt)
        }

    fun movementHistory(itemId: String, snapshot: CustodyPlacementSnapshot = load()): List<CustodyMovement> =
        snapshot.movements.filter { it.itemId == itemId }.sortedWith(compareByDescending<CustodyMovement> { it.movedAt }.thenByDescending { it.createdAt })

    private fun detachItemFromActiveLots(lots: List<PlacementLot>, itemId: String, removedAt: Long): List<PlacementLot> = lots.map { lot ->
        if (lot.items.none { it.itemId == itemId && it.removedAt == null }) lot
        else lot.copy(items = lot.items.map { item -> if (item.itemId == itemId && item.removedAt == null) item.copy(removedAt = removedAt) else item })
    }

    private fun validate(snapshot: CustodyPlacementSnapshot) {
        require(snapshot.schemaVersion == CustodyPlacementSnapshot.CURRENT_SCHEMA) { "Unsupported custody schema" }
        require(snapshot.locations.map { it.id }.distinct().size == snapshot.locations.size) { "Duplicate location ID" }
        require(snapshot.parties.map { it.id }.distinct().size == snapshot.parties.size) { "Duplicate party ID" }
        require(snapshot.lots.map { it.id }.distinct().size == snapshot.lots.size) { "Duplicate lot ID" }
        require(snapshot.lots.map { it.lotNumber.lowercase() }.distinct().size == snapshot.lots.size) { "Duplicate lot number" }
        val partyIds = snapshot.parties.map { it.id }.toSet()
        require(snapshot.lots.all { it.partyId in partyIds }) { "Lot party link missing" }
        val activeItemIds = snapshot.lots.flatMap { lot -> lot.items.filter { it.removedAt == null }.map { it.itemId } }
        require(activeItemIds.distinct().size == activeItemIds.size) { "Item multiple active lots me hai" }
        snapshot.lots.forEach { lot ->
            require(lot.status in setOf("ACTIVE", "CLOSED")) { "Lot status invalid" }
            require(lot.openedAt > 0L && lot.amountReceivedPaise >= 0L && lot.monthlyRateBasisPoints in 0..100_000) { "Lot finance invalid" }
            require(lot.items.map { it.itemId }.distinct().size == lot.items.size) { "Lot duplicate item" }
            require(lot.fundingAdvances.all { it.amountPaise > 0L && it.createdAt >= lot.openedAt && it.monthlyRateBasisPoints in 0..100_000 }) { "External funding advance invalid" }
            require(lot.fundingPayments.all { it.amountPaise > 0L && it.createdAt >= lot.openedAt }) { "External funding payment invalid" }
            val paymentIds = lot.fundingPayments.map { it.id }
            require(paymentIds.distinct().size == paymentIds.size) { "Duplicate external payment ID" }
            lot.fundingPayments.filter { it.isReversal }.forEach { reversal ->
                require(!reversal.reversedPaymentId.isNullOrBlank()) { "External reversal link missing" }
                require(lot.fundingPayments.any { !it.isReversal && it.id == reversal.reversedPaymentId }) { "External reversal original missing" }
            }
        }
        val locationIds = snapshot.locations.map { it.id }.toSet()
        snapshot.movements.forEach { movement ->
            require(movement.girviId.isNotBlank() && movement.itemId.isNotBlank() && movement.movedAt > 0) { "Custody movement invalid" }
            when (movement.destinationType) {
                "LOCATION" -> require(movement.destinationId in locationIds) { "Movement location missing" }
                "EXTERNAL" -> require(movement.destinationId in partyIds && !movement.lotId.isNullOrBlank()) { "External movement invalid" }
                else -> error("Unknown custody destination")
            }
        }
    }

    private fun write(target: File, snapshot: CustodyPlacementSnapshot) {
        val plaintext = encode(snapshot).toByteArray(Charsets.UTF_8)
        val encrypted = keyManager.encrypt(plaintext, AAD)
        FileOutputStream(target).use { stream ->
            DataOutputStream(stream.buffered()).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(FORMAT_VERSION)
                output.writeInt(encrypted.iv.size)
                output.write(encrypted.iv)
                output.writeInt(encrypted.ciphertext.size)
                output.write(encrypted.ciphertext)
                output.flush()
            }
            runCatching { stream.fd.sync() }
        }
    }

    private fun read(source: File): CustodyPlacementSnapshot = DataInputStream(source.inputStream().buffered()).use { input ->
        require(input.readInt() == MAGIC && input.readInt() == FORMAT_VERSION) { "Custody store format invalid" }
        val ivSize = input.readInt(); require(ivSize in 12..32)
        val iv = ByteArray(ivSize).also(input::readFully)
        val payloadSize = input.readInt(); require(payloadSize in 16..MAX_BYTES)
        val payload = ByteArray(payloadSize).also(input::readFully)
        require(input.read() == -1) { "Custody store trailing bytes" }
        decode(String(keyManager.decrypt(EncryptedPayload(payload, iv), AAD), Charsets.UTF_8)).also(::validate)
    }

    private fun encode(snapshot: CustodyPlacementSnapshot): String = JSONObject().apply {
        put("schemaVersion", snapshot.schemaVersion)
        put("locations", JSONArray().apply { snapshot.locations.forEach { value -> put(JSONObject().apply {
            put("id", value.id); put("name", value.name); put("type", value.type); put("detail", value.detail); put("active", value.active); put("createdAt", value.createdAt)
        }) } })
        put("parties", JSONArray().apply { snapshot.parties.forEach { value -> put(JSONObject().apply {
            put("id", value.id); put("name", value.name); put("mobile", value.mobile); put("address", value.address); put("defaultMonthlyRateBasisPoints", value.defaultMonthlyRateBasisPoints); put("note", value.note); put("active", value.active); put("createdAt", value.createdAt)
        }) } })
        put("lots", JSONArray().apply { snapshot.lots.forEach { lot -> put(JSONObject().apply {
            put("id", lot.id); put("lotNumber", lot.lotNumber); put("partyId", lot.partyId); put("openedAt", lot.openedAt); put("amountReceivedPaise", lot.amountReceivedPaise); put("monthlyRateBasisPoints", lot.monthlyRateBasisPoints); put("note", lot.note); put("status", lot.status); put("closedAt", lot.closedAt ?: JSONObject.NULL)
            put("items", JSONArray().apply { lot.items.forEach { item -> put(JSONObject().apply { put("girviId", item.girviId); put("itemId", item.itemId); put("addedAt", item.addedAt); put("removedAt", item.removedAt ?: JSONObject.NULL) }) } })
            put("fundingAdvances", JSONArray().apply { lot.fundingAdvances.forEach { advance -> put(JSONObject().apply {
                put("id", advance.id); put("amountPaise", advance.amountPaise); put("monthlyRateBasisPoints", advance.monthlyRateBasisPoints); put("createdAt", advance.createdAt); put("interestRule", advance.interestRule.name); put("note", advance.note)
            }) } })
            put("fundingPayments", JSONArray().apply { lot.fundingPayments.forEach { payment -> put(JSONObject().apply {
                put("id", payment.id); put("amountPaise", payment.amountPaise); put("createdAt", payment.createdAt); put("note", payment.note); put("isReversal", payment.isReversal); put("reversedPaymentId", payment.reversedPaymentId ?: JSONObject.NULL)
            }) } })
        }) } })
        put("movements", JSONArray().apply { snapshot.movements.forEach { value -> put(JSONObject().apply {
            put("id", value.id); put("girviId", value.girviId); put("itemId", value.itemId); put("destinationType", value.destinationType); put("destinationId", value.destinationId); put("lotId", value.lotId ?: JSONObject.NULL); put("movedAt", value.movedAt); put("note", value.note); put("createdAt", value.createdAt)
        }) } })
    }.toString()

    private fun decode(raw: String): CustodyPlacementSnapshot {
        val root = JSONObject(raw)
        val locations = root.optJSONArray("locations") ?: JSONArray()
        val parties = root.optJSONArray("parties") ?: JSONArray()
        val lots = root.optJSONArray("lots") ?: JSONArray()
        val movements = root.optJSONArray("movements") ?: JSONArray()
        return CustodyPlacementSnapshot(
            schemaVersion = CustodyPlacementSnapshot.CURRENT_SCHEMA,
            locations = List(locations.length()) { index -> locations.getJSONObject(index).run {
                StorageLocation(getString("id"), getString("name"), optString("type", "OTHER"), optString("detail"), optBoolean("active", true), optLong("createdAt"))
            } },
            parties = List(parties.length()) { index -> parties.getJSONObject(index).run {
                ExternalParty(getString("id"), getString("name"), optString("mobile"), optString("address"), optInt("defaultMonthlyRateBasisPoints"), optString("note"), optBoolean("active", true), optLong("createdAt"))
            } },
            lots = List(lots.length()) { index -> lots.getJSONObject(index).run {
                val itemArray = optJSONArray("items") ?: JSONArray()
                val lotId = getString("id")
                val openedAt = getLong("openedAt")
                val legacyAmount = getLong("amountReceivedPaise")
                val legacyRate = optInt("monthlyRateBasisPoints")
                val advanceArray = optJSONArray("fundingAdvances")
                val paymentArray = optJSONArray("fundingPayments") ?: JSONArray()
                val advances = if (advanceArray == null && legacyAmount > 0L) {
                    listOf(ExternalFundingAdvance(id = "initial-$lotId", amountPaise = legacyAmount, monthlyRateBasisPoints = legacyRate, createdAt = openedAt, interestRule = ExternalInterestRule.EXACT_DAYS, note = "Migrated initial lot funding"))
                } else {
                    val array = advanceArray ?: JSONArray()
                    List(array.length()) { advanceIndex -> array.getJSONObject(advanceIndex).run {
                        ExternalFundingAdvance(
                            id = getString("id"), amountPaise = getLong("amountPaise"), monthlyRateBasisPoints = optInt("monthlyRateBasisPoints"), createdAt = getLong("createdAt"),
                            interestRule = runCatching { ExternalInterestRule.valueOf(optString("interestRule", ExternalInterestRule.EXACT_DAYS.name)) }.getOrDefault(ExternalInterestRule.EXACT_DAYS),
                            note = optString("note"),
                        )
                    } }
                }
                PlacementLot(
                    id = lotId, lotNumber = getString("lotNumber"), partyId = getString("partyId"), openedAt = openedAt, amountReceivedPaise = legacyAmount, monthlyRateBasisPoints = legacyRate, note = optString("note"), status = optString("status", "ACTIVE"),
                    items = List(itemArray.length()) { itemIndex -> itemArray.getJSONObject(itemIndex).run { PlacementItem(getString("girviId"), getString("itemId"), getLong("addedAt"), optNullableLong("removedAt")) } },
                    fundingAdvances = advances,
                    fundingPayments = List(paymentArray.length()) { paymentIndex -> paymentArray.getJSONObject(paymentIndex).run {
                        ExternalFundingPayment(getString("id"), getLong("amountPaise"), getLong("createdAt"), optString("note"), optBoolean("isReversal", false), optNullableString("reversedPaymentId"))
                    } },
                    closedAt = optNullableLong("closedAt"),
                )
            } },
            movements = List(movements.length()) { index -> movements.getJSONObject(index).run {
                CustodyMovement(getString("id"), getString("girviId"), getString("itemId"), getString("destinationType"), getString("destinationId"), optNullableString("lotId"), getLong("movedAt"), optString("note"), optLong("createdAt"))
            } },
        )
    }

    private fun JSONObject.optNullableLong(name: String): Long? = if (!has(name) || isNull(name)) null else getLong(name)
    private fun JSONObject.optNullableString(name: String): String? = if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    companion object {
        private const val FILE_NAME = "custody_placement_v1.bin"
        private const val MAGIC = 0x474B4331
        private const val FORMAT_VERSION = 1
        private const val MAX_BYTES = 32 * 1024 * 1024
        private val AAD = "girvi-khata-custody-placement-v1".toByteArray(Charsets.UTF_8)
    }
}
