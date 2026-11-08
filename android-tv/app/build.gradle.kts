import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.stemstream.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stemstream.tv"
        minSdk = 23
        targetSdk = 36
        versionCode = 13
        versionName = "1.2.1"

        // The published catalogue, refreshed on launch and every few minutes.
        // Override for a fork or a local test server with
        //   ./gradlew assembleRelease -PstemstreamCatalogUrl=http://10.0.2.2:5173/catalog.json
        val catalogUrl = (findProperty("stemstreamCatalogUrl") as String?)
            ?.takeIf { it.isNotBlank() }
            // The canonical site origin. The github.io address only redirects
            // here, and a redirect is a wasted round trip on a TV stick.
            ?: "https://novicelab.org/STEMstream/catalog.json"
        buildConfigField("String", "CATALOG_URL", "\"$catalogUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
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
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module",
            )
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// The Android catalogue is generated from the same file the website uses.
// Keeping one source of truth means a daily curation commit also ships in the
// next APK without a second data file to maintain.
val catalogOutputDir = layout.buildDirectory.dir("generated/catalogAssets")
val syncCatalog = tasks.register<Copy>("syncCatalog") {
    from(rootProject.file("../src/data/data.json"))
    into(catalogOutputDir)
    rename { "catalog.json" }
}
android.sourceSets["main"].assets.srcDir(catalogOutputDir)
tasks.named("preBuild") { dependsOn(syncCatalog) }
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(syncCatalog)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")

    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.tv:tv-foundation:1.0.0")

    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
}
