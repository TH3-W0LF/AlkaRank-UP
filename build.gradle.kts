import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    `maven-publish`
}

group = "com.alkacode"
version = "1.0.18"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    // motor de economia centralizado da network - substitui o Vault como ponte
    // (publicado via `./gradlew publishToMavenLocal` no projeto AlkaEconomy). AlkaCore
    // e necessario tambem porque AlkaEconomyPlugin agora estende
    // com.alkacode.core.plugin.AlkaPlugin (o javac precisa da hierarquia completa).
    compileOnly("com.alkacode:AlkaCore:1.0.2")
    compileOnly("com.alkacode:AlkaEconomy:1.0.5")
    compileOnly("me.clip:placeholderapi:2.11.6")
    // icone de rank opcional via item/bloco custom do ItemsAdder - mesma versao/padrao
    // ja usado em plugins/AlkaMines (nao existe tag 3.6.4 publicada no jitpack).
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    // sem isso, o Gradle nao percebe que so `version` mudou e reusa o plugin.yml
    // antigo do cache (processResources fica UP-TO-DATE incorretamente).
    inputs.property("version", project.version)
    expand("version" to project.version)
}

// publica o jar "puro" (sem o sqlite-jdbc relocado do shadowJar) no repositorio
// Maven local, para outros plugins Alka* (ex: AlkaMines) consumirem via compileOnly.
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "AlkaRankUp"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
