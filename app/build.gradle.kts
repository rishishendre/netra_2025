plugins {
    id("com.android.application")

}

android {
    namespace = "com.example.myapplication3"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication3"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }

    buildFeatures {
        viewBinding = true
        mlModelBinding = true
        dataBinding = true

    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation(project(mapOf("path" to ":opencv")))
    implementation("androidx.activity:activity:1.9.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("com.github.Krusshnaa:Joystick_Lib:1.0")
    implementation(project(":opencv"))
    implementation ("com.github.yukuku:ambilwarna:2.0.1")

    implementation ("com.google.android.gms:play-services-vision:20.1.3")
    implementation ("com.google.mlkit:text-recognition:16.0.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0")

    val cameraxVersion = "1.4.0-alpha03"
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
}