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
        viewBinding = true;
        compose = true;
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
    implementation("com.mapbox.maps:android:11.12.4")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.mapbox.extension:maps-style:10.16.0")

//    implementation("com.mapbox.search:autofill:2.12.0-beta.1")
//    implementation("com.mapbox.search:discover:2.12.0-beta.1")
//    implementation("com.mapbox.search:place-autocomplete:2.12.0-beta.1")
//    implementation("com.mapbox.search:offline:2.12.0-beta.1")
//    implementation("com.mapbox.search:mapbox-search-android:2.12.0-beta.1")
//    implementation("com.mapbox.search:mapbox-search-android-ui:2.12.0-beta.1")

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