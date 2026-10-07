// Pure Kotlin/JVM, and it must stay that way: the agenda of a day built from the calendar's
// instances, the day's sentence, the fit of a widget, the next moment a surface must change.
// All business logic lives here and is tested on the JVM without Android (PLANNING.md §2, §12).
plugins {
    alias(libs.plugins.tempo.jvm.library)
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
}
