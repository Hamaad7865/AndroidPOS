package com.nexapos.retail.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    /** Parent main category id; null = this IS a main category. A nullable column needs no
     *  SQL default, so Room's expected schema matches the v8→v9 migration (no destructive wipe). */
    val parentId: Long? = null,
)
