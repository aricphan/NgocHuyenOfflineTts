import java.net.URL
import java.util.zip.ZipInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.aricphan.offlinetts"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aricphan.offlinetts"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    androidResources {
        noCompress += listOf("onnx", "json", "txt")
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val downloadVoiceAssets by tasks.registering {
    group = "voice"
    description = "Download Ngoc Huyen ONNX voice assets from https://github.com/aricphan/onnxvoice"

    val outputDir = layout.projectDirectory.dir("src/main/assets/voice")
    val marker = outputDir.file(".downloaded-from-onnxvoice")

    outputs.file(marker)

    doLast {
        val dir = outputDir.asFile
        dir.mkdirs()

        val mustHave = listOf(
            "ngoc-huyen.onnx",
            "ngoc-huyen.onnx.json",
            "tokens.txt",
            "espeak-ng-data"
        )

        val alreadyOk = mustHave.all { File(dir, it).exists() }
        if (alreadyOk) {
            marker.asFile.writeText("Assets already exist.\n")
            println("Voice assets already exist: ${dir.absolutePath}")
            return@doLast
        }

        val zipFile = layout.buildDirectory.file("onnxvoice-main.zip").get().asFile
        zipFile.parentFile.mkdirs()
        println("Downloading voice assets from GitHub...")
        URL("https://github.com/aricphan/onnxvoice/archive/refs/heads/main.zip").openStream().use { input ->
            zipFile.outputStream().use { output -> input.copyTo(output) }
        }

        println("Extracting assets into: ${dir.absolutePath}")
        ZipInputStream(zipFile.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val prefix = "onnxvoice-main/assets/"
                if (!entry.name.startsWith(prefix)) continue
                val relativeName = entry.name.removePrefix(prefix)
                if (relativeName.isBlank()) continue

                val target = File(dir, relativeName)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }

        val missing = mustHave.filterNot { File(dir, it).exists() }
        check(missing.isEmpty()) { "Missing voice asset(s): $missing" }
        marker.asFile.writeText("Downloaded from aricphan/onnxvoice main.\n")
        println("Voice assets ready.")
    }
}

tasks.named("preBuild") {
    dependsOn(downloadVoiceAssets)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.bihe0832.android:lib-sherpa-onnx:8.5.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
