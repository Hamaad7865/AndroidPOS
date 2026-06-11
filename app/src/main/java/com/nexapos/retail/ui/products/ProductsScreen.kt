package com.nexapos.retail.ui.products

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.barcode.BarcodeScanner
import com.nexapos.retail.data.barcode.Ean13
import com.nexapos.retail.data.entity.VatType
import com.nexapos.retail.data.media.ImageStore
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.domain.repository.ProductUsage
import com.nexapos.retail.ui.components.AppBar
import com.nexapos.retail.ui.components.Ean13Bars
import com.nexapos.retail.ui.components.EditableField
import com.nexapos.retail.ui.components.NavShell
import com.nexapos.retail.ui.components.PickerField
import com.nexapos.retail.ui.components.PosIcon
import com.nexapos.retail.ui.components.PosIcons
import com.nexapos.retail.ui.components.ProductThumb
import com.nexapos.retail.ui.sale.PosProduct
import com.nexapos.retail.ui.sale.categoryLabel
import com.nexapos.retail.ui.sale.matchesCategory
import com.nexapos.retail.ui.session.rememberIsAdmin
import com.nexapos.retail.ui.theme.HankenGrotesk
import com.nexapos.retail.ui.theme.JetBrainsMono
import com.nexapos.retail.ui.theme.PosTheme
import com.nexapos.retail.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Filter + sort state
// ---------------------------------------------------------------------------

private enum class StockFilter(val label: String) {
    ALL("All stock"),
    IN_STOCK("In stock"),
    LOW("Low stock (≤ 6)"),
    OUT("Out of stock"),
}

private enum class SortBy(val label: String) {
    NAME_AZ("Name (A → Z)"),
    NAME_ZA("Name (Z → A)"),
    PRICE_HI("Price (high → low)"),
    PRICE_LO("Price (low → high)"),
    STOCK_HI("Stock (high → low)"),
    STOCK_LO("Stock (low → high)"),
}

// ---------------------------------------------------------------------------
// Products list
// ---------------------------------------------------------------------------

