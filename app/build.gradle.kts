import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
} else {
    throw GradleException("local.properties file not found. Please create one with SONGIFY_BASE_URL and SONGIFY_API_KEY.")
}

android {
    namespace = "com.example.songify"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.songify"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Fetch properties and throw an error if they are missing.
        val songifyBaseUrl = localProperties.getProperty("SONGIFY_BASE_URL")
        val songifyApiKey = localProperties.getProperty("SONGIFY_API_KEY")

        if (songifyBaseUrl == null || songifyApiKey == null) {
            throw GradleException("SONGIFY_BASE_URL and/or SONGIFY_API_KEY not found in local.properties.")
        }

        buildConfigField("String", "SONGIFY_BASE_URL", "\"$songifyBaseUrl\"")
        buildConfigField("String", "SONGIFY_API_KEY", "\"$songifyApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("io.coil-kt:coil:2.6.0")


    // Required for PlayerControlView and PlayerView
    implementation("androidx.media3:media3-ui:1.2.1")

    // Ensure these are also present for the rest of your code
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
}