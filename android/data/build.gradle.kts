plugins {
    alias(libs.plugins."android-library")
    alias(libs.plugins."kotlin-android")
    alias(libs.plugins."kotlin-kapt")
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.eduquiz.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
