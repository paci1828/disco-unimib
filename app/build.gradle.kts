plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.simone.discounimib"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.simone.discounimib"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}
configurations.all {
    resolutionStrategy {
        // ⭐ Aggiornato a 1.68.1 per risolvere NoClassDefFoundError
        val grpcVersion = "1.68.1"
        force("io.grpc:grpc-core:$grpcVersion")
        force("io.grpc:grpc-stub:$grpcVersion")
        force("io.grpc:grpc-protobuf-lite:$grpcVersion")
        force("io.grpc:grpc-okhttp:$grpcVersion")
        force("io.grpc:grpc-android:$grpcVersion")
        force("io.grpc:grpc-api:$grpcVersion")
    }
}
dependencies {

    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.play.services.auth)

    // Multidex
    implementation("androidx.multidex:multidex:2.0.1")

    // ⭐ gRPC Dependencies (Allineate a 1.68.1)
    val grpcVersion = "1.68.1"
    implementation("io.grpc:grpc-core:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-protobuf-lite:$grpcVersion")
    implementation("io.grpc:grpc-okhttp:$grpcVersion")
    implementation("io.grpc:grpc-android:$grpcVersion")
    implementation("io.grpc:grpc-api:$grpcVersion")

    // Google API
    implementation(libs.google.api.client.android) {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava")
        exclude(group = "io.grpc")
    }

    implementation(libs.google.api.calendar) {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava")
        exclude(group = "io.grpc")
    }

    implementation(libs.google.api.gmail) {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava")
        exclude(group = "io.grpc")
    }

    implementation(libs.google.http.gson) {
        exclude(group = "org.apache.httpcomponents")
    }

    // Mail
    implementation(libs.javax.mail)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
