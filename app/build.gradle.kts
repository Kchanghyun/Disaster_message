import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
    id ("com.google.gms.google-services")
}

android {
    namespace = "com.example.disaster_message"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.disaster_message"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            val localProperties = Properties()
            localProperties.load(FileInputStream(localPropertiesFile))
            val apiKey = localProperties.getProperty("GPT_API_KEY")
                ?: throw GradleException("GPT_API_KEY is missing in local.properties")
            val map_api_key = localProperties.getProperty("GOOGLE_MAP_API_KEY")

            buildConfigField("String", "GPT_API_KEY", "\"$apiKey\"")
            buildConfigField("String", "GOOGLE_MAP_API_KEY", "\"$map_api_key\"")
        } else {
            throw GradleException("local.properties file not found!")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        viewBinding.isEnabled = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0, LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.ui.graphics.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-messaging-directboot:24.1.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")
}

//apply plugin: "com.google.gms.google-services"