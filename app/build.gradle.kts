plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.th.optimizer"
    compileSdk = 34
    defaultConfig { applicationId = "com.th.optimizer"; minSdk = 29; targetSdk = 34; versionCode = 11; versionName = "4.1" }
    buildTypes { release { isMinifyEnabled = true } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
