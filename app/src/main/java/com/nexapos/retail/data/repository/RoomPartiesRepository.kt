package com.nexapos.retail.data.repository

import com.nexapos.retail.data.dao.PartyDao
import com.nexapos.retail.data.entity.Party
import com.nexapos.retail.domain.repository.PartiesRepository
import kotlinx.coroutines.flow.Flow

/** Room-backed [PartiesRepository]. */
class RoomPartiesRepository(private val partyDao: PartyDao) : PartiesRepository {
    override fun observeCustomers(): Flow<List<Party>> = partyDao.observeByType(Party.TYPE_CUSTOMER)

    override fun observeSuppliers(): Flow<List<Party>> = partyDao.observeByType(Party.TYPE_SUPPLIER)

    override fun observeCustomerCount(): Flow<Int> = partyDao.observeCountByType(Party.TYPE_CUSTOMER)

    override fun observeSupplierCount(): Flow<Int> = partyDao.observeCountByType(Party.TYPE_SUPPLIER)

    override suspend fun upsert(party: Party): Long = partyDao.upsert(party)

    override suspend fun getParty(id: Long): Party? = partyDao.getById(id)

    override suspend fun adjustBalance(
        partyId: Long,
        deltaCents: Long,
    ) = partyDao.adjustBalance(partyId, deltaCents)

    override suspend fun delete(party: Party) = partyDao.delete(party)
}
