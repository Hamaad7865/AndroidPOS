# NexaPOS Retail — Complete Screen Map

The single source of truth for **every** screen the app must have. Used both for the
design prompt and for implementation tracking, so nothing gets forgotten.

Legend: 📷 = seen in the reference screenshots · ➕ = implied / obviously required but
not shown in the screenshots.

## 0. Onboarding & Auth  (➕ — none were in the screenshots, but all are required)
- Splash ➕
- Login (email / phone + password) ➕
- Sign Up / Create Business ➕
- Forgot password / OTP verify ➕
- First-run **Business Setup wizard** (name, logo, currency, tax, opening balance) ➕
- **Subscription / Plans + paywall** 📷 (referenced: "Free Plan", "Select your favourite plan")

## 1. Selling
- **POS Sale** — quick tap-grid 📷
- **Sale / Add Sales** — full invoice form 📷
- **Checkout / Payment** — discount (flat/%), VAT, shipping, rounding, received vs due, payment type, split tender 📷
- Numeric keypad / tender sheet ➕
- **Barcode scanner (camera)** ➕ (scan icon seen 📷)
- **Receipt / Sale complete** — preview, print, share ➕
- **Sales List** — invoice history + status/date filters 📷 (menu)
- Sale / Invoice detail ➕
- **Sales Return** 📷 (referenced in Reports)

## 2. Purchasing
- **Purchase** — stock-in from a supplier 📷 (menu)
- **Purchase List** 📷 (menu)
- Purchase detail ➕
- **Purchase Return** 📷 (referenced in Reports)

## 3. Inventory
- **Products** list 📷 (menu "Product")
- Product detail ➕
- **Add / Edit Product** — rich form 📷
- Categories / Brands / Racks / Shelves / Models / Units management ➕ (fields seen 📷)
- **Stock List** / stock adjustment 📷 (menu)
- Low-stock alerts ➕
- **Barcode / label generator + print** 📷 (settings)

## 4. Parties (customers & suppliers)
- **Parties** list (Customers / Suppliers tabs) 📷 (menu)
- Add / Edit Party ➕ ("+" on the customer selector 📷)
- Party detail / statement ➕
- **Due List** — outstanding balances 📷 (menu)

## 5. Money & accounting
- **Income** (list + add) 📷
- **Expense** (list + add) 📷
- Income Categories 📷 (report)
- **Cash & Bank** — accounts, transfers 📷 (settings)
- **Ledger** 📷 (menu)
- **Vat & Tax** — manage rates 📷 (menu)
- **Profit & Loss** 📷

## 6. Dashboard & Reports
- **Home dashboard** — Quick Overview + quick-action tiles 📷
- **Analytics Dashboard** — chart + date filter + stat cards (Stock Value, Items, Categories, Due) 📷
- **Reports hub** 📷
- Report details: Sales, Sales Return, Purchase, Purchase Return, Due, Day Book, All Transaction,
  Bill-wise Profit, Profit & Loss, Cashflow, Balance Sheet, Tax, Product Sale History,
  Product Purchase History 📷

## 7. Settings & admin
- **Settings** hub 📷
- **Business Profile** (view/edit) 📷
- **Printing / Invoice settings** + **Custom Print** template 📷
- **Sales Settings** 📷
- **User Role / Staff & permissions** 📷
- **Currency** 📷 · **Language** 📷
- **Subscription** management 📷
- Delete account / Log out 📷

## 8. Cross-cutting states (apply to every screen)
Empty states · loading / skeletons · error / offline · success · confirmation dialogs ·
global search · (optional) notifications.

## Suggested implementation order
- **Phase 1 — money-making core:** Auth + Business Setup → POS Sale → Checkout → Receipt → Products + Add/Edit Product.
- **Phase 2 — daily ops:** Sales List + Invoice detail → Parties + Due/Ledger → Home & Analytics Dashboard.
- **Phase 3 — buying & cash:** Purchase + Purchase List → Stock List → Income/Expense → Cash & Bank.
- **Phase 4 — accounting & compliance:** Reports suite → Sales/Purchase Returns → Vat & Tax → Barcode labels → Custom Print.
- **Phase 5 — admin & polish:** User Roles → Subscription → Settings → final visual polish.
