# NexaPOS Retail — Progress Tracker

Living checkpoint so we can stop and resume without losing the thread.
**Updated each working step.** Last updated: 2026-05-26.

## 🎯 Goal
A sellable Android retail POS (one-time **Rs 5,000** on Play Store) that matches the
**Claude Design handoff EXACTLY** — layout, components, icons, logo, fonts, animations.
Source of truth: `reference/handoff/nexapos-retail/project/*.jsx` + `tokens.css`.
Full screen list: [SCREENS.md](SCREENS.md) · Architecture: [ARCHITECTURE.md](ARCHITECTURE.md).

## ▶️ RESUME HERE (where we left off)
**Currently:** faithful design rebuild from the handoff, in verified passes.
**Next action:** Build the **nav rail + app bar + geometric product tiles + `card-machined` cards**, swap the seed catalog to the **12 hardware products**, then **rebuild the POS screen** to match `selling.jsx` exactly. Then Checkout, then Receipt.

To resume building/running, see [README.md](../README.md) (set `$env:JAVA_HOME` first).
Verify via `adb shell uiautomator dump` text (image API was blocked late in the session) or the live tablet emulator `pos_tablet`.

## ✅ Done
- Toolchain on D: (JDK 17, SDK 35, Gradle 8.9, emulators `pos_emulator` phone + `pos_tablet`).
- Project scaffold, offline Room data layer, MVVM + repository interfaces, sealed CheckoutState.
- detekt + ktlint + unit tests (runTest/Turbine) — all green. Release/AAB signing pipeline.
- "Workshop Precision" theme aligned to handoff token hexes (Color.kt) + Hanken/JetBrains/Fraunces fonts.
- **App launcher icon** → handoff logo (ink square + amber "N" + bone lines).
- **Custom icon set** `ui/icons/PosIcons.kt` (36 stroke icons, exact handoff paths).
- POS + Checkout screens exist & work (functionally) — but are MY interpretation, **not yet matching the handoff** layout/components.

## 🔨 Design rebuild checklist (match handoff exactly)
- [⏳] Foundation: launcher icon ✅, PosIcons ✅, Fraunces ✅ — *(build verifying)*
- [ ] `NavRail` (88px, 8 items, ink-active + amber bar, logo top, "SK" avatar bottom)
- [ ] `AppBar` (eyebrow "QUINCAILLERIE RB TRADING · Counter 01" + title + subtitle + actions)
- [ ] `ProductTile` geometric illustrations (sprayer/drill/wrench/saw/scrubber/paint/hammer/pipe/generic)
- [ ] `card-machined` card style (gradient + inset highlight + shadow)
- [ ] Seed catalog → 12 hardware SKUs; categories All/Plumbing/Tools/Fasteners/Paint/Garden/Electrical
- [ ] POS rebuild → exact `selling.jsx` layout (customer card, search ⌘K, chips w/ counts, 4-col grid, ticket panel S-00010 with SKU lines, steppers, totals, Charge xl)
- [ ] Checkout rebuild → two machined panels, charges grid, payment buttons, keypad (1-3/C, 4-6/←, 7-9/., 00/0), quick-amount chips
- [ ] Receipt screen → confirmation + perforated receipt paper preview + PAID stamp
- [ ] Theme toggle (Daylight/Counter) surfaced in the app bar
- [ ] Add-to-cart "+1 fly" + count-up animations

## ⏭️ After the rebuild (per SCREENS.md phases)
Sales list · Parties · Dashboard · Purchases · Stock · Income/Expense · Reports suite ·
Returns · Auth/Login · Business Setup · User roles · Subscription/licensing · Play release.

## 📌 Key facts
- applicationId `com.nexapos.retail`, minSdk 26 / target 35. Currency `Rs` (`util/Money.kt`).
- Money stored as integer cents. Business sample: QUINCAILLERIE RB TRADING (Curepipe, Mauritius).
- Build gotcha: run `ktlintFormat` in its OWN gradle invocation before `ktlintCheck`.
