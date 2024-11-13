import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.theimpartialai.speechScribe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.theimpartialai.speechScribe"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
        vectorDrawables {
            useSupportLibrary = true
        }

        val secureProps = Properties()
        if (rootProject.file("secure.properties").exists()) {
            secureProps.load(FileInputStream(rootProject.file("secure.properties")))
        }

        buildConfigField("String", "AWS_USER_POOL_ID", "\"${secureProps["AWS_USER_POOL_ID"]}\"")
        buildConfigField("String", "AWS_CLIENT_ID", "\"${secureProps["AWS_CLIENT_ID"]}\"")
        buildConfigField(
            "String",
            "AWS_IDENTITY_POOL_ID",
            "\"${secureProps["AWS_IDENTITY_POOL_ID"]}\""
        )
        buildConfigField("String", "AWS_REGION", "\"${secureProps["AWS_REGION"]}\"")
        buildConfigField("String", "AWS_BUCKET_NAME", "\"${secureProps["AWS_BUCKET_NAME"]}\"")
        buildConfigField("String", "AWS_DEFAULT_USER", "\"${secureProps["AWS_DEFAULT_USER"]}\"")
        buildConfigField("String", "AWS_DEFAULT_PASSWORD", "\"${secureProps["AWS_DEFAULT_PASSWORD"]}\"")
        buildConfigField("String", "AWS_DEFAULT_NEW_PASSWORD", "\"${secureProps["AWS_DEFAULT_NEW_PASSWORD"]}\"")
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(project(":opus"))
    implementation(libs.gagravarr.vorbis.java.core)
    implementation(libs.compose.audiowaveform)
    implementation(libs.androidx.navigation.compose)

    // S3 dependencies
    implementation(libs.aws.android.sdk.s3)
    implementation(libs.aws.android.core)
    implementation(libs.aws.android.sdk.cognitoidentityprovider)

    // Firebase dependencies
    implementation(libs.firebase.analytics)
    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.security.crypto)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
}