package com.nexapos.retail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.barcode.Ean13
import com.nexapos.retail.ui.theme.JetBrainsMono

/**
 * Renders a 13-digit EAN-13 value as standard barcode bars on a white card,
 * with the numeric reading underneath. Falls back to a quiet "invalid"
 * message when the value isn't a valid EAN-13.
 */
@Composable
fun Ean13Bars(
    value: String,
    modifier: Modifier = Modifier,
    barHeightDp: Int = 56,
) {
    if (!Ean13.isValid(value)) {
        Text(
            "Not a valid 13-digit EAN — tap Generate or scan one.",
            modifier = modifier,
            fontSize = 11.sp,
            color = Color(0xFFB43329),
        )
        return
    }
    val pattern = Ean13.encode(value)
    Column(
        modifier.background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(barHeightDp.dp)) {
            val moduleWidth = size.width / pattern.length
            pattern.forEachIndexed { i, ch ->
                if (ch == '1') {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(i * moduleWidth, 0f),
                        size = Size(moduleWidth, size.height),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
        )
    }
}