@Composable
fun ProductsListScreen(
    vm: CatalogViewModel,
    onNav: (String) -> Unit,
    onAddProduct: () -> Unit,
    onOpenProduct: (productId: Long) -> Unit,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    // Cashiers never see cost data — gates the COST column and the CSV export.
    val admin = rememberIsAdmin()
    var query by remember { mutableStateOf("") }
    var selMain by remember { mutableStateOf<String?>(null) }
    var selSub by remember { mutableStateOf<String?>(null) }
    var stockFilter by remember { mutableStateOf(StockFilter.ALL) }
    var sortBy by remember { mutableStateOf(SortBy.NAME_AZ) }
    var showFilters by remember { mutableStateOf(false) }

    val rows =
        remember(vm.products, selMain, selSub, query, stockFilter, sortBy) {
            vm.products
                .filter { p ->
                    val catOk = matchesCategory(p.mainCat.ifEmpty { p.cat }, p.cat, selMain, selSub)
                    val qOk =
                        query.isBlank() ||
                            (p.name + p.sku + (p.barcode ?: "")).contains(query, ignoreCase = true)
                    val stockOk =
                        when (stockFilter) {
                            StockFilter.ALL -> true
                            StockFilter.IN_STOCK -> p.stock > 0
                            StockFilter.LOW -> p.stock in 1..6
                            StockFilter.OUT -> p.stock <= 0
                        }
                    catOk && qOk && stockOk
                }
                .let { list ->
                    when (sortBy) {
                        SortBy.NAME_AZ -> list.sortedBy { item -> item.name.lowercase() }
                        SortBy.NAME_ZA -> list.sortedByDescending { item -> item.name.lowercase() }
                        SortBy.PRICE_HI -> list.sortedByDescending { item -> item.priceCents }
                        SortBy.PRICE_LO -> list.sortedBy { item -> item.priceCents }
                        SortBy.STOCK_HI -> list.sortedByDescending { item -> item.stock }
                        SortBy.STOCK_LO -> list.sortedBy { item -> item.stock }
                    }
                }
        }

    // SAF: user picks a CSV file location.
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) {
                // Read the role at write time (not capture time): cashier exports
                // must not contain the cost column. Import only needs Name + Price.
                val canSeeCost = (context.applicationContext as PosApplication).container.session.isAdmin
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(productsCsv(vm.products, includeCost = canSeeCost).toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    Toast.makeText(context, "Exported ${vm.products.size} products", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

    // CSV import: explain the format, let the user pick a file, then show a summary.
    var showImportHelp by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val text =
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                if (text.isNullOrBlank()) {
                    Toast.makeText(context, "Couldn't read that file", Toast.LENGTH_LONG).show()
                } else {
                    vm.importProductsFromCsv(text) { result -> importResult = result }
                }
            }
        }

    val filterActive = stockFilter != StockFilter.ALL || sortBy != SortBy.NAME_AZ

    if (showFilters) {
        FiltersDialog(
            stockFilter = stockFilter,
            sortBy = sortBy,
            onStock = { stockFilter = it },
            onSort = { sortBy = it },
            onClear = {
                stockFilter = StockFilter.ALL
                sortBy = SortBy.NAME_AZ
            },
            onDismiss = { showFilters = false },
        )
    }

    if (showImportHelp) {
        ImportHelpDialog(
            onChoose = {
                showImportHelp = false
                importLauncher.launch("*/*")
            },
            onDismiss = { showImportHelp = false },
        )
    }
    importResult?.let { res ->
        ImportResultDialog(result = res, onDismiss = { importResult = null })
    }

    NavShell(active = "products", onNav = onNav) {
        AppBar(
            title = "Products",
            subtitle = "${vm.products.size} SKUs · ${vm.categoryTree.size} categories · ${Money.format(vm.stockValueCents)} stock value",
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecBtn(PosIcons.upload, "Import") { showImportHelp = true }
                    SecBtn(PosIcons.download, "Export") {
                        if (vm.products.isEmpty()) {
                            Toast.makeText(context, "No products to export yet", Toast.LENGTH_SHORT).show()
                        } else {
                            exportLauncher.launch("nexapos-products.csv")
                        }
                    }
                    SecBtn(PosIcons.print, "Print labels") {
                        val withCodes = vm.products.filter { it.barcode != null && Ean13.isValid(it.barcode) }
                        val toPrint = if (withCodes.isEmpty()) vm.products else withCodes
                        if (toPrint.isEmpty()) {
                            Toast.makeText(context, "No products to print", Toast.LENGTH_SHORT).show()
                        } else {
                            printProductLabels(context, toPrint, BusinessProfile.name(context))
                        }
                    }
                    PrimaryBtn(PosIcons.plus, "Add product", onAddProduct)
                }
            },
        )
        // search + chips + filters
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchField(query, { query = it }, Modifier.width(360.dp))
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Chip("All", selMain == null) {
                    selMain = null
                    selSub = null
                }
                vm.categoryTree.forEach { m ->
                    Chip(m.name, selMain == m.name) {
                        selMain = m.name
                        selSub = null
                    }
                }
                vm.categoryTree.firstOrNull { it.name == selMain }?.subs?.forEach { s ->
                    Chip("· ${s.name}", selSub == s.name) { selSub = s.name }
                }
            }
            SecBtn(PosIcons.filter, if (filterActive) "Filters ●" else "Filters") { showFilters = true }
        }
        // table card
        Box(Modifier.weight(1f).fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 22.dp)) {
            Column(
                Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)),
            ) {
                TableHead(showCost = admin)
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    if (rows.isEmpty()) {
                        EmptyRows(hasProducts = vm.products.isNotEmpty())
                    } else {
                        rows.forEach { p ->
                            val pid = p.id.toLongOrNull()
                            TableRow(p, showCost = admin) { if (pid != null) onOpenProduct(pid) }
                        }
                    }
                }
                // footer
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                Row(
                    Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Showing ${rows.size} of ${vm.products.size}", fontSize = 12.sp, color = c.muted)
                    Spacer(Modifier.weight(1f))
                    if (filterActive) {
                        Text(
                            "Filter: ${stockFilter.label} · Sort: ${sortBy.label}",
                            fontSize = 11.sp,
                            color = c.muted,
                            fontFamily = JetBrainsMono,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHead(showCost: Boolean) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.width(54.dp))
        HeadCell("PRODUCT", Modifier.weight(2f))
        HeadCell("CATEGORY", Modifier.weight(1f))
        if (showCost) HeadCell("COST", Modifier.width(80.dp), TextAlign.End)
        HeadCell("PRICE", Modifier.width(80.dp), TextAlign.End)
        HeadCell("STOCK", Modifier.width(64.dp), TextAlign.End)
        HeadCell("VALUE", Modifier.width(96.dp), TextAlign.End)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(PosTheme.colors.hairline))
}

