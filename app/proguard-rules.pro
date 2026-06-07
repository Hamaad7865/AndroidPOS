# Add project specific ProGuard rules here.
# By default the flags in this file are appended to flags specified
# in proguard-android-optimize.txt (see app/build.gradle.kts).

# Room entities need their field names preserved for SQLite column mapping.
# The broad data.** keep is intentionally narrowed to entity sub-packages only;
# security/crypto classes must NOT be preserved here as they should be obfuscated.
# TODO: if runtime reflection errors appear for DAOs or migrations, add targeted
#       -keep rules per class rather than re-broadening to data.**.
-keep class com.nexapos.retail.data.entity.** { *; }

# SQLCipher (net.zetetic:android-database-sqlcipher). The native libsqlcipher.so
# resolves Java fields/methods BY NAME via JNI — e.g. SQLiteDatabase.mNativeHandle
# inside register_android_database_SQLiteCompiledSql / JNI_OnLoad. If R8 renames or
# strips them the app hard-aborts (SIGABRT, NoSuchFieldError) the first time it opens
# the encrypted DB. Keep the whole package verbatim.
-keep class net.sqlcipher.** { *; }
-keep interface net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
