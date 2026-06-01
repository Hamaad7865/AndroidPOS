package com.nexapos.retail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.ui.theme.HankenGrotesk
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme

/** Machined card with an eyebrow heading — the standard form section container. */
@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = PosTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.raised)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(20.dp),
    ) {
        Eyebrow(title)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted)
}

/** Read-only display field (the prototype's pre-filled inputs). */
@Composable
fun LabeledField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    leftSearch: Boolean = false,
    right: String? = null,
    drop: Boolean = false,
    tall: Boolean = false,
) {
    val c = PosTheme.colors
    Column(modifier) {
        FieldLabel(label)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (tall) Modifier.heightIn(min = 64.dp) else Modifier.height(42.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(c.raised2)
                .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = if (tall) 10.dp else 0.dp),
            verticalAlignment = if (tall) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (leftSearch) PosIcon(PosIcons.search, tint = c.muted, size = 16.dp)
            Text(
                value.ifEmpty { " " },
                modifier = Modifier.weight(1f),
                fontFamily = if (mono) JetBrainsMono else HankenGrotesk,
                fontSize = 14.sp,
                fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Medium,
                color = if (value.isEmpty()) c.muted else c.ink,
                maxLines = if (tall) 3 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (drop) PosIcon(PosIcons.chevD, tint = c.muted, size = 14.dp)
            if (right != null) Text(right, fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.muted)
        }
    }
}

/** Editable text field styled to match [LabeledField]; state is hoisted by the caller. */
@Composable
fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    mono: Boolean = false,
    number: Boolean = false,
    right: String? = null,
    tall: Boolean = false,
) {
    val c = PosTheme.colors
    val family = if (mono) JetBrainsMono else HankenGrotesk
    // Focus the editor when the user taps anywhere in the field box (not just on
    // the text itself), and let the editor fill the full width as the tap target.
    val focus = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    Column(modifier) {
        FieldLabel(label)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (tall) Modifier.heightIn(min = 64.dp) else Modifier.height(42.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(c.raised2)
                .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                .clickable(interactionSource = interaction, indication = null) { focus.requestFocus() }
                .padding(horizontal = 12.dp, vertical = if (tall) 10.dp else 0.dp),
            verticalAlignment = if (tall) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = !tall,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    textStyle =
                        TextStyle(
                            fontFamily = family,
                            fontSize = 14.sp,
                            fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Medium,
                            color = c.ink,
                        ),
                    keyboardOptions =
                        if (number) {
                            KeyboardOptions(keyboardType = KeyboardType.Number)
                        } else {
                            KeyboardOptions.Default
                        },
                    cursorBrush = SolidColor(c.amber),
                )
                if (value.isEmpty()) Text(placeholder, fontFamily = family, fontSize = 14.sp, color = c.muted)
            }
            if (right != null) Text(right, fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.muted)
        }
    }
}

/**
 * Field shaped like [EditableField] that opens a dropdown of [options]. Picking
 * one calls [onValueChange]. Users can also type their own value (which lets
 * the caller auto-create the category/brand on save).
 */
@Composable
fun PickerField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    mono: Boolean = false,
    allowFreeText: Boolean = true,
) {
    val c = PosTheme.colors
    val family = if (mono) JetBrainsMono else HankenGrotesk
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        FieldLabel(label)
        Spacer(Modifier.height(6.dp))
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.raised2)
                    .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (allowFreeText) {
                    Box(Modifier.weight(1f)) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle =
                                TextStyle(
                                    fontFamily = family,
                                    fontSize = 14.sp,
                                    fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Medium,
                                    color = c.ink,
                                ),
                            cursorBrush = SolidColor(c.amber),
                        )
                        if (value.isEmpty()) Text(placeholder, fontFamily = family, fontSize = 14.sp, color = c.muted)
                    }
                } else {
                    Text(
                        value.ifEmpty { placeholder },
                        Modifier.weight(1f).clickable { expanded = true },
                        fontFamily = family,
                        fontSize = 14.sp,
                        fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (value.isEmpty()) c.muted else c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.clickable { expanded = !expanded }) {
                    PosIcon(PosIcons.chevD, tint = c.muted, size = 14.dp)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Type a new one above", fontSize = 13.sp, color = c.muted) },
                        onClick = { expanded = false },
                    )
                } else {
                    options.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, fontSize = 14.sp, color = c.ink) },
                            onClick = {
                                onValueChange(opt)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SumRow(
    label: String,
    value: String,
    mono: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val c = PosTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = c.muted)
        Text(value, fontFamily = if (mono) JetBrainsMono else HankenGrotesk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}

@Composable
fun GhostBtn(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted)
    }
}

/** Full-height (44dp) button used in form footers and summaries. */
@Composable
fun WideBtn(
    label: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    icon: List<String>? = null,
    onClick: () -> Unit = {},
) {
    val c = PosTheme.colors
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (primary) c.amber else c.raised)
            .then(if (primary) Modifier else Modifier.border(1.dp, c.hairline, RoundedCornerShape(10.dp)))
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) PosIcon(icon, tint = if (primary) Color.White else c.ink, size = 16.dp)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (primary) Color.White else c.ink)
    }
}
