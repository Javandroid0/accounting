plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.javandroid.accounting_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.javandroid.accounting_app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
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
}

dependencies {
//
//    implementation(libs.appcompat) {
//        exclude(group = "com.intellij", module = "annotations")
//    }

    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.escpos.thermalprinter.android)
    implementation(libs.androidx.work.runtime)
    implementation(libs.mockito.core)
    implementation(libs.androidx.core.testing)
    implementation(libs.mockito.inline)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    annotationProcessor(libs.room.compiler)
//    implementation(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.zxing.android.embedded)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.recyclerview)
//    implementation(libs.annotations)
    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)

//    implementation(libs.poi.ooxml)
}