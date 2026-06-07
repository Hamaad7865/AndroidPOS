package com.nexapos.retail.ui.purchase.receipt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexapos.retail.data.media.ImageStore
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.FormSection
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.PrimaryBtn
import com.nexapos.retail.ui.components.SecBtn
import com.nexapos.retail.ui.components.SumRow
import com.nexapos.retail.ui.components.WideBtn
import com.nexapos.retail.ui.components.rsStr
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme

@Composable
fun ReceiptScanScreen(
    vm: ReceiptScanViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // Camera capture writes a temp JPEG into cacheDir/receipts (declared in
    // res/xml/file_paths.xml) and hands it to the VM via a FileProvider URI.
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val takePhoto =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val uri = pendingUri
            if (ok && uri != null) vm.onImageCaptured(context, uri)
        }
    val pickImage =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) vm.onImageCaptured(context, uri)
        }
    fun launchCamera() {
        val dir = java.io.File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = java.io.File(dir, "capture-${System.currentTimeMillis()}.jpg")
        val uri =
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        pendingUri = uri
        takePhoto.launch(uri)
    }

    NavShell(active = "purchase", onNav = onNav) {
        AppBar(
            title = "Scan receipt",
            subtitle = "Photograph a supplier receipt",
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecBtn(null, "Cancel") {
                        vm.reset()
                        onBack()
                    }
                }
            },
        )
        when (vm.phase) {
            ScanPhase.IDLE ->
                IdleBody(
                    onTakePhoto = { launchCamera() },
                    onPickImage = { pickImage.launch("image/*") },
                )
            ScanPhase.PROCESSING -> ProcessingBody()
            ScanPhase.REVIEW ->
                ReviewBody(
                    vm = vm,
                    onBack = onBack,
                )
            ScanPhase.DONE -> Unit // register() already calls onBack()
        }
    }
}

@Composable
private fun IdleBody(
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
) {
    val c = PosTheme.colors
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(c.amberSoft),
            contentAlignment = Alignment.Center,
        ) { PosIcon(PosIcons.receipt, tint = c.amberPress, size = 30.dp) }
        Spacer(Modifier.height(16.dp))
        Text("Scan a supplier receipt", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "Take a photo or pick an image of a printed receipt. We'll read the items so you can review and register them as a purchase order.",
            fontSize = 13.sp,
            color = c.muted,
            modifier = Modifier.fillMaxWidth(0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryBtn(PosIcons.scan, "Take photo") { onTakePhoto() }
            SecBtn(PosIcons.upload, "Pick image") { onPickImage() }
        }
    }
}

@Composable
private fun ProcessingBody() {
    val c = PosTheme.colors
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = c.amber)
        Spacer(Modifier.height(14.dp))
        Text("Reading receipt…", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text("Recognising the items on the photo.", fontSize = 12.sp, color = c.muted)
    }
}

@Composable
private fun ReviewBody(
    vm: ReceiptScanViewModel,
    onBack: () -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    val thumb: ImageBitmap? = remember(vm.imageName) { ImageStore.load(context, vm.imageName) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FormSection("Receipt") {
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = "Scanned receipt",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, c.hairline, RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(12.dp))
            }
            EditableField(
                "Supplier",
                vm.supplier,
                { vm.supplier = it },
                Modifier.fillMaxWidth(),
                placeholder = "Who issued this receipt?",
            )
            if (vm.warnings.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                WarningBox(vm.warnings)
            }
        }
        FormSection("Items") {
            if (vm.lines.isEmpty()) {
                Text(
                    "No items recognised — tap Add line to enter them manually.",
                    fontSize = 12.sp,
                    color = c.muted,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    vm.lines.forEachIndexed { i, line ->
                        ReceiptLineRow(
                            line = line,
                            known = vm.isKnown(line),
                            onChange = { vm.updateLine(i, it) },
                            onRemove = { vm.removeLine(i) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SecBtn(PosIcons.plus, "Add line") { vm.addBlankLine() }
        }
        FormSection("Total") {
            SumRow("Total", rsStr(vm.total), mono = true)
            Spacer(Modifier.height(8.dp))
            Text(
                "Registers as a cash purchase order. New item names are added to your catalog; existing ones have their stock raised.",
                fontSize = 11.sp,
                color = c.muted,
            )
            Spacer(Modifier.height(12.dp))
            WideBtn(
                "Register purchase",
                primary = true,
                Modifier.fillMaxWidth(),
                icon = PosIcons.check,
                onClick = { vm.register { onBack() } },
            )
        }
    }
}

@Composable
private fun WarningBox(warnings: List<String>) {
    val c = PosTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.amberSoft)
            .border(1.dp, c.amberPress, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        warnings.forEach { w ->
            Text("• $w", fontSize = 12.sp, color = c.amberPress)
        }
    }
}

@Composable
private fun ReceiptLineRow(
    line: ReceiptDraftLine,
    known: Boolean,
    onChange: (ReceiptDraftLine) -> Unit,
    onRemove: () -> Unit,
) {
    val c = PosTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, c.hairline2, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EditableField(
                "Item",
                line.name,
                { onChange(line.copy(name = it)) },
                Modifier.weight(1f),
                placeholder = "Item name",
            )
            EditableField(
                "Qty",
                if (line.quantity > 0) line.quantity.toString() else "",
                { onChange(line.copy(quantity = it.toIntOrNull() ?: 1)) },
                Modifier.width(76.dp),
                mono = true,
                number = true,
                placeholder = "1",
            )
            EditableField(
                "Unit cost",
                if (line.unitCostRupees > 0) line.unitCostRupees.toString() else "",
                { onChange(line.copy(unitCostRupees = it.toIntOrNull() ?: 0)) },
                Modifier.width(120.dp),
                mono = true,
                number = true,
                right = "Rs",
                placeholder = "0",
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Tag(if (known) "In catalog" else "New", known)
            Box(
                Modifier.size(28.dp).clip(CircleShape).clickable { onRemove() },
                contentAlignment = Alignment.Center,
            ) { PosIcon(PosIcons.trash, tint = c.muted, size = 14.dp) }
        }
    }
}

@Composable
private fun Tag(
    text: String,
    known: Boolean,
) {
    val c = PosTheme.colors
    val (bg, fg) = if (known) c.emeraldSoft to c.emerald else c.amberSoft to c.amberPress
    Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(text, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}
