import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Адреса и ключи сервера берём из local.properties (он не попадает в git).
// Если ключей нет — приложение работает на демо-данных.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String): String = "\"" + localProps.getProperty(key, "") + "\""

android {
    namespace = "com.example.homepanel"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.example.homepanel"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "JELLYFIN_URL", secret("jellyfin.url"))
        buildConfigField("String", "JELLYFIN_KEY", secret("jellyfin.key"))
        buildConfigField("String", "QBIT_URL", secret("qbit.url"))
        buildConfigField("String", "QBIT_USER", secret("qbit.user"))
        buildConfigField("String", "QBIT_PASSWORD", secret("qbit.password"))
        buildConfigField("String", "SEERR_URL", secret("seerr.url"))
        buildConfigField("String", "SEERR_KEY", secret("seerr.key"))
        buildConfigField("String", "RADARR_URL", secret("radarr.url"))
        buildConfigField("String", "RADARR_KEY", secret("radarr.key"))
        buildConfigField("String", "SONARR_URL", secret("sonarr.url"))
        buildConfigField("String", "SONARR_KEY", secret("sonarr.key"))
        buildConfigField("String", "PROWLARR_URL", secret("prowlarr.url"))
        buildConfigField("String", "PROWLARR_KEY", secret("prowlarr.key"))
        buildConfigField("String", "MEDIADL_URL", secret("mediadl.url"))
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    // Список языков (en, ru, kk) собирается из папок values-* автоматически:
    // его видят системные настройки «Язык приложения» на Android 13+
    androidResources {
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")  // смена языка внутри приложения
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Сеть: Retrofit + OkHttp + kotlinx.serialization для JSON
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Локальная база: сохранённые тайтлы (Room + корутины/Flow)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Настройки пользователя (сортировка сохранённого)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Постеры
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
