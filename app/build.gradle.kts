import java.net.URL
import java.io.File
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
        versionCode = 2
        versionName = "1.0.1"
    }

    androidResources {
        noCompress += listOf("onnx", "json", "txt", "fst", "far")
    }

    buildFeatures { compose = true }

    kotlinOptions { jvmTarget = "17" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val downloadVoiceAssets by tasks.registering {
    group = "voice"
    description = "Download Ngoc Huyen ONNX voice assets from aricphan/onnxvoice"

    val outputDir = layout.projectDirectory.dir("src/main/assets/voice")
    val marker = outputDir.file(".downloaded-from-onnxvoice")
    outputs.file(marker)

    doLast {
        val dir = outputDir.asFile
        dir.mkdirs()
        val mustHave = listOf("ngoc-huyen.onnx", "ngoc-huyen.onnx.json", "tokens.txt", "espeak-ng-data")
        if (mustHave.all { File(dir, it).exists() }) {
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

        ZipInputStream(zipFile.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val prefix = "onnxvoice-main/assets/"
                if (!entry.name.startsWith(prefix)) continue
                val relativeName = entry.name.removePrefix(prefix)
                if (relativeName.isBlank()) continue
                val target = File(dir, relativeName)
                if (entry.isDirectory) target.mkdirs() else {
                    target.parentFile.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }

        val missing = mustHave.filterNot { File(dir, it).exists() }
        check(missing.isEmpty()) { "Missing voice asset(s): $missing" }
        marker.asFile.writeText("Downloaded from aricphan/onnxvoice main.\n")
        println("Voice assets ready: ${dir.absolutePath}")
    }
}

val downloadSherpaOnnxNative by tasks.registering {
    group = "sherpa"
    description = "Download official sherpa-onnx Android native libraries"

    val jniLibsDir = layout.projectDirectory.dir("src/main/jniLibs")
    val marker = jniLibsDir.file(".downloaded-sherpa-onnx-v1.13.2")
    outputs.file(marker)

    doLast {
        val jniDir = jniLibsDir.asFile
        if (File(jniDir, "arm64-v8a/libsherpa-onnx-jni.so").exists()) {
            marker.asFile.writeText("Native libs already exist.\n")
            println("Sherpa native libs already exist: ${jniDir.absolutePath}")
            return@doLast
        }

        val tarFile = layout.buildDirectory.file("sherpa-onnx-v1.13.2-android.tar.bz2").get().asFile
        tarFile.parentFile.mkdirs()
        println("Downloading official sherpa-onnx Android native libs...")
        URL("https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.2/sherpa-onnx-v1.13.2-android.tar.bz2").openStream().use { input ->
            tarFile.outputStream().use { output -> input.copyTo(output) }
        }

        val extractDir = layout.buildDirectory.dir("sherpa-android-extract").get().asFile
        if (extractDir.exists()) extractDir.deleteRecursively()
        extractDir.mkdirs()

        // Windows 10/11 has tar.exe. Git Bash/macOS/Linux also have tar.
        project.exec {
            commandLine("tar", "-xjf", tarFile.absolutePath, "-C", extractDir.absolutePath)
        }

        val foundJni = extractDir.walkTopDown().firstOrNull { it.isDirectory && it.name == "jniLibs" }
            ?: error("Cannot find jniLibs inside ${tarFile.name}. Please extract it manually and copy jniLibs to app/src/main/jniLibs")

        if (jniDir.exists()) jniDir.deleteRecursively()
        jniDir.mkdirs()
        foundJni.copyRecursively(jniDir, overwrite = true)

        check(File(jniDir, "arm64-v8a/libsherpa-onnx-jni.so").exists()) {
            "Missing arm64-v8a/libsherpa-onnx-jni.so after extraction"
        }
        marker.asFile.writeText("Downloaded official sherpa-onnx v1.13.2 native libs.\n")
        println("Sherpa native libs ready: ${jniDir.absolutePath}")
    }
}

tasks.named("preBuild") {
    dependsOn(downloadVoiceAssets)
    dependsOn(downloadSherpaOnnxNative)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
