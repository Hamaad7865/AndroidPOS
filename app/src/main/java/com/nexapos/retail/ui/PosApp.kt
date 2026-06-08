package com.nexapos.retail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.data.security.PinManager
import com.nexapos.retail.ui.auth.BusinessSetupScreen
import com.nexapos.retail.ui.auth.LoginScreen
import com.nexapos.retail.ui.auth.SplashScreen
import com.nexapos.retail.ui.checkout.PosCheckoutScreen
import com.nexapos.retail.ui.checkout.PosReceiptScreen
import com.nexapos.retail.ui.dashboard.DashboardScreen
import com.nexapos.retail.ui.money.AddTxnScreen
import com.nexapos.retail.ui.money.ExpenseListScreen
import com.nexapos.retail.ui.money.IncomeListScreen
import com.nexapos.retail.ui.money.LedgerScreen
import com.nexapos.retail.ui.money.MoneyHubScreen
import com.nexapos.retail.ui.parties.PartiesScreen
import com.nexapos.retail.ui.products.AddProductScreen
import com.nexapos.retail.ui.products.ProductsListScreen
import com.nexapos.retail.ui.purchase.AddPurchaseScreen
import com.nexapos.retail.ui.purchase.PurchaseDetailScreen
import com.nexapos.retail.ui.purchase.PurchaseListScreen
import com.nexapos.retail.ui.reports.ReportDetailScreen
import com.nexapos.retail.ui.reports.ReportsScreen
import com.nexapos.retail.ui.sale.PosSaleScreen
import com.nexapos.retail.ui.sale.SaleReturnScreen
import com.nexapos.retail.ui.sale.SalesListScreen
import com.nexapos.retail.ui.sale.SellingViewModel
import com.nexapos.retail.ui.settings.PrintingSettingsScreen
import com.nexapos.retail.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Top-level destinations reachable from the nav rail (plus Money's in-screen
 * tabs, which behave like modules). Anything not in this set is treated as a
 * sub-screen: it's pushed on top of the current module so Back/Cancel returns
 * to that module rather than collapsing to Home.
 */
private val MODULE_ROUTES =
    setOf("home", "pos", "products", "parties", "purchase", "money", "income", "expense", "ledger", "reports", "settings")

/**
 * Root navigation. A single [SellingViewModel] is shared so the cart carries
 * POS → Checkout → Receipt. The nav rail routes to every top-level destination;
 * not-yet-built ones show a placeholder.
 *
 * Boot flow:
 *  1. splash
 *  2. If no PIN is stored → "setup" (mandatory — user cannot skip to login)
 *  3. login
 *  4. home (or back to "setup" if business profile not yet filled in)
 */
