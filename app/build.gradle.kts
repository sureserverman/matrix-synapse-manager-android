import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.matrix.synapse.manager"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "com.matrix.synapse.manager"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "1.3.6"

        testInstrumentationRunner = "com.matrix.synapse.manager.HiltTestRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile")!!)
                storePassword = keystoreProperties.getProperty("storePassword")
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            vcsInfo.include = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Avoid duplicate META-INF/LICENSE.md from JUnit Jupiter jars in androidTest
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}

// Baseline profiles are non-deterministic — disable for reproducible builds
tasks.configureEach {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:model"))
    implementation(project(":core:resources"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":core:security"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:devices"))
    implementation(project(":feature:servers"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:users"))
    implementation(project(":feature:rooms"))
    implementation(project(":feature:stats"))
    implementation(project(":feature:media"))
    implementation(project(":feature:federation"))
    implementation(project(":feature:jobs"))
    implementation(project(":feature:moderation"))

    // Compose BOM — all Compose versions managed here
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.compose)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.bundles.lifecycle)
    debugImplementation(libs.compose.ui.tooling)

    // Hilt — KSP is required with Kotlin 2.0; kapt is deprecated
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Reorderable list (drag-and-drop)
    implementation("org.burnoutcrew.composereorderable:reorderable-jvm:0.9.6")

    // Coroutines
    implementation(libs.coroutines.android)

    // Coil with disk cache for avatars (shared by stats, rooms, users)
    implementation("io.coil-kt.coil3:coil:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")

    // Unit tests
    testImplementation(libs.bundles.unit.test)

    // Instrumented tests
    androidTestImplementation(libs.bundles.android.test)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
