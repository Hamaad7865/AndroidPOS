# Add project specific ProGuard rules here.
# By default the flags in this file are appended to flags specified
# in proguard-android-optimize.txt (see app/build.gradle.kts).

# Room entities need their field names preserved for SQLite column mapping.
# The broad data.** keep is intentionally narrowed to entity sub-packages only;
# security/crypto classes must NOT be preserved here as they should be obfuscated.
# TODO: if runtime reflection errors appear for DAOs or migrations, add targeted
#       -keep rules per class rather than re-broadening to data.**.
-keep class com.nexapos.retail.data.entity.** { *; }
