package com.nexapos.retail.data.branch

import com.nexapos.retail.fake.FakeCatalogRepository
import com.nexapos.retail.fake.FakeMoneyRepository
import com.nexapos.retail.fake.FakeRemoteStore
import com.nexapos.retail.fake.FakeReturnsRepository
import com.nexapos.retail.fake.FakeSalesRepository
import com.nexapos.retail.fake.FakeShiftRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class RealBranchSyncTest {
    private fun engine(
        remote: FakeRemoteStore,
        scope: CoroutineScope,
    ) = RealBranchSync(
        remote = remote,
        sales = FakeSalesRepository(),
        catalog = FakeCatalogRepository(),
        returns = FakeReturnsRepository(),
        money = FakeMoneyRepository(),
        shifts = FakeShiftRepository(),
        branchCode = "A",
        branchName = "Curepipe",
        isHq = false,
        scope = scope,
        zone = ZoneId.of("UTC"),
        // Fixed clock: 2023-11-14T22:13:20Z.
        now = { 1_700_000_000_000L },
    )

    @Test
    fun `syncNow when signed in writes the summary and today's day doc`() =
        runTest {
            val remote = FakeRemoteStore(uid = "U")
            val sync = engine(remote, backgroundScope)
            val result = sync.syncNow()
            assertEquals(SyncResult.Ok, result)
            assertTrue(remote.written.containsKey("businesses/U/branches/A/state/summary"))
            assertTrue(remote.written.containsKey("businesses/U/branches/A/days/2023-11-14"))
            assertEquals(SyncState.OK, sync.status.value.state)
        }

    @Test
    fun `syncNow without a session fails and writes nothing`() =
        runTest {
            val remote = FakeRemoteStore(uid = null)
            val sync = engine(remote, backgroundScope)
            val result = sync.syncNow()
            assertTrue(result is SyncResult.Failed)
            assertTrue(remote.written.isEmpty())
            assertEquals(SyncState.NOT_SIGNED_IN, sync.status.value.state)
        }

    @Test
    fun `a write failure surfaces OFFLINE, not an exception`() =
        runTest {
            val remote = FakeRemoteStore(uid = "U", failOnWrite = true)
            val sync = engine(remote, backgroundScope)
            val result = sync.syncNow()
            assertTrue(result is SyncResult.Failed)
            assertEquals(SyncState.OFFLINE, sync.status.value.state)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onSaleRecorded never throws and swallows a write failure`() =
        runTest {
            val remote = FakeRemoteStore(uid = "U", failOnWrite = true)
            // Unconfined so the fire-and-forget push runs eagerly within the call.
            val sync = engine(remote, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
            sync.onSaleRecorded() // must return immediately without throwing
            assertEquals(SyncState.OFFLINE, sync.status.value.state)
            assertFalse(remote.written.containsKey("businesses/U/branches/A/state/summary"))
        }
}
