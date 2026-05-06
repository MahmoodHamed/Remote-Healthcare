plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.rpm.watch"
    compileSdk = 34

    defaultConfig {
        applicationId          = "com.rpm.watch"
        // Galaxy Watch 4+ runs Wear OS 3 (API 30); Watch 8 runs Wear OS 4 (API 33)
        minSdk                 = 30
        targetSdk              = 34
        versionCode            = 1
        versionName            = "1.0.0"

        // MQTT broker settings – override per build variant or inject at runtime via DataStore
        buildConfigField("String", "MQTT_HOST", "\"192.168.1.100\"")   // change to server IP
        buildConfigField("int",    "MQTT_PORT", "1883")
        buildConfigField("String", "DEFAULT_PATIENT_ID", "\"00000000-0000-0000-0000-000000000001\"")
        buildConfigField("boolean", "LOCAL_SENSOR_ONLY", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    // ── Local Samsung Health Sensor SDK ──────────────────────────────────
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    // ── Android / Lifecycle ───────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.activity.compose)

    // ── Wear OS ───────────────────────────────────────────────────────────
    implementation(libs.wear)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)

    // ── Compose (required by Wear Compose) ────────────────────────────────
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // ── Hilt ──────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ── MQTT (HiveMQ lightweight client – no Android Service dependency) ─
    implementation(libs.hivemq.mqtt.client) {
        // Exclude SLF4J API – use Android Log instead
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    // ── Serialization & Coroutines ────────────────────────────────────────
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // ── DataStore ─────────────────────────────────────────────────────────
    implementation(libs.datastore.preferences)

    // ── Tests ─────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
}
