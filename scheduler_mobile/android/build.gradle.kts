// 1. IMPORT CLASS ANDROID AGAR TIDAK ERROR "UNRESOLVED REFERENCE"
import com.android.build.gradle.BaseExtension

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}

// --- [SCRIPT PENYELAMAT: FIX NAMESPACE] ---
// Ditaruh DI ATAS 'evaluationDependsOn' agar tidak telat
subprojects {
    // Kita cek kondisi dulu, kalau sudah dieksekusi, langsung jalankan.
    // Kalau belum, pakai afterEvaluate.
    if (project.state.executed) {
        fixNamespace(project)
    } else {
        project.afterEvaluate {
            fixNamespace(project)
        }
    }
}

// Fungsi pembantu untuk mengisi namespace
fun fixNamespace(project: Project) {
    val android = project.extensions.findByName("android")
    if (android != null) {
        // Menggunakan configure dengan BaseExtension agar property 'namespace' dikenali
        project.configure<BaseExtension> {
            if (namespace == null) {
                namespace = project.group.toString()
            }
        }
    }
}
// ------------------------------------------

subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}