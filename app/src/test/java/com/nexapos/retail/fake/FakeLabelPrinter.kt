package com.nexapos.retail.fake

import com.nexapos.retail.domain.hardware.LabelPrinter
import com.nexapos.retail.domain.hardware.LabelSpec
import com.nexapos.retail.domain.hardware.PrintOutcome

/** In-memory [LabelPrinter]: records every batch; can fail once at a given index. */
class FakeLabelPrinter(
    private var failOnceAtIndex: Int? = null,
) : LabelPrinter {
    val batches = mutableListOf<List<LabelSpec>>()

    override suspend fun print(
        labels: List<LabelSpec>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): PrintOutcome {
        batches += labels
        labels.forEachIndexed { i, _ ->
            failOnceAtIndex?.let { failAt ->
                if (i == failAt) {
                    failOnceAtIndex = null
                    return PrintOutcome.FailedAt(i, "printer unplugged")
                }
            }
            onProgress(i + 1, labels.size)
        }
        return PrintOutcome.Done
    }
}
