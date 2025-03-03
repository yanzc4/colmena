plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "app.codemultiall.lacolmena"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.codemultiall.lacolmena"
        minSdk = 24
        targetSdk = 33
        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    //libreria lottie
    implementation("com.airbnb.android:lottie:6.5.2")

    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    // define any required OkHttp artifacts without version
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.3.0") // Para comandos ESC/POS
    implementation("androidx.appcompat:appcompat:1.7.0") // Para compatibilidad
    implementation("com.google.firebase:firebase-messaging:22.0.0")
    implementation("com.pusher:push-notifications-android:1.10.0")
    implementation("androidx.browser:browser:1.8.0")
}