@Composable
private fun HeadCell(
    text: String,
    modifier: Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(text, modifier = modifier, fontSize = 11.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted, textAlign = align)
}

@Composable
private fun TableRow(
    p: PosProduct,
    showCost: Boolean,
    onOpen: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable { onOpen() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(c.raised2).border(1.dp, c.hairline2, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            ProductThumb(imagePath = p.imagePath, kind = p.kind, size = 50.dp)
        }
        Column(Modifier.weight(2f)) {
            Text(p.name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = c.ink)
            val codeLine =
                buildString {
                    if (p.sku.isNotBlank()) append(p.sku)
                    if (!p.barcode.isNullOrBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append("BC ").append(p.barcode)
                    }
                    if (isEmpty()) append("no SKU / barcode")
                }
            Text(codeLine, fontFamily = JetBrainsMono, fontSize = 11.sp, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(categoryLabel(p.mainCat, p.cat), Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (showCost) {
            Text(if (p.costCents > 0L) Money.format(p.costCents) else "—", Modifier.width(80.dp), fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.graphite, textAlign = TextAlign.End)
        }
        Text(Money.format(p.priceCents), Modifier.width(80.dp), fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink, textAlign = TextAlign.End)
        Box(Modifier.width(64.dp), contentAlignment = Alignment.CenterEnd) { StockBadge(p.stock) }
        Text(Money.format(p.priceCents * p.stock), Modifier.width(96.dp), fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.End)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline2))
}

@Composable
private fun StockBadge(stock: Int) {
    val c = PosTheme.colors
    val (bg, fg) =
        when {
            stock <= 0 -> c.lowSoft to c.low
            stock <= 6 -> c.lowSoft to c.low
            stock <= 20 -> c.amberSoft to c.amberPress
            else -> c.emeraldSoft to c.emerald
        }
    Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text("$stock", fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun EmptyRows(hasProducts: Boolean) {
    val c = PosTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PosIcon(PosIcons.box, tint = c.muted, size = 28.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            if (hasProducts) "No products match these filters" else "No products yet",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = c.ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasProducts) {
                "Try clearing the search, category chip, or Filters."
            } else {
                "Tap Add product to create your first SKU."
            },
            fontSize = 12.sp,
            color = c.muted,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------------------
// Filters dialog
// ---------------------------------------------------------------------------

@Composable
private fun FiltersDialog(
    stockFilter: StockFilter,
    sortBy: SortBy,
    onStock: (StockFilter) -> Unit,
    onSort: (SortBy) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            TextButton(onClick = {
                onClear()
                onDismiss()
            }) { Text("Clear") }
        },
        title = { Text("Filter & sort") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Eyebrow("Stock")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StockFilter.entries.forEach { opt ->
                        RadioRow(label = opt.label, selected = stockFilter == opt) { onStock(opt) }
                    }
                }
                Eyebrow("Sort by")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SortBy.entries.forEach { opt ->
                        RadioRow(label = opt.label, selected = sortBy == opt) { onSort(opt) }
                    }
                }
            }
        },
    )
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (selected) c.amber else c.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(c.amber))
        }
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.ink)
    }
}

// ---------------------------------------------------------------------------
// CSV import dialogs
// ---------------------------------------------------------------------------

