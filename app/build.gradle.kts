plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.fit.realestate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fit.realestate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    // XML
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)

    // Phone Code
    implementation(libs.ccp)

    // Image Profile
    implementation(libs.github.glide)

    // MapBox
//    implementation("com.mapbox.maps:android:10.15.0")
//    implementation("com.mapbox.maps:android:10.14.1")
    implementation(libs.android)
    implementation(libs.annotation)
    implementation(libs.maps.style)
    implementation(libs.autofill)
    implementation(libs.discover)
    implementation(libs.place.autocomplete)
    implementation(libs.offline)
    implementation(libs.mapbox.search.android)
    implementation(libs.search.mapbox.search.android.ui)
    implementation("com.mapbox.search:mapbox-search-android:1.0.0-beta.32")


    // Google Map
    implementation(libs.places)

    // Carousel
    implementation(libs.material)
    implementation(libs.github.glide)

    // Firebase
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.play.services.auth)

    // Default Android
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}