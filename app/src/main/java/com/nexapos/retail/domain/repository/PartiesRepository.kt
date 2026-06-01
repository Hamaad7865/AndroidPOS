package com.nexapos.retail.domain.repository

import com.nexapos.retail.data.entity.Party
import kotlinx.coroutines.flow.Flow

/** Read/write access to customers and suppliers. */
interface PartiesRepository {
    fun observeCustomers(): Flow<List<Party>>

    fun observeSuppliers(): Flow<List<Party>>

    fun observeCustomerCount(): Flow<Int>

    fun observeSupplierCount(): Flow<Int>

    suspend fun upsert(party: Party): Long

    /** Loads a single party by id, or null. */
    suspend fun getParty(id: Long): Party?

    /**
     * Adjusts a party's outstanding balance by [deltaCents] (positive = the
     * party owes more, e.g. a credit sale; negative = a repayment).
     */
    suspend fun adjustBalance(
        partyId: Long,
        deltaCents: Long,
    )

    suspend fun delete(party: Party)
}
