import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.travelcash.krmpzv"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath =
        providers.gradleProperty("TRAVEL_CASH_STORE_FILE").orNull
          ?: System.getenv("KEYSTORE_PATH")
          ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword =
        providers.gradleProperty("TRAVEL_CASH_STORE_PASSWORD").orNull
          ?: System.getenv("STORE_PASSWORD")
      keyAlias =
        providers.gradleProperty("TRAVEL_CASH_KEY_ALIAS").orNull
          ?: "upload"
      keyPassword =
        providers.gradleProperty("TRAVEL_CASH_KEY_PASSWORD").orNull
          ?: System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

abstract class CopyTravelCashApkTask : org.gradle.api.DefaultTask() {
  @get:org.gradle.api.tasks.InputFile
  abstract val sourceApk: org.gradle.api.file.RegularFileProperty

  @get:org.gradle.api.tasks.OutputFile
  abstract val targetApk: org.gradle.api.file.RegularFileProperty

  @get:org.gradle.api.tasks.Internal
  abstract val customDestinationDir: org.gradle.api.provider.Property<String>

  @org.gradle.api.tasks.TaskAction
  fun copyApk() {
    val src = sourceApk.get().asFile
    val dest = targetApk.get().asFile
    dest.parentFile?.mkdirs()
    src.copyTo(dest, overwrite = true)

    val customDirStr = customDestinationDir.orNull
    if (!customDirStr.isNullOrBlank()) {
      try {
        val customDir = file(customDirStr)
        customDir.mkdirs()
        val customApk = file("${customDir.absolutePath}/TravelCash.apk")
        src.copyTo(customApk, overwrite = true)
        logger.lifecycle("TravelCash.apk guardado y sobreescrito con éxito en: ${customApk.absolutePath}")
      } catch (e: Exception) {
        logger.warn("No se pudo copiar el APK al directorio personalizado: ${e.message}")
      }
    }
  }
}

val copyTravelCashReleaseApk =
  tasks.register("copyTravelCashReleaseApk", CopyTravelCashApkTask::class.java) {
    sourceApk.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    targetApk.set(layout.buildDirectory.file("outputs/apk/release/TravelCash.apk"))
    val defaultCustomDir =
      providers.gradleProperty("TRAVEL_CASH_CUSTOM_APK_DIR").orNull
        ?: "C:/Users/perez/Desktop/basura/Aplicaciones/TravelCash"
    customDestinationDir.set(defaultCustomDir)
    dependsOn("packageRelease")
  }

tasks.matching { it.name == "assembleRelease" }.configureEach {
  finalizedBy(copyTravelCashReleaseApk)
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.osmdroid.android)
  // implementation(libs.maps.compose)
  // implementation(libs.play.services.maps)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
