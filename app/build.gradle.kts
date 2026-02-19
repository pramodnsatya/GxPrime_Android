import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Read API key from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    FileInputStream(localPropertiesFile).use { localProperties.load(it) }
}

android {
    namespace = "com.pramod.validator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pramod.validator"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Read OpenAI API key from local.properties or use empty string as fallback
        val openAiApiKey = localProperties.getProperty("OPENAI_API_KEY", "")
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiApiKey\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "org/bouncycastle/LICENSE"
            // Ensure PDFBox resources are NOT excluded
            // The resources should be automatically included from the JAR
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.storage)
    
    // Email Service
    implementation("com.sendgrid:sendgrid-java:4.9.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // HTTP client for OpenAI API
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    
    // Lottie for animations
    implementation(libs.lottie.compose)
    
    // PDF text extraction - using older stable version with better resource handling
    implementation("com.tom-roush:pdfbox-android:1.8.10.3") {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
    }
    
    // Accompanist for enhanced UI components
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)
    implementation(libs.accompanist.swiperefresh)
    // Core Compose setup (aligns versions automatically)
    implementation(platform("androidx.compose:compose-bom:2024.09.01"))

    // Material 3 UI library
    implementation("androidx.compose.material3:material3")

    // Core + Extended Material Icons
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Optional – Lottie for animations (if you use icons/animations from Lottie)
    implementation("com.airbnb.android:lottie-compose:6.0.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}