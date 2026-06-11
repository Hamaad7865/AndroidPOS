import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.nexapos.retail"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexapos.retail"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Credentials live in keystore.properties (gitignored). When absent
            // (e.g. on a fresh checkout or CI), the release build is left unsigned.
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties =
                    Properties().apply { load(FileInputStream(keystorePropertiesFile)) }
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (rootProject.file("keystore.properties").exists()) {
                    signingConfigs.getByName("release")
                } else {
                    null
                }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}

// Export the Room schema (exportSchema = true) so future schema bumps can be
// validated with a real migration test instead of silently wiping the DB.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Fail fast when a release build is attempted without keystore.properties.
// Debug builds are unaffected — only tasks whose name contains "Release" are guarded.
gradle.taskGraph.whenReady {
    val releaseTaskRequested =
        allTasks.any { task ->
            task.name.contains("Release", ignoreCase = true) &&
                (task.name.startsWith("assemble") || task.name.startsWith("bundle"))
        }
    if (releaseTaskRequested && !rootProject.file("keystore.properties").exists()) {
        throw GradleException(
            "keystore.properties not found — cannot sign a release build. " +
                "Create keystore.properties with storeFile/storePassword/keyAlias/keyPassword, " +
                "or use assembleDebug for an unsigned local build.",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.services.code.scanner)
    implementation(libs.mlkit.text.recognition)

    // Multi-branch add-on (optional, paid): Firestore + Auth. Initialised lazily from
    // the owner's pasted config — no google-services plugin / google-services.json.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
