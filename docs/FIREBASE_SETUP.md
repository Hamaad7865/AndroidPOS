# Multi-Branch — Firebase Setup (owner, one time)

The multi-branch add-on syncs through **your own** Firebase project. NexaPOS uses
no `google-services.json` and no build-time config — you paste three values into
the app, so any branch can be pointed at the same project in minutes. Firebase's
free (Spark) tier comfortably covers a small multi-shop business; set a budget
alert anyway (step 6).

## 1. Create the project
1. Go to <https://console.firebase.google.com> → **Add project**. Name it (e.g.
   `nexapos-rbtrading`). Google Analytics is optional — you can skip it.

## 2. Register an Android app
1. In the project, **Add app → Android**.
2. **Android package name:** `com.nexapos.retail` (exactly).
3. Skip the `google-services.json` download — NexaPOS doesn't use it.

## 3. Copy the three connection values
**Project settings (gear icon) → General → Your apps → the Android app.** You need:
- **Project ID** — e.g. `nexapos-rbtrading`.
- **App ID** — looks like `1:534000000000:android:abc123def456`.
- **API key** ("Web API Key" / the Android key) — looks like `AIzaSy…`.

## 4. Turn on Authentication
**Build → Authentication → Get started → Sign-in method → Email/Password →
Enable.** You'll create one **business account** (one email + password) that every
device of your business signs in with.

## 5. Turn on Firestore + lock it down
1. **Build → Firestore Database → Create database → Production mode** (any region
   close to Mauritius, e.g. `europe-west`).
2. **Rules tab** → replace the contents with the rules from
   [`firestore.rules`](../firestore.rules) in this repo → **Publish**. These
   restrict every business to its own data (`request.auth.uid == businessId`).

## 6. Set a budget alert (recommended)
Firebase console → ⚙ → **Usage and billing → Details & settings →** set a budget
alert (e.g. a few dollars) so you're warned if usage ever grows.

## 7. Configure the app on each device
In NexaPOS on each shop's device: **Settings → Multi-branch**:
1. Enter the **licence key** (paid add-on) — see `docs/LICENSING.md`.
2. **This shop:** pick **Branch** or **Head office**, set a short **branch code**
   (e.g. `A`, `HQ`, `CUR1`) and a display name → **Save this shop**.
3. **Cloud sync:** paste **Project ID / App ID / API key**, enter the **business
   email + password**, → **Connect & sync**. The first device to sign in creates
   the account; the rest reuse it.

After this, each branch publishes its summary after every sale and a full push on
shift close. Cross-branch viewing (the Branches screen) and head-office controls
arrive in the following updates.

## Notes
- **Offline-first is untouched.** Selling never waits on or fails because of sync;
  a sync problem just shows "offline" in Settings and retries later.
- **History** accrues from the day you connect. (Back-filling older days is a
  planned follow-up.)
- Auto-sync on every sale starts after the **next app restart** once you first
  configure multi-branch; **Connect & sync** pushes immediately in the meantime.
