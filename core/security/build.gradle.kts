plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.matrix.synapse.security"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Token encryption uses KeystoreCrypto (Android Keystore) directly; ciphertext is held in
    // plain SharedPreferences.
    // DEPRECATED: security-crypto is kept only for the one-time legacy migration in TokenStoreImpl.
    // TODO: keep for several releases — F-Droid users update irregularly and a user who skips the
    // migration release would otherwise lose their session. Only then delete the migration code + dep.
    implementation(libs.security.crypto)

    // Coroutines
    implementation(libs.coroutines.android)

    // Unit tests
    testImplementation(libs.bundles.unit.test)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.test.runner)
}
