plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.maxkach.swipingcards"
    compileSdk = 36
    defaultConfig {
        minSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates("com.maxkach", "swipingcards", "0.1.0")

    pom {
        name.set("SwipingCards")
        description.set("A Compose Multiplatform swipeable card-stack widget with swipe-to-cycle gestures, infinite deck, and spring animations.")
        inceptionYear.set("2026")
        url.set("https://github.com/makzimi/SwipingCards")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/makzimi/SwipingCards/blob/main/LICENSE")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("makzimi")
                name.set("Maxim Kachinkin")
                url.set("https://github.com/makzimi")
            }
        }
        scm {
            url.set("https://github.com/makzimi/SwipingCards")
            connection.set("scm:git:git://github.com/makzimi/SwipingCards.git")
            developerConnection.set("scm:git:ssh://git@github.com/makzimi/SwipingCards.git")
        }
    }
}
