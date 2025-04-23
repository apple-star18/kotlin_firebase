// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        google()  // 구글 저장소 추가
        mavenCentral()
    }
    dependencies {
        // Firebase 및 Google 서비스 플러그인 추가
        classpath("com.google.gms:google-services:4.3.15")  // Firebase Google 서비스 플러그인
    }
}