package com.nexapos.retail.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexapos.retail.PosApplication
import com.nexapos.retail.ui.dashboard.DashboardViewModel
import com.nexapos.retail.ui.labels.LabelPrintViewModel
import com.nexapos.retail.ui.money.MoneyViewModel
import com.nexapos.retail.ui.parties.PartiesViewModel
import com.nexapos.retail.ui.products.CatalogViewModel
import com.nexapos.retail.ui.purchase.PurchaseDetailViewModel
import com.nexapos.retail.ui.purchase.PurchasesViewModel
import com.nexapos.retail.ui.purchase.receipt.ReceiptScanViewModel
import com.nexapos.retail.ui.reports.ReportsViewModel
import com.nexapos.retail.ui.sale.SaleReturnViewModel
import com.nexapos.retail.ui.sale.SalesListViewModel
import com.nexapos.retail.ui.sale.SellingViewModel
import com.nexapos.retail.ui.settings.StaffViewModel
import com.nexapos.retail.ui.shift.ShiftViewModel

/** Wires ViewModels to the app's [com.nexapos.retail.di.AppContainer]. */
object AppViewModelProvider {
    val Factory =
        viewModelFactory {
            initializer {
                val c = posApplication().container
                SellingViewModel(
                    catalogRepository = c.catalogRepository,
                    salesRepository = c.salesRepository,
                    partiesRepository = c.partiesRepository,
                    drawerKicker = c.drawerKicker,
                    shiftRepository = c.shiftRepository,
                    session = c.session,
                )
            }
            initializer {
                CatalogViewModel(catalogRepository = posApplication().container.catalogRepository)
            }
            initializer {
                val c = posApplication().container
                LabelPrintViewModel(
                    catalogRepository = c.catalogRepository,
                    labelPrinter = c.labelPrinter,
                )
            }
            initializer {
                val c = posApplication().container
                PartiesViewModel(
                    partiesRepository = c.partiesRepository,
                    salesRepository = c.salesRepository,
                )
            }
            initializer {
                val c = posApplication().container
                MoneyViewModel(
                    moneyRepository = c.moneyRepository,
                    salesRepository = c.salesRepository,
                    shiftRepository = c.shiftRepository,
                    session = c.session,
                )
            }
            initializer {
                val c = posApplication().container
                PurchasesViewModel(
                    purchasesRepository = c.purchasesRepository,
                    catalogRepository = c.catalogRepository,
                    partiesRepository = c.partiesRepository,
                )
            }
            initializer {
                val c = posApplication().container
                DashboardViewModel(
                    salesRepository = c.salesRepository,
                    catalogRepository = c.catalogRepository,
                    partiesRepository = c.partiesRepository,
                )
            }
            initializer {
                val c = posApplication().container
                ReportsViewModel(
                    salesRepository = c.salesRepository,
                    moneyRepository = c.moneyRepository,
                    purchasesRepository = c.purchasesRepository,
                    catalogRepository = c.catalogRepository,
                    partiesRepository = c.partiesRepository,
                    returnsRepository = c.returnsRepository,
                )
            }
            initializer {
                SalesListViewModel(salesRepository = posApplication().container.salesRepository)
            }
            initializer {
                val c = posApplication().container
                SaleReturnViewModel(
                    salesRepository = c.salesRepository,
                    returnsRepository = c.returnsRepository,
                    drawerKicker = c.drawerKicker,
                    shiftRepository = c.shiftRepository,
                    session = c.session,
                )
            }
            initializer {
                PurchaseDetailViewModel(purchasesRepository = posApplication().container.purchasesRepository)
            }
            initializer {
                val c = posApplication().container
                StaffViewModel(staffRepository = c.staffRepository, session = c.session)
            }
            initializer {
                val c = posApplication().container
                ShiftViewModel(
                    shiftRepository = c.shiftRepository,
                    session = c.session,
                )
            }
            initializer {
                val c = posApplication().container
                ReceiptScanViewModel(
                    purchasesRepository = c.purchasesRepository,
                    catalogRepository = c.catalogRepository,
                    partiesRepository = c.partiesRepository,
                )
            }
        }
}

private fun CreationExtras.posApplication(): PosApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PosApplication
