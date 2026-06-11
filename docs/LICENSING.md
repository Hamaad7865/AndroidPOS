# Multi-Branch Licensing (developer-only)

The multi-branch add-on is unlocked by an offline, ECDSA-P256-signed licence
code bound to the customer's business name. The app embeds only the **public**
key; the **private** key and the generator (`tools/licensing/LicenseTool.java`)
stay with you and are **never committed** (`tools/` is gitignored).

## Code format

```
NXB-<urlBase64(payload)>.<urlBase64(signature)>
payload = UTF-8 "business|maxBranches|expiryEpochDay"   (expiry 0 = never)
signature = SHA256withECDSA(payload) under the developer private key
```

`business` binds the code to one customer. At verify time both the code's name
and the install's business name are **normalized** (upper-case, keep
alphanumerics + single spaces) and compared, so a code for one shop won't unlock
another's install. The business name **must not contain `|`**.

## Keys

- Public key (in the app): `LicenseManager.PUBLIC_KEY_B64`.
- Private key: generated once with `LicenseTool keygen`; store it somewhere
  secure and offline. **If you lose it you cannot issue new codes** for the
  installed public key (you'd ship a new public key in an app update).

Regenerate a fresh keypair (only if you ever need to rotate):

```
java tools/licensing/LicenseTool.java keygen
# -> PUBLIC <b64>   (paste into LicenseManager.PUBLIC_KEY_B64)
# -> PRIVATE <b64>  (keep secret)
```

## Issuing a code for a customer

```
java tools/licensing/LicenseTool.java issue <privateKeyB64> "Quincaillerie RB Trading" 4 0
# -> NXB-...   (send this to the customer; they paste it in Settings -> Multi-branch)
```

- `4` = max branches they paid for (HQ refuses to register more).
- `0` = never expires; or pass an epoch-day (e.g. `LocalDate.toEpochDay()`) for a
  time-limited / subscription code.

The customer enters the code once in **Settings → Multi-branch**; it is verified
locally with no internet and re-verified on every app start.
