package com.nexapos.retail.fake

import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.domain.repository.PartiesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [PartiesRepository] for unit tests. */
class FakePartiesRepository(initial: List<Party> = emptyList()) : PartiesRepository {
    private val parties = MutableStateFlow(initial)

    override fun observeCustomers(): Flow<List<Party>> = parties.map { it.filter { p -> p.type == Party.TYPE_CUSTOMER } }

    override fun observeSuppliers(): Flow<List<Party>> = parties.map { it.filter { p -> p.type == Party.TYPE_SUPPLIER } }

    override fun observeCustomerCount(): Flow<Int> = parties.map { it.count { p -> p.type == Party.TYPE_CUSTOMER } }

    override fun observeSupplierCount(): Flow<Int> = parties.map { it.count { p -> p.type == Party.TYPE_SUPPLIER } }

    override suspend fun upsert(party: Party): Long {
        val nextId = if (party.id == 0L) (parties.value.maxOfOrNull { it.id } ?: 0L) + 1 else party.id
        val stored = party.copy(id = nextId)
        parties.value = parties.value.filterNot { it.id == stored.id } + stored
        return nextId
    }

    override suspend fun getParty(id: Long): Party? = parties.value.firstOrNull { it.id == id }

    override suspend fun adjustBalance(
        partyId: Long,
        deltaCents: Long,
    ) {
        parties.value =
            parties.value.map {
                if (it.id == partyId) it.copy(balanceCents = it.balanceCents + deltaCents) else it
            }
    }

    override suspend fun delete(party: Party) {
        parties.value = parties.value.filterNot { it.id == party.id }
    }
}
