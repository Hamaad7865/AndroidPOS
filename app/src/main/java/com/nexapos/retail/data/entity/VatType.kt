package com.nexapos.retail.data.entity

/**
 * VAT classification for a product (Mauritius MRA). Standard is the 15% rate;
 * Exempt and Zero-rated both charge 0% at the till — the distinction is kept for
 * reporting (zero-rated can reclaim input VAT, exempt cannot).
 */
enum class VatType(val id: String, val ratePercent: Double, val label: String) {
    STANDARD("STANDARD", 15.0, "Standard (15%)"),
    EXEMPT("EXEMPT", 0.0, "Exempt"),
    ZERO_RATED("ZERO_RATED", 0.0, "Zero-rated"),
    ;

    companion object {
        fun from(id: String?): VatType = entries.firstOrNull { it.id == id } ?: STANDARD
    }
}