@Composable
private fun ImportHelpDialog(
    onChoose: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PosTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onChoose) { Text("Choose CSV file") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Import products from CSV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Pick a .csv file (e.g. saved from Excel or Google Sheets). The first row must be the column headers.",
                    fontSize = 13.sp,
                    color = c.ink,
                )
                Text("Columns", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                Text(
                    "• Name — required\n" +
                        "• Price — required (whole rupees)\n" +
                        "• SKU, Barcode, Category, Cost, Stock — optional",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = c.muted,
                )
                Text(
                    "Existing items are matched by barcode, then SKU, then name and updated; the rest are added. " +
                        "Tip: tap Export first to get a file in the exact format.",
                    fontSize = 12.sp,
                    color = c.muted,
                )
            }
        },
    )
}

@Composable
private fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit,
) {
    val c = PosTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text(if (result.ok) "Import complete" else "Import failed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!result.ok && result.fatalError != null) {
                    Text(result.fatalError, fontSize = 13.sp, color = c.ink)
                } else {
                    Text(
                        "Added ${result.imported} · Updated ${result.updated}" +
                            if (result.skipped > 0) " · Skipped ${result.skipped}" else "",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.ink,
                    )
                    Text("Your product list has been updated.", fontSize = 12.sp, color = c.muted)
                }
                if (result.messages.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text("Notes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    result.messages.forEach { m ->
                        Text("• $m", fontSize = 11.5.sp, color = c.muted)
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Print via Android system print framework (renders an off-screen WebView).
// ---------------------------------------------------------------------------

private fun printProductLabels(
    context: Context,
    products: List<PosProduct>,
    businessName: String,
) {
    val html = labelsHtml(products, businessName)
    val webView = WebView(context)
    webView.webViewClient =
        object : WebViewClient() {
            override fun onPageFinished(
                view: WebView,
                url: String,
            ) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = view.createPrintDocumentAdapter("nexapos-labels")
                printManager.print(
                    "NexaPOS labels",
                    adapter,
                    PrintAttributes.Builder().build(),
                )
            }
        }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

// ---------------------------------------------------------------------------
// Add product
// ---------------------------------------------------------------------------

@Composable
fun AddProductScreen(
    vm: CatalogViewModel,
    onNav: (String) -> Unit,
    onBack: () -> Unit,
    productId: Long? = null,
) {
    val c = PosTheme.colors
    val context = LocalContext.current
    val editing = productId != null

    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var mainCategory by remember { mutableStateOf("") }
    var subCategory by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var rack by remember { mutableStateOf("") }
    var shelf by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var price by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var lowStock by remember { mutableStateOf("5") }
    var vatType by remember { mutableStateOf(VatType.STANDARD) }

    var imageName by remember { mutableStateOf<String?>(null) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var loaded by remember(productId) { mutableStateOf(productId == null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Hardware barcode scanner fills the barcode field, even when it isn't focused.
    LaunchedEffect(Unit) {
        com.nexapos.retail.data.barcode.ScannerEvents.scans.collect { code ->
            if (code.isNotBlank()) barcode = code
        }
    }

    // Load existing product when opened in edit mode.
    LaunchedEffect(productId) {
        if (productId != null) {
            val p = vm.loadProduct(productId)
            if (p != null) {
                name = p.name
                sku = p.sku
                barcode = p.barcode.orEmpty()
                val posModel = vm.products.firstOrNull { it.id == p.id.toString() }
                val (mn, sn) = vm.categoryNamesFor(p.id)
                mainCategory = mn
                subCategory = sn
                brand = posModel?.brand.orEmpty()
                rack = p.rack
                shelf = p.shelf
                model = p.model
                unit = p.unit.ifBlank { "pcs" }
                price = Money.toInput(p.priceCents)
                cost = Money.toInput(p.costCents)
                stock = p.stockQty.toString()
                lowStock = p.lowStockThreshold.toString()
                vatType = VatType.from(p.vatType)
                imageName = p.imagePath
                imageBitmap = withContext(Dispatchers.IO) { ImageStore.load(context, p.imagePath) }
            }
            loaded = true
        }
    }

    val scope = rememberCoroutineScope()
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                // Decode + downscale + JPEG-compress off the main thread (ANR risk).
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { ImageStore.save(context, uri, sku.ifBlank { "product" }) }
                    if (saved != null) {
                        imageName = saved
                        imageBitmap = withContext(Dispatchers.IO) { ImageStore.load(context, saved) }
                    }
                }
            }
        }

    val priceCents = Money.parseToCents(price) ?: 0L
    val costCents = Money.parseToCents(cost) ?: 0L
    val canPublish = name.isNotBlank() && priceCents > 0L && loaded
    // Cashiers never see purchase price or margin; on save the loaded cost
    // state passes through publish() unchanged, so hiding the field is safe.
    val admin = rememberIsAdmin()

    fun publish() {
        if (!canPublish) return
        val stockN = stock.filter { it.isDigit() }.toIntOrNull() ?: 0
        val lowStockN = lowStock.filter { it.isDigit() }.toIntOrNull() ?: 5
        vm.saveProduct(
            id = productId,
            name = name,
            sku = sku,
            barcode = barcode,
            priceCents = priceCents,
            costCents = costCents,
            mainCategoryName = mainCategory,
            subCategoryName = subCategory,
            brandName = brand,
            stock = stockN,
            lowStockThreshold = lowStockN,
            unit = unit,
            model = model,
            rack = rack,
            shelf = shelf,
            vatType = vatType.id,
            kind = "generic",
            imagePath = imageName,
        )
        onBack()
    }

    if (showDeleteDialog && productId != null) {
        DeleteProductDialog(
            vm = vm,
            productId = productId,
            productName = name.ifBlank { "this product" },
            onDismiss = { showDeleteDialog = false },
            onDeleted = {
                showDeleteDialog = false
                onBack()
            },
        )
    }

    NavShell(active = "products", onNav = onNav) {
        AppBar(
            title = if (editing) "Edit Product" else "Add Product",
            subtitle = if (editing) "Update this SKU" else "Create a new SKU",
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostBtn("Cancel", onBack)
                    PrimaryBtn(PosIcons.check, if (editing) "Save changes" else "Publish") { publish() }
                }
            },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FormCard("Identity") {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column(Modifier.width(160.dp)) {
                            Label("Photo")
                            Spacer(Modifier.height(6.dp))
                            Column(
                                Modifier.size(160.dp).clip(RoundedCornerShape(14.dp)).background(c.raised2)
                                    .border(2.dp, c.hairline, RoundedCornerShape(14.dp))
                                    .clickable { imagePicker.launch("image/*") },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                val bmp = imageBitmap
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = "Product photo",
                                        modifier = Modifier.size(156.dp).clip(RoundedCornerShape(13.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    PosIcon(PosIcons.box, tint = c.muted, size = 32.dp)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Tap to add photo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.muted)
                                }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            EditableField(
                                "Product name *",
                                name,
                                { name = it },
                                Modifier.fillMaxWidth(),
                                placeholder = "e.g. Hammer Claw 16oz",
                            )
                            EditableField(
                                "SKU",
                                sku,
                                { sku = it },
                                Modifier.fillMaxWidth(),
                                mono = true,
                                placeholder = "e.g. HMR-16",
                            )
                            // Barcode row: field + Scan + Generate inline so both are always visible.
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                EditableField(
                                    "Barcode (optional)",
                                    barcode,
                                    { barcode = it },
                                    Modifier.weight(1f),
                                    mono = true,
                                    placeholder = "Scan, type or Generate",
                                )
                                Box(Modifier.padding(bottom = 4.dp)) {
                                    SecBtn(PosIcons.scan, "Scan") {
                                        BarcodeScanner.scan(context) { code ->
                                            if (!code.isNullOrBlank()) barcode = code
                                        }
                                    }
                                }
                                Box(Modifier.padding(bottom = 4.dp)) {
                                    PrimaryBtn(PosIcons.refresh, "Generate") {
                                        barcode = Ean13.next()
                                    }
                                }
                            }
                            Text(
                                "Tap Scan to read an existing barcode with the camera, Generate for a fresh in-store EAN-13, " +
                                    "or paste/type one — USB counter scanners also type straight into this field.",
                                fontSize = 11.sp,
                                color = PosTheme.colors.muted,
                            )
                            if (Ean13.isValid(barcode)) {
                                Spacer(Modifier.height(2.dp))
                                Ean13Bars(
                                    value = barcode,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, PosTheme.colors.hairline, RoundedCornerShape(10.dp)),
                                )
                            }
                        }
                    }
                }
                FormCard("Classification") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PickerField(
                            "Main category",
                            mainCategory,
                            options = vm.categoryTree.map { it.name },
                            onValueChange = {
                                mainCategory = it
                                subCategory = ""
                            },
                            Modifier.weight(1f),
                            placeholder = "Tools, Plumbing, Paint…",
                        )
                        PickerField(
                            "Sub-category (optional)",
                            subCategory,
                            options = vm.categoryTree.firstOrNull { it.name.equals(mainCategory, ignoreCase = true) }?.subs?.map { it.name } ?: emptyList(),
                            onValueChange = { subCategory = it },
                            Modifier.weight(1f),
                            placeholder = "Pipes, Fittings…",
                        )
                        PickerField(
                            "Brand",
                            brand,
                            options = vm.brands,
                            onValueChange = { brand = it },
                            Modifier.weight(1f),
                            placeholder = "Tolsen, Bosch, …",
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pick an existing category/brand or type a new one — it's auto-created on save.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                }
                FormCard("Specs & location") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditableField(
                            "Model / part number",
                            model,
                            { model = it },
                            Modifier.weight(1f),
                            mono = true,
                            placeholder = "HMR-16-PRO",
                        )
                        PickerField(
                            "Unit",
                            unit,
                            options = listOf("pcs", "box", "pack", "pair", "set", "kg", "ltr", "m"),
                            onValueChange = { unit = it },
                            Modifier.weight(1f),
                            mono = true,
                            placeholder = "pcs",
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditableField(
                            "Rack",
                            rack,
                            { rack = it },
                            Modifier.weight(1f),
                            mono = true,
                            placeholder = "A-02",
                        )
                        EditableField(
                            "Shelf",
                            shelf,
                            { shelf = it },
                            Modifier.weight(1f),
                            mono = true,
                            placeholder = "Top",
                        )
                    }
                }
                FormCard("Pricing") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditableField(
                            "Sale price *",
                            price,
                            { price = it },
                            Modifier.weight(1f),
                            mono = true,
                            number = true,
                            right = "Rs",
                            placeholder = "0",
                        )
                        if (admin) {
                            EditableField(
                                "Purchase price",
                                cost,
                                { cost = it },
                                Modifier.weight(1f),
                                mono = true,
                                number = true,
                                right = "Rs",
                                placeholder = "0",
                            )
                        }
                    }
                    val hasMargin = priceCents > 0L && costCents > 0L && priceCents > costCents
                    if (admin && hasMargin) {
                        Spacer(Modifier.height(8.dp))
                        val marginCents = priceCents - costCents
                        val marginPct = (marginCents * 100 / priceCents).toInt()
                        Text(
                            "Margin ${Money.format(marginCents)} ($marginPct%)",
                            fontSize = 11.sp,
                            color = c.emerald,
                        )
                    }
                }
                FormCard("Stock") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditableField(
                            "Opening stock",
                            stock,
                            { stock = it },
                            Modifier.weight(1f),
                            mono = true,
                            number = true,
                            placeholder = "0",
                        )
                        EditableField(
                            "Low-stock alert at",
                            lowStock,
                            { lowStock = it },
                            Modifier.weight(1f),
                            mono = true,
                            number = true,
                            placeholder = "5",
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "When stock falls to or below this, the row turns red on the Products page.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                }
                FormCard("VAT") {
                    PickerField(
                        "VAT type",
                        vatType.label,
                        options = VatType.entries.map { it.label },
                        onValueChange = { sel -> vatType = VatType.entries.first { it.label == sel } },
                        Modifier.fillMaxWidth(),
                        allowFreeText = false,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Standard is charged 15% (inclusive). Exempt and Zero-rated are 0% at the till; " +
                            "the type is recorded for your VAT returns.",
                        fontSize = 11.sp,
                        color = c.muted,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    GhostBtnBox("Cancel", onBack)
                    PrimaryBtn(PosIcons.arrowR, if (canPublish) "Publish product" else "Fill name + price") {
                        if (canPublish) publish()
                    }
                }
                if (editing) {
                    FormCard("Danger zone") {
                        Text(
                            "Hide this product from the catalog, or remove it permanently.",
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.crimson)
                                .clickable { showDeleteDialog = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text("Delete product", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteProductDialog(
    vm: CatalogViewModel,
    productId: Long,
    productName: String,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
) {
    val c = PosTheme.colors
    var usage by remember(productId) { mutableStateOf<ProductUsage?>(null) }
    LaunchedEffect(productId) { usage = vm.productUsage(productId) }
    val u = usage
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete “$productName”?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    u == null ->
                        Text("Checking where this product is used…", fontSize = 13.sp, color = c.muted)
                    u.isUsed -> {
                        val parts =
                            buildList {
                                if (u.sales > 0) add("${u.sales} sale${if (u.sales == 1) "" else "s"}")
                                if (u.purchases > 0) add("${u.purchases} purchase${if (u.purchases == 1) "" else "s"}")
                            }.joinToString(" and ")
                        Text(
                            "This product is used in $parts.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.crimson,
                        )
                        Text(
                            "Archive hides it from the catalog and POS but keeps those records, returns and reports " +
                                "intact. Deleting permanently removes it for good and can break restock-on-return and " +
                                "analytics for those records (printed receipts are unaffected).",
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                    }
                    else ->
                        Text(
                            "It isn't used in any sales or purchases yet, so it's safe to remove completely — " +
                                "or just archive (hide) it.",
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = u != null,
                onClick = { vm.deleteProduct(productId, hard = true, onDone = onDeleted) },
            ) { Text("Delete permanently", color = c.crimson) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(
                    enabled = u != null,
                    onClick = { vm.deleteProduct(productId, hard = false, onDone = onDeleted) },
                ) { Text("Archive") }
            }
        },
    )
}

@Composable
private fun FormCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val c = PosTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(20.dp),
    ) {
        Eyebrow(title)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

// ---------------------------------------------------------------------------
// shared bits
// ---------------------------------------------------------------------------

@Composable
private fun Eyebrow(text: String) {
    Text(text, fontSize = 11.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted)
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted)
}

@Composable
private fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier,
) {
    val c = PosTheme.colors
    Row(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PosIcon(PosIcons.search, tint = c.ink, size = 16.dp)
        Box(Modifier.weight(1f)) {
            BasicTextField(value, onChange, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = HankenGrotesk, fontSize = 14.sp, color = c.ink), cursorBrush = SolidColor(c.amber))
            if (value.isEmpty()) Text("Search by name, SKU, barcode…", fontSize = 14.sp, color = c.muted)
        }
    }
}

@Composable
private fun Chip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(
        Modifier.height(34.dp).clip(CircleShape).background(if (active) c.ink else c.raised).border(1.dp, if (active) c.ink else c.hairline, CircleShape).clickable { onClick() }.padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (active) c.surface else c.ink)
    }
}

@Composable
private fun SecBtn(
    icon: List<String>?,
    label: String,
    onClick: () -> Unit = {},
) {
    val c = PosTheme.colors
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) PosIcon(icon, tint = c.ink, size = 14.dp)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}

@Composable
private fun PrimaryBtn(
    icon: List<String>,
    label: String,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(c.amber).clickable { onClick() }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PosIcon(icon, tint = Color.White, size = 14.dp)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun GhostBtn(
    label: String,
    onClick: () -> Unit,
) {
    Box(Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PosTheme.colors.muted)
    }
}

@Composable
private fun GhostBtnBox(
    label: String,
    onClick: () -> Unit,
) {
    val c = PosTheme.colors
    Box(Modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).background(c.raised).border(1.dp, c.hairline, RoundedCornerShape(10.dp)).clickable { onClick() }.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}
