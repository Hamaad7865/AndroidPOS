# Multi-Branch (Hybrid Sync) — Implementation Plan

**Spec:** `docs/superpowers/specs/2026-06-11-multi-branch-design.md`
**Branch:** `feature/multi-branch` off `main` — merged only when all stages pass.
**Per-stage gate (no exceptions):** `assembleDebug` + `testDebugUnitTest` + `detekt` + `ktlintCheck` green → on-device check → one commit. A Sonnet-agent bug hunt runs once at the end (Stage 5), not per stage, to conserve tokens.

---

## Stage 1 — Licence gate (no Firebase)

- `data/branch/LicenseManager.kt`: parse `NXB-…` code → payload `{businessKey, maxBranches, expiry?}` + ECDSA P-256 verify against an embedded public key (pure Kotlin, `java.security` only).
- Persist accepted licence in `EncryptedSharedPreferences`; expose `MultiBranch.licensed(context)`.
- Settings → "Multi-branch (add-on)" card: locked state + licence entry field + clear error on bad code.
- Nav: "Branches" item plumbed but hidden unless licensed (and admin).
- **Tests:** `LicenseManagerTest` with a test keypair — valid, tampered payload, tampered signature, wrong key, expiry, maxBranches parse.
- Keygen: developer-side script kept **outside the repo**; repo gets `docs/LICENSING.md` (format + how codes are issued) and the public key constant.

## Stage 2 — Branch identity + sync read-models (no Firebase)

- `data/branch/BranchIdentity.kt`: role (HQ | BRANCH), branch code (A–Z0–9, ≤4 chars), display name — `EncryptedSharedPreferences`; setup UI inside the Multi-branch settings card.
- `data/branch/SyncModels.kt`: pure mappers from existing repository read paths → `SummaryDoc`, `StockChunkDoc` (chunk at 1 500), `DayDoc` (sales+items, returns, money txns, closed shifts). Cents stay `Long` end-to-end.
- **Tests:** mapper round-trips (DTO → map → DTO), chunking boundaries, day-bucketing by local date, empty-day and VAT/discount fields preserved exactly.

## Stage 3 — Firebase wiring + upload path

- Gradle: Firebase BoM + `auth-ktx` + `firestore-ktx` + `google-services` plugin; commit `google-services.json`; write `docs/FIREBASE_SETUP.md` (console steps for the owner) and `firestore.rules` (uid-scoped) + deploy instructions.
- Business account UI in Multi-branch settings: create / sign in / re-auth banner.
- `BranchSync`: summary push after sale (fire-and-forget hook in `SellingViewModel` after checkout, beside the drawer kick) and after shift close; full push (summary+stock+day) on shift close, manual **Sync now**, and a `WorkManager` periodic job (~6 h); 30-day backfill on first enable.
- Sync status row (last push, queued/offline indicator).
- **Tests:** unit-test trigger logic around fakes (`FakeBranchSync` asserting "kick-like" semantics: fires after persist, never throws into the sale flow). Firestore itself is exercised on-device.
- **On-device:** emulator sale → doc visible in Firestore console; airplane-mode sale → syncs when back online.

## Stage 4 — Read-only remote views

- `RemoteBranchRepository` (domain interface) + `FirestoreRemoteBranchRepository` (snapshot listeners, Firestore offline cache).
- `BranchesScreen`: viewable branches with last-sync + today summary; admin-gated nav item now visible.
- `RemoteBranchScreen` tabs reusing existing components: **Overview** (dashboard cards), **Stock** (search + low-stock filter), **Sales** (day picker → invoice list with `CODE · S-00001` display prefix). Staleness label on every screen.
- **On-device:** two emulators (`pos_tablet` = HQ, `pos_emulator` = branch A) — sell on A, watch HQ update.

## Stage 5 — HQ controls + consolidated view + hardening

- HQ-only: branch registry (enforces `maxBranches`) + visibility matrix editor → `config/visibility`; viewers enforce the matrix client-side.
- HQ "All branches" overview: consolidated today/week cards summing branch summaries (cents-exact).
- Edge polish: re-auth flow, empty states, sign-out clears remote listeners, role checks rechecked on session switch.
- **Bug hunt:** 2 focused Sonnet agents (sync correctness + money exactness across DTOs) over the new module only; fix confirmed findings.
- Docs: `ARCHITECTURE.md`, `SCREENS.md`, phase report; bump `versionName` to 1.1.0.
- Full two-device end-to-end: licence → identity → sign-in → sell on A → check from B (allowed) → blocked from C (matrix) → HQ sees all.

---

## Owner actions needed (before Stage 3)
1. Create the Firebase project + Android app `com.nexapos.retail`; download `google-services.json` into `app/`.
2. Enable **Email/Password** auth + **Firestore**; set a billing alarm.
3. Decide the add-on price / `maxBranches` tiers so licence codes can be issued.

## Token discipline
Spec/plan authored directly (this doc). Implementation edits are surgical per stage; the only agent fan-out is the single Stage-5 bug hunt on Sonnet. No worktrees, no parallel agents during build stages.
