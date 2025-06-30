plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.primetrace.testviseme"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.primetrace.testviseme"
        minSdk = 26  // Minimum SDK is now 26 for adaptive icons support
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // BuildConfig fields
        buildConfigField("boolean", "DEBUG", "true")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            // Add build config fields specific to debug
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Add build config fields specific to release
            buildConfigField("String", "BUILD_TYPE", "\"release\"")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        // Enable view binding
        viewBinding = true
        // Enable build config generation
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.constraintlayout)
    
    // Audio processing
    implementation("androidx.media:media:1.6.0")
    implementation("androidx.media3:media3-session:1.2.0")
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media:media:1.6.0")
    implementation("androidx.core:core-ktx:1.12.0")
    
    // Activity KTX for viewModels()
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}