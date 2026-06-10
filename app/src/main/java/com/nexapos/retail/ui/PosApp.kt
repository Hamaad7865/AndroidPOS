package com.nexapos.retail.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexapos.retail.PosApplication
import com.nexapos.retail.data.profile.BusinessProfile
import com.nexapos.retail.data.security.PinManager
import com.nexapos.retail.data.security.StaffAuthenticator
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
import com.nexapos.retail.ui.session.currentStaff
import com.nexapos.retail.ui.settings.PrintingSettingsScreen
import com.nexapos.retail.ui.settings.SettingsScreen
import com.nexapos.retail.ui.shift.ShiftHistoryScreen
import com.nexapos.retail.ui.shift.ShiftScreen
import com.nexapos.retail.ui.shift.ShiftViewModel
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

/** Screens reachable WITHOUT a signed-in staff member. Everything else is guarded. */
private val PUBLIC_ROUTES = setOf("splash", "login", "setup")

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
    // App-level like `selling`, so the "open a shift?" prompt state survives navigation.
    val shiftVm: ShiftViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val appContext = LocalContext.current

    // Stable lambda: navigating to a top-level module resets the back stack down
    // to Home.  Navigating to any other destination keeps the module on the stack.
    // Wrapped in remember so its identity is stable across recompositions and does
    // not cause the entire nav graph to rebuild.
    val go: (String) -> Unit =
        remember(navController, appContext, selling, shiftVm) {
            { id ->
                when {
                    // "lock" is the sign-out action (nav-rail session badge), not a
                    // screen: drop the session, wipe the in-memory ticket so the next
                    // staff member starts clean, and land on login with a clean stack
                    // so Back can't walk into another staff member's session.
                    id == "lock" -> {
                        selling.resetForSignOut()
                        shiftVm.onSignOut()
                        (appContext.applicationContext as PosApplication).container.session.logout()
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                    id in MODULE_ROUTES -> {
                        // No saveState/restoreState: a module always re-opens fresh, so one
                        // staff member's saved screen/form state can never be restored into
                        // the next staff member's session after a sign-out.
                        navController.navigate(id) {
                            launchSingleTop = true
                            popUpTo("home")
                        }
                    }
                    else -> {
                        navController.navigate(id) { launchSingleTop = true }
                    }
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

    // Defence in depth: if the OS restores the app onto a protected screen after
    // process death — when the in-memory session is gone — force a fresh sign-in
    // so data can't render for nobody. Public routes (splash/login/setup) are exempt.
    val signedIn by (appContext.applicationContext as PosApplication).container.session.current.collectAsState()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(signedIn, currentRoute) {
        val base = currentRoute?.substringBefore('/')?.substringBefore('?')
        if (signedIn == null && base != null && base !in PUBLIC_ROUTES) {
            navController.navigate("login") {
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
            val container = (context.applicationContext as PosApplication).container
            // The login screen IS the lock screen: however we got here, no one
            // is signed in any more.
            LaunchedEffect(Unit) { container.session.logout() }
            LoginScreen(
                verifyPin = { pin ->
                    // PBKDF2 is CPU-heavy — keep it off the main thread so the UI
                    // never freezes on a slow tablet.
                    withContext(Dispatchers.Default) {
                        val staff = StaffAuthenticator.authenticate(context, container.staffRepository, pin)
                        if (staff != null) container.session.login(staff)
                        staff != null
                    }
                },
                lockoutRemainingMs = { PinManager.lockoutRemainingMs(context) },
                onAuthenticated = finishAuth,
                onCreate = { navController.navigate("setup") },
            )
        }
        composable("home") {
            DashboardScreen(vm = viewModel(factory = AppViewModelProvider.Factory), onNav = go)
            // Nudge once per sign-in: a till shift makes the day's cash accountable.
            val openShift by shiftVm.openShift.collectAsState()
            val signedIn = currentStaff()
            if (signedIn != null && openShift == null && !shiftVm.promptDismissed) {
                AlertDialog(
                    onDismissRequest = { shiftVm.promptDismissed = true },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                shiftVm.promptDismissed = true
                                go("shift")
                            },
                        ) { Text("Open shift") }
                    },
                    dismissButton = {
                        TextButton(onClick = { shiftVm.promptDismissed = true }) { Text("Not now") }
                    },
                    title = { Text("Open a till shift?") },
                    text = {
                        Text(
                            "Count your float and open a shift — sales, returns and cash movements " +
                                "get tallied so you can balance the drawer at close.",
                        )
                    },
                )
            }
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
        composable("drawer-settings") {
            com.nexapos.retail.ui.settings.DrawerSettingsScreen(onNav = go, onBack = { navController.popBackStack() })
        }
        composable("shift") {
            ShiftScreen(vm = shiftVm, onNav = go, onBack = { navController.popBackStack() })
        }
        composable("shift-history") {
            ShiftHistoryScreen(vm = shiftVm, onNav = go, onBack = { navController.popBackStack() })
        }
        composable("staff-settings") {
            com.nexapos.retail.ui.settings.StaffScreen(
                vm = viewModel(factory = AppViewModelProvider.Factory),
                onNav = go,
                onBack = { navController.popBackStack() },
            )
        }
        composable("setup") {
            BusinessSetupScreen(onDone = finishSetup, onBack = { navController.popBackStack() })
        }
    }
}
