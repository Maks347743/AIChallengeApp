import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.aichallengeapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aichallengeapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DEEPSEEK_BASE_URL", "\"https://api.deepseek.com\"")
        buildConfigField("String", "OLLAMA_BASE_URL", "\"http://10.0.2.2:11434/v1\"")
        buildConfigField("String", "MCP_SERVER_URL", "\"http://10.0.2.2:3001/mcp\"")
        buildConfigField("String", "DEEPWIKI_MCP_URL", "\"https://mcp.deepwiki.com/mcp\"")
        buildConfigField("String", "RAG_MCP_URL", "\"http://10.0.2.2:3002/mcp\"")
        buildConfigField("String", "SUPPORT_MCP_URL", "\"http://10.0.2.2:3003/mcp\"")
        buildConfigField("String", "HOME_SERVER_API_KEY",
            "\"${localProperties.getProperty("HOME_SERVER_API_KEY", "")}\"")
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "DEEPSEEK_API_KEY",
                "\"${localProperties.getProperty("DEEPSEEK_API_KEY", "")}\""
            )
            buildConfigField(
                "String",
                "GITHUB_KEY",
                "\"${localProperties.getProperty("GITHUB_KEY", "")}\""
            )
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField(
                "String",
                "DEEPSEEK_API_KEY",
                "\"\""
            )
            buildConfigField(
                "String",
                "GITHUB_KEY",
                "\"\""
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:mcp"))
    implementation(project(":core:periodic-task"))
    implementation(project(":feature:chat-settings"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:chat-list"))
    implementation(project(":feature:user-preferences"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Security
    implementation(libs.androidx.security.crypto)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Timber
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
