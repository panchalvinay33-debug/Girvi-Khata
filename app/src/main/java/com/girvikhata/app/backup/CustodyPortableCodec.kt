package com.girvikhata.app.backup

import com.girvikhata.app.custody.CustodyMovement
import com.girvikhata.app.custody.CustodyPlacementSnapshot
import com.girvikhata.app.custody.ExternalParty
import com.girvikhata.app.custody.PlacementItem
import com.girvikhata.app.custody.PlacementLot
import com.girvikhata.app.custody.StorageLocation
import com.girvikhata.app.domain.ExternalFundingAdvance
import com.girvikhata.app.domain.ExternalFundingPayment
import com.girvikhata.app.domain.ExternalInterestRule
import org.json.JSONArray
import org.json.JSONObject

object CustodyPortableCodec {
    fun encode(snapshot: CustodyPlacementSnapshot): ByteArray = JSONObject().apply {
        put("schemaVersion", snapshot.schemaVersion)
        put("locations", JSONArray().apply { snapshot.locations.forEach { value -> put(JSONObject().apply {
            put("id", value.id); put("name", value.name); put("type", value.type); put("detail", value.detail); put("active", value.active); put("createdAt", value.createdAt)
        }) } })
        put("parties", JSONArray().apply { snapshot.parties.forEach { value -> put(JSONObject().apply {
            put("id", value.id); put("name", value.name); put("mobile", value.mobile); put("address", value.address); put("defaultMonthlyRateBasisPoints", value.defaultMonthlyRateBasisPoints); put("note", value.note); put("active", value.active); put("createdAt", value.createdAt)
        }) } })
        put("lots", JSONArray().apply { snapshot.lots.forEach { lot -> put(JSONObject().apply {
            put("id", lot.id); put("lotNumber", lot.lotNumber); put("partyId", lot.partyId); put("openedAt", lot.openedAt); put("amountReceivedPaise", lot.amountReceivedPaise); put("monthlyRateBasisPoints", lot.monthlyRateBasisPoints); put("note", lot.note); put("status", lot.status); put("closedAt", lot.closedAt ?: JSONObject.NULL)
            put("items", JSONArray().apply { lot.items.forEach { item -> put(JSONObject().apply {
                put("girviId", item.girviId); put("itemId", item.itemId); put("addedAt", item.addedAt); put("removedAt", item.removedAt ?: JSONObject.NULL)
            }) } })
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
    }.toString().toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): CustodyPlacementSnapshot {
        require(bytes.isNotEmpty()) { "Custody backup empty" }
        require(bytes.size <= MAX_BYTES) { "Custody backup too large" }
        val root = JSONObject(String(bytes, Charsets.UTF_8))
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
                val itemsArray = optJSONArray("items") ?: JSONArray()
                val advanceArray = optJSONArray("fundingAdvances") ?: JSONArray()
                val paymentArray = optJSONArray("fundingPayments") ?: JSONArray()
                PlacementLot(
                    id = getString("id"),
                    lotNumber = getString("lotNumber"),
                    partyId = getString("partyId"),
                    openedAt = getLong("openedAt"),
                    amountReceivedPaise = optLong("amountReceivedPaise"),
                    monthlyRateBasisPoints = optInt("monthlyRateBasisPoints"),
                    note = optString("note"),
                    status = optString("status", "ACTIVE"),
                    items = List(itemsArray.length()) { itemIndex -> itemsArray.getJSONObject(itemIndex).run {
                        PlacementItem(getString("girviId"), getString("itemId"), getLong("addedAt"), nullableLong("removedAt"))
                    } },
                    fundingAdvances = List(advanceArray.length()) { advanceIndex -> advanceArray.getJSONObject(advanceIndex).run {
                        ExternalFundingAdvance(
                            id = getString("id"),
                            amountPaise = getLong("amountPaise"),
                            monthlyRateBasisPoints = optInt("monthlyRateBasisPoints"),
                            createdAt = getLong("createdAt"),
                            interestRule = runCatching { ExternalInterestRule.valueOf(optString("interestRule", ExternalInterestRule.EXACT_DAYS.name)) }.getOrDefault(ExternalInterestRule.EXACT_DAYS),
                            note = optString("note"),
                        )
                    } },
                    fundingPayments = List(paymentArray.length()) { paymentIndex -> paymentArray.getJSONObject(paymentIndex).run {
                        ExternalFundingPayment(getString("id"), getLong("amountPaise"), getLong("createdAt"), optString("note"), optBoolean("isReversal", false), nullableString("reversedPaymentId"))
                    } },
                    closedAt = nullableLong("closedAt"),
                )
            } },
            movements = List(movements.length()) { index -> movements.getJSONObject(index).run {
                CustodyMovement(getString("id"), getString("girviId"), getString("itemId"), getString("destinationType"), getString("destinationId"), nullableString("lotId"), getLong("movedAt"), optString("note"), optLong("createdAt"))
            } },
        ).also(::validate)
    }

    private fun validate(snapshot: CustodyPlacementSnapshot) {
        require(snapshot.locations.map { it.id }.distinct().size == snapshot.locations.size) { "Custody duplicate location" }
        require(snapshot.parties.map { it.id }.distinct().size == snapshot.parties.size) { "Custody duplicate party" }
        require(snapshot.lots.map { it.id }.distinct().size == snapshot.lots.size) { "Custody duplicate lot" }
        require(snapshot.lots.map { it.lotNumber.lowercase() }.distinct().size == snapshot.lots.size) { "Custody duplicate lot number" }
        val partyIds = snapshot.parties.map { it.id }.toSet()
        val locationIds = snapshot.locations.map { it.id }.toSet()
        require(snapshot.lots.all { it.partyId in partyIds }) { "Custody lot party missing" }
        val activeItems = snapshot.lots.flatMap { it.items.filter { item -> item.removedAt == null }.map { item -> item.itemId } }
        require(activeItems.distinct().size == activeItems.size) { "Custody item in multiple active lots" }
        snapshot.movements.forEach { movement ->
            require(movement.girviId.isNotBlank() && movement.itemId.isNotBlank() && movement.movedAt > 0) { "Custody movement invalid" }
            if (movement.destinationType == "LOCATION") require(movement.destinationId in locationIds) { "Custody movement location missing" }
            else if (movement.destinationType == "EXTERNAL") require(movement.destinationId in partyIds && !movement.lotId.isNullOrBlank()) { "Custody external movement invalid" }
            else error("Custody destination invalid")
        }
    }

    private fun JSONObject.nullableLong(name: String): Long? = if (!has(name) || isNull(name)) null else getLong(name)
    private fun JSONObject.nullableString(name: String): String? = if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
    private const val MAX_BYTES = 16 * 1024 * 1024
}
