package com.nexapos.retail.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A customer or supplier. [type] is CUSTOMER or SUPPLIER. */
@Entity(
    tableName = "parties",
    indices = [Index("type")],
)
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val locality: String = "",
    val type: String = TYPE_CUSTOMER,
    /** Outstanding balance owed by/to this party, in minor units. */
    val balanceCents: Long = 0,
    val createdAt: Long = 0,
    val isActive: Boolean = true,
) {
    companion object {
        const val TYPE_CUSTOMER = "CUSTOMER"
        const val TYPE_SUPPLIER = "SUPPLIER"
    }
}
