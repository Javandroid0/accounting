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