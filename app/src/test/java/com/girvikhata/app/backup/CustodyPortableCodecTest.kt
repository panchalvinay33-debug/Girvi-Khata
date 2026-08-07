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
import org.junit.Assert.assertEquals
import org.junit.Test

class CustodyPortableCodecTest {
    @Test
    fun roundTrip_preservesLocationsLotsMovementsAndFinance() {
        val location = StorageLocation(id = "loc-1", name = "Home Locker 1", type = "HOME", detail = "Box A", createdAt = 10L)
        val party = ExternalParty(id = "party-1", name = "Sharma Finance", mobile = "9876543210", defaultMonthlyRateBasisPoints = 150, createdAt = 11L)
        val lot = PlacementLot(
            id = "lot-1",
            lotNumber = "LOT-001",
            partyId = party.id,
            openedAt = 100L,
            amountReceivedPaise = 80_000_00,
            monthlyRateBasisPoints = 150,
            items = listOf(PlacementItem("g-1", "i-1", 100L)),
            fundingAdvances = listOf(ExternalFundingAdvance("a-1", 80_000_00, 150, 100L, ExternalInterestRule.EXACT_DAYS, "Initial")),
            fundingPayments = listOf(ExternalFundingPayment("p-1", 10_000_00, 200L, "Paid")),
        )
        val snapshot = CustodyPlacementSnapshot(
            locations = listOf(location),
            parties = listOf(party),
            lots = listOf(lot),
            movements = listOf(CustodyMovement("m-1", "g-1", "i-1", "EXTERNAL", party.id, lot.id, 100L, "Placed", 101L)),
        )
        assertEquals(snapshot, CustodyPortableCodec.decode(CustodyPortableCodec.encode(snapshot)))
    }
}
