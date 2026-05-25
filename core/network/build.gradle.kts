plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.matrix.synapse.network"
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

    buildFeatures {
        // Needed so NetworkModule can gate verbose OkHttp body logging on BuildConfig.DEBUG.
        buildConfig = true
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:security"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Networking — exposed as api so feature modules can use Retrofit annotations and HttpException
    api(libs.bundles.retrofit)
    api(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.coroutines.android)

    // Unit tests
    testImplementation(libs.bundles.unit.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.test.runner)
}
