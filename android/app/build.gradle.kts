plugins { id("com.android.application");id("org.jetbrains.kotlin.android");id("org.jetbrains.kotlin.plugin.compose");id("org.jetbrains.compose") }
android {
 namespace = "cn.gdcp.timetable"
 compileSdk = 35
 defaultConfig { applicationId = "cn.gdcp.timetable";minSdk = 26;targetSdk = 35;versionCode = 67;versionName = "3.2.20";testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner";resValue("string", "app_title", "个人课表") }
 testOptions { unitTests.isIncludeAndroidResources = true }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17;targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildTypes {
  release { isMinifyEnabled = false }
  debug {
   applicationIdSuffix = ".preview"
   versionNameSuffix = "-preview"
   resValue("string", "app_title", "个人课表·验证版")
  }
 }
 packaging { resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1") }
}
dependencies {
 implementation(files("libs/haze-android-1.5.4.aar"))
 implementation("top.yukonga.miuix.kmp:miuix:0.5.1")
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation(compose.animation)
 implementation(compose.foundation)
 implementation(compose.ui)
 implementation(compose.runtime)
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
 testImplementation("org.robolectric:robolectric:4.14.1")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.json:json:20240303")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
