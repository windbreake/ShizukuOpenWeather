plugins {
    base
}

tasks.register<Exec>("checkRust") {
    group = "verification"
    commandLine("rustc", "--version")
}

tasks.register<Exec>("checkCargo") {
    group = "verification"
    commandLine("cargo", "--version")
}

tasks.register<Exec>("checkNode") {
    group = "verification"
    commandLine("node", "--version")
}

tasks.register<Exec>("checkTypeScript") {
    group = "verification"
    commandLine("tsc", "--version")
}

tasks.register<Exec>("checkKotlin") {
    group = "verification"
    commandLine("kotlinc", "-version")
}

tasks.register<Exec>("checkSqlite") {
    group = "verification"
    commandLine("sqlite3", "--version")
}

tasks.register("checkDevEnvironment") {
    group = "verification"
    dependsOn("checkRust", "checkCargo", "checkNode", "checkTypeScript", "checkKotlin", "checkSqlite")
}

tasks.register<Exec>("buildRust") {
    group = "build"
    commandLine("cargo", "build", "--workspace")
}

tasks.register<Exec>("testRust") {
    group = "verification"
    commandLine("cargo", "test", "--workspace")
}

tasks.register<Exec>("installWeb") {
    group = "build"
    workingDir = file("apps/web")
    commandLine("npm", "install")
}

tasks.register<Exec>("buildWeb") {
    group = "build"
    dependsOn("installWeb")
    workingDir = file("apps/web")
    commandLine("npm", "run", "build")
}

tasks.register("buildAll") {
    group = "build"
    dependsOn("buildRust", "buildWeb", ":apps:api:build")
}

tasks.register("buildAndroid") {
    group = "build"
    description = "Builds the Android debug APK."
    dependsOn(":apps:android:assembleDebug")
}
