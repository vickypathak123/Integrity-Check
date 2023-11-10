plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.safetynet.integritycheck"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
    kotlinOptions {
        jvmTarget = "19"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.multidex:multidex:2.0.1")

    implementation("com.google.android.play:integrity:1.2.0")
    implementation("com.google.apis:google-api-services-playintegrity:v1-rev20231018-2.0.0") {
        exclude(group = "com.google.guava", module = "guava")
    }

    implementation("com.google.guava:guava") {
        version {
            strictly("31.1-android")
        }
    }
//  Google Authentication
    implementation ("com.google.api-client:google-api-client-jackson2:1.20.0")
    implementation ("com.google.auth:google-auth-library-credentials:1.20.0")
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.20.0")
}

afterEvaluate {
    publishing {
        publications {
            register("release", MavenPublication::class) {
                from(components["release"])
                groupId = "com.github.sagarpatel1137"
                artifactId = "Integrity-Check"
                version = "1.0.0"
            }
        }
    }
}