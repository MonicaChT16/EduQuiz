plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.eduquiz.feature.store"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
}

dependencies {
        implementation(project(":core"))
        implementation(project(":domain"))
        implementation(project(":feature-auth"))
        
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.foundation)
        debugImplementation(libs.androidx.compose.ui.tooling)
        
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.ktx)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.androidx.hilt.navigation.compose)
        
        implementation(libs.hilt.android)
        kapt(libs.hilt.compiler)
        
        // Para cargar imágenes desde URL
        implementation("io.coil-kt:coil-compose:2.6.0")
        implementation("io.coil-kt:coil-gif:2.6.0")
    }
