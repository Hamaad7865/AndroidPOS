package com.nexapos.retail.fake

import com.nexapos.retail.domain.hardware.DrawerKicker
import com.nexapos.retail.domain.hardware.KickReason
import com.nexapos.retail.domain.hardware.KickResult

/** Records kicks so tests can assert when (and when not) the drawer fires. */
class FakeDrawerKicker : DrawerKicker {
    val kicks = mutableListOf<KickReason>()

    override fun kick(reason: KickReason) {
        kicks += reason
    }

    override suspend fun kickNow(reason: KickReason): KickResult {
        kicks += reason
        return KickResult.Sent
    }
}
