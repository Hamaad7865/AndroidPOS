# Receipt Scan → Register Purchase — Design Spec

- **Date:** 2026-06-08
- **Status:** Approved (brainstorming) → ready for implementation plan
- **Feature owner request:** "Suppliers give a hardcopy receipt. Let the owner take a picture of it and register everything in the app."

## Goal

Let the shop owner photograph a supplier's hardcopy receipt and, after a quick
review, register it as a **Purchase** (supplier + line items + costs + stock
increase + Money/ledger) while keeping the **product catalog** in sync. Stays
fully offline, SQLCipher-encrypted, no cloud, no subscription.

## Decisions (from brainstorming)

1. **What "register" creates:** *Both* — a full Purchase/goods-receipt **and** a
   catalog refresh (new items created, known items linked/updated).
2. **Review:** *Editable draft → Confirm.* OCR misreads never silently hit stock
   or Money.
3. **Receipt types:** *Mix of printed + handwritten.* Flow must auto-extract
   printed receipts AND fall back to easy manual entry for handwritten ones.
4. **OCR engine:** Google **ML Kit** on-device text recognition (bundled,
   offline, no Play Services required), feeding the editable review screen.

## User flow

1. **Capture** — "Scan receipt" on the Purchase screen → take a photo or pick an
   image. Saved via the existing `ImageStore`.
2. **OCR** — ML Kit reads the image (text + line layout) on-device (~1–2s spinner).
3. **Parse** — `ReceiptParser` produces a best-effort draft: guessed supplier +
   line items `{name, qty, unitCost}` + warnings.
4. **Review & edit** — `ReceiptReviewScreen` opens pre-filled with the draft and a
   receipt thumbnail. Owner fixes supplier, edits/adds/deletes lines, corrects
   name/qty/cost; each line shows **matched** vs **new** product; running total.
   (Handwritten/unreadable → same screen as fast manual entry, photo on screen.)
5. **Register** — *Register purchase* → calls the existing
   `PurchasesViewModel.recordPurchase(supplier, items, …)`, which creates the
   supplier if new, creates/links products, records the Purchase, and raises
   stock. Returns to the Purchase list with the new PO.

## Architecture

**New, focused units:**
- `ReceiptOcr` — thin wrapper over ML Kit (`bitmap → text + line layout`).
  Isolated for swap/mock.
- `ReceiptParser` — **pure Kotlin, no Android deps, unit-tested** (same pattern as
  `ProductDocs`). `OcrResult → ParsedReceipt { supplierGuess, lines, warnings }`.
- `ReceiptReviewScreen` + `ReceiptScanViewModel` — editable draft UI/state;
  builds `List<PurchaseDraftItem>` and calls `recordPurchase`.

**Reused (not rebuilt):**
- `PurchasesViewModel.recordPurchase` — already auto-creates supplier + products,
  records the Purchase, and raises stock. The review screen produces the exact
  `PurchaseDraftItem` list it consumes.
- Product matching (barcode→SKU→name) — factored into one shared helper used by
  both CSV import and receipt lines ("reuse the import pipeline" steer).
- `ImageStore`, existing image-pick pattern, theme/screen components.

## Parser strategy (best-effort)

- Detect money amounts (`Rs`, decimals, thousands separators) via the existing
  `parseMoney`-style cleanup.
- Pair an amount with its line's description text and a leading quantity.
- **Exclude** non-item lines: total / subtotal / VAT / TVA / balance / change /
  tendered / "amount due".
- Supplier guessed from the top text block.
- Output is intentionally forgiving; correctness is finalized in the review screen.

## Photo storage — no schema change

Save the receipt image through `ImageStore`, keyed to the purchase **code**
(e.g. `receipt-PO-1044.jpg`); load by convention on the review + purchase-detail
screens. **No new DB column**, so app updates won't trigger
`fallbackToDestructiveMigration` and wipe the client's live data. (A first-class
`Purchase.receiptImagePath` column with a real Room migration is a possible
future upgrade, out of scope here.)

## Dependency

ML Kit **bundled** Latin text-recognition (`com.google.mlkit:text-recognition`):
fully on-device, works without Play Services, adds ~a few MB to the APK. Added to
the version catalog + `app/build.gradle.kts`.

## Error handling

- No camera permission → request; if denied, allow pick-from-gallery.
- Blurry/empty OCR → land in the manual review screen with the photo shown and a
  "couldn't read it — enter manually" note (never a dead end).
- Registration is one atomic `recordPurchase` call — never half-committed.
- Cancel discards the draft and the captured image.

## Testing

- `ReceiptParser` unit tests: sample printed-receipt texts → expected line items;
  total/VAT lines excluded; messy money formats (`Rs 1,200.50`, `1.200,00`);
  empty/garbage input → empty draft + warning.
- Manual emulator pass: capture → OCR → review edits → register → PO + stock
  updated; handwritten/blank path → manual entry works.

## Out of scope (future)

- First-class `receiptImagePath` DB column + real migration.
- Confidence-based auto-accept of high-confidence lines.
- Multi-page / multi-receipt batch scanning.
