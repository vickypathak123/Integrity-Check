plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")

}

android {
    namespace = "com.safetynet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.safetynet"
        minSdk = 23
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
    }
}
val integrityVersion: String by rootProject.extra
dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(project(":integritycheck"))
//    implementation("com.github.vickypathak123:Integrity-Check:1.0.5")
}