@Composable
fun PosApp() {
    val navController = rememberNavController()
    val selling: SellingViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val appContext = LocalContext.current

    // Stable lambda: navigating to a top-level module resets the back stack down
    // to Home.  Navigating to any other destination keeps the module on the stack.
    // Wrapped in remember so its identity is stable across recompositions and does
    // not cause the entire nav graph to rebuild.
    val go: (String) -> Unit =
        remember(navController) {
            { id ->
                if (id in MODULE_ROUTES) {
                    navController.navigate(id) {
                        launchSingleTop = true
                        popUpTo("home") { saveState = true }
                        restoreState = true
                    }
                } else {
                    navController.navigate(id) { launchSingleTop = true }
                }
            }
        }

    // After PIN auth: if business isn't configured yet → setup wizard; otherwise → home.
    // Stable: depends only on navController (captured once).
    val finishAuth: () -> Unit =
        remember(navController, appContext) {
            {
                val destination = if (BusinessProfile.isConfigured(appContext)) "home" else "setup"
                navController.navigate(destination) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }

    // After setup wizard finishes → home with a clean back stack.
    val finishSetup: () -> Unit =
        remember(navController) {
            {
                navController.navigate("home") {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onDone = {
                    // If no PIN has been set yet, route to mandatory setup before login.
                    val hasPinAlready = PinManager.hasPin(appContext)
                    val next = if (hasPinAlready) "login" else "setup"
                    navController.navigate(next) { popUpTo("splash") { inclusive = true } }
                },
            )
        }
        composable("login") {
            val context = LocalContext.current
            LoginScreen(
                verifyPin = { pin ->
                    // PBKDF2 is CPU-heavy — keep it off the main thread so the UI
                    // never freezes on a slow tablet.
                    withContext(Dispatchers.Default) {
                        PinManager.verify(context, pin)
                    }
                },
                lockoutRemainingMs = { PinManager.lockoutRemainingMs(context) },
                onAuthenticated = finishAuth,
                onCreate = { navController.navigate("setup") },
            )
        }
        composable("home") {
            DashboardScreen(vm = viewModel(factory = AppViewModelProvider.Factory), onNav = go)
        }

        composable("pos") {
            PosSaleScreen(
                vm = selling,
                onCharge = {
                    selling.beginCheckout()
                    navController.navigate("checkout")
                },
                onNav = go,
            )
        }
        composable("checkout") {
            PosCheckoutScreen(
                vm = selling,
                onBack = { navController.popBackStack() },
                onComplete = { navController.navigate("receipt") },
                onNav = go,
            )
        }
        composable("receipt") {
            PosReceiptScreen(
                vm = selling,
                onNewSale = { navController.popBackStack("pos", inclusive = false) },
                onBack = { navController.popBackStack("pos", inclusive = false) },
                onNav = go,
            )
        }
        composable("sales-list") {
            SalesListScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onBack = { navController.popBackStack() },
                onReturn = { saleId -> navController.navigate("sale-return/$saleId") },
            )
        }
        composable(
            "sale-return/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")
            SaleReturnScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                saleId = id,
                onNav = go,
                onBack = { navController.popBackStack() },
            )
        }

        composable("products") {
            ProductsListScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onAddProduct = { navController.navigate("add-product") },
                onOpenProduct = { id -> navController.navigate("edit-product/$id") },
            )
        }
        composable("add-product") {
            AddProductScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "edit-product/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")
            AddProductScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onBack = { navController.popBackStack() },
                productId = id,
            )
        }
        composable("parties") {
            PartiesScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onNewSale = { navController.navigate("pos") },
                onNewPurchase = { supplierId -> navController.navigate("add-purchase?supplierId=$supplierId") },
            )
        }
        composable("purchase") {
            PurchaseListScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onNewPurchase = { navController.navigate("add-purchase") },
                onScanReceipt = { go("receipt-scan") },
                onOpen = { id -> navController.navigate("purchase-detail/$id") },
            )
        }
        composable("receipt-scan") {
            com.nexapos.retail.ui.purchase.receipt.ReceiptScanScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "add-purchase?supplierId={supplierId}",
            arguments =
                listOf(
                    androidx.navigation.navArgument("supplierId") {
                        type = androidx.navigation.NavType.LongType
                        defaultValue = -1L
                    },
                ),
        ) { backStackEntry ->
            val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: -1L
            AddPurchaseScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onBack = { navController.popBackStack() },
                initialSupplierId = supplierId.takeIf { it > 0 },
            )
        }
        composable(
            "purchase-detail/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")
            PurchaseDetailScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                purchaseId = id,
                onNav = go,
                onBack = { navController.popBackStack() },
            )
        }
        composable("money") {
            MoneyHubScreen(vm = viewModel(factory = AppViewModelProvider.Factory), onNav = go)
        }
        composable("income") {
            IncomeListScreen(vm = viewModel(factory = AppViewModelProvider.Factory), onNav = go)
        }
        composable("expense") {
            ExpenseListScreen(vm = viewModel(factory = AppViewModelProvider.Factory), onNav = go)
        }
        composable("ledger") {
            LedgerScreen(vm = viewModel(factory = AppViewModelProvider.Factory), onNav = go)
        }
        composable("add-expense") {
            AddTxnScreen(
                income = false,
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onBack = { navController.popBackStack() },
            )
        }
        composable("add-income") {
            AddTxnScreen(
                income = true,
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onBack = { navController.popBackStack() },
            )
        }
        composable("reports") {
            ReportsScreen(
                onNav = go,
                onOpenReport = { id -> navController.navigate("reports/$id") },
            )
        }
        composable(
            "reports/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            ReportDetailScreen(
                reportId = id,
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onBack = { navController.popBackStack() },
            )
        }
        composable("settings") { SettingsScreen(onNav = go) }
        composable("printing-settings") {
            PrintingSettingsScreen(onNav = go, onBack = { navController.popBackStack() })
        }
        composable("scanner-settings") {
            com.nexapos.retail.ui.settings.ScannerSettingsScreen(onNav = go, onBack = { navController.popBackStack() })
        }
        composable("setup") {
            BusinessSetupScreen(onDone = finishSetup, onBack = { navController.popBackStack() })
        }
    }
}
