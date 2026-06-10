# Multi-Branch (Hybrid Sync) — Design Spec

**Date:** 2026-06-11 · **Status:** draft, awaiting owner approval.
**Branch:** `feature/multi-branch` (off `main`).
**Pricing:** paid add-on — unlocked per business with an offline licence key.

## Purpose

Let one business run several NexaPOS installs — branches **A, B, C, D… plus a Head Office (HQ)** — where a branch can **check** another branch and HQ can check all of them. "Check" in v1 means **read-only visibility**: dashboard, reports, stock levels, and sales history of the other branch. Nobody edits another branch's books.

The app stays **offline-first**: every existing flow (sales, shifts, money, returns) keeps working with no internet, exactly as today. The add-on makes the app **hybrid** — when internet is available, each branch pushes compact read-models of its data to a **Firebase (Firestore)** backend and pulls the branches it is allowed to see.

## Decisions (agreed with owner)

| Decision | Choice |
|---|---|
| Sync transport | **Firebase** (Firestore + Firebase Auth), managed backend |
| Visibility scope | Read-only: reports, stock, sales (no remote editing, no stock transfers in v1) |
| Topology | **HQ controls who sees whom** (visibility matrix); HQ sees all |
| Unlock | **Offline licence key** (signed, per business), entered once in Settings |

## Architecture principles (bug-avoidance first)

1. **Zero changes to the operational database.** No new Room tables, no migration. Branch identity lives in `EncryptedSharedPreferences`. Remote-branch data is **never written into the local DB** — it is served straight from Firestore, whose built-in offline cache gives persistence and stale-reads for free.
2. **Each branch is the single writer of its own data.** Peers and HQ only read. No two-way merge, no conflict resolution, no invoice-number collisions (remote invoices are displayed with a branch prefix, e.g. `B · S-00042`, at render time only).
3. **Sync never touches the sale transaction.** The after-sale summary push is fire-and-forget *after* `checkout()` persists — the same pattern as the drawer kick. A sync failure can never break or delay a sale.
4. **No hand-rolled sync queue.** Firestore's offline persistence queues writes and retries automatically; we lean on it instead of building (and debugging) our own.

## Firestore data model

One Firebase **Auth account per business** (email + password, created by the owner at setup). Every device of that business signs into the same account; security rules isolate each business by `uid`.

```
businesses/{uid}
  profile                      → business name, createdAt
  config/registry              → branches: {code → name, role}, maxBranches (from licence)
  config/visibility            → matrix: {viewerCode → [viewableCodes...]}  (HQ-managed)
  branches/{code}/state/summary    → today's totals, ticket count, stock value, low-stock count,
                                     open-shift info, lastSyncAt (server timestamp)
  branches/{code}/state/stock-{n}  → product list chunks (~1 500 SKUs/doc: name, sku, barcode,
                                     priceCents, stockQty, lowStockThreshold, category)
  branches/{code}/days/{yyyy-MM-dd} → that day's sales (with line items), returns, money txns,
                                      closed shifts — immutable once the day is over
```

- **Write volume:** 1 small summary write per sale + a handful of stock/day docs per day → comfortably inside Firestore's free tier for a small business. The owner sets a **billing alarm** on the Firebase project anyway.
- **History backfill:** when the add-on is first enabled, the branch uploads the last **30 days** + current stock. Remote views show data "synced since {date}".
- **Timestamps:** `lastSyncAt` uses server timestamps — immune to device clock skew. Every remote screen shows a staleness label ("Last synced 2 h ago").

### Security rules (`firestore.rules`, committed + deploy doc)

`request.auth.uid == businessId` for all reads/writes under `businesses/{businessId}` — one business can never read another. The **visibility matrix is policy, not cryptography**: all of a business's devices share one account, so branch-vs-branch visibility is enforced client-side (and the Branches UI is admin-gated). Threat model: honest-but-curious staff — acceptable for v1 and documented.

## Sync engine

| Trigger | What is pushed |
|---|---|
| After each sale persists | `state/summary` (fire-and-forget) |
| Shift close | summary + stock chunks + today's day-doc |
| Manual **Sync now** (Settings/Branches) | full push + pull |
| `WorkManager` periodic (~6 h, network + battery-not-low) | full push |
| App open with stale data | pull of viewable branches |

Pulls are Firestore snapshot listeners on the branches the device may view — near-live when online, cached when not.

## Licence (paid unlock)

- **Format:** `NXB-` + base32 of `payload {businessKey, maxBranches, expiry?}` + **ECDSA P-256 signature**. The app embeds only the **public key**; the private key and generator script stay offline with the developer (never committed).
- **Flow:** Settings → Multi-branch → enter licence → verified locally (no internet) → stored in `EncryptedSharedPreferences`.
- **Enforcement:** registering more branch codes than `maxBranches` is refused at HQ. Client-side enforcement only — a determined pirate could patch the APK; accepted for this market.

## Components

### New
- `data/branch/` — `BranchIdentity` (prefs: role HQ/branch, code, name), `LicenseManager` (ECDSA verify, pure + unit-tested), `SyncModels` (pure Room→DTO mappers, unit-tested), `BranchSync` (uploader + WorkManager worker), `FirestoreRemoteBranchRepository`.
- `domain/repository/RemoteBranchRepository` — read-only interface (summaries, stock, days) mirroring existing repo style.
- `ui/branches/` — `BranchesScreen` (viewable-branch list + last-sync + today summary), `RemoteBranchScreen` (tabs: Overview / Stock / Sales, reusing existing card & report components), HQ-only: branch registry + visibility matrix editor, "All branches" consolidated overview.
- `ui/settings/MultiBranchSettingsScreen` — licence entry, business sign-in/create, role & branch identity, sync status, Sync now.

### Modified
- `AppContainer` — lazily constructs the sync stack **only when licensed & configured**; unlicensed installs never initialise Firebase.
- `PosApp` / nav rail — "Branches" item, visible only when licensed + configured + **admin role** (cashiers never browse other branches' takings).
- `SellingViewModel` — one fire-and-forget `branchSync.onSaleRecorded()` after the existing post-checkout block (next to the drawer kick).
- `ShiftViewModel` — same hook on successful close.
- `app/build.gradle.kts` — Firebase BoM, `firebase-auth-ktx`, `firebase-firestore-ktx`, `google-services` plugin (+ `google-services.json`, safe to commit).

**No change** to: Room schema/DAOs/migrations, checkout transaction, PIN auth, drawer, receipts, backup.

## Failure modes

| Situation | Behaviour |
|---|---|
| No internet | App fully functional; writes queue in Firestore cache; remote views show cached data + staleness label |
| Firebase outage | Same as offline — operational flows unaffected |
| Wrong/old business password | Sync pauses with a "re-sign-in" banner in Settings; selling unaffected |
| Reinstall / new device | Sign in + licence re-entry; cloud data intact |
| Day-doc partially written at crash | Day docs are rewritten whole on next sync (idempotent set, not append) |

## Out of scope (v1)

Stock transfers between branches · remote editing · catalog merging/sharing · web HQ dashboard · push notifications · per-staff cloud accounts · live per-second sync.

## Open setup prerequisite (owner action)

Create the Firebase project (console: project → Android app `com.nexapos.retail` → download `google-services.json` → enable Email/Password auth + Firestore + set billing alarm). Exact click-by-click steps will be in `docs/FIREBASE_SETUP.md` (Stage 3).
