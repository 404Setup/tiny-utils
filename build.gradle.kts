plugins {
    `java-library`
    idea
    signing

    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "one.pkg"
version = "2.2.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:26.0.2")

    compileOnly("com.github.luben:zstd-jni:1.5.7-4")
    compileOnly("com.aayushatharva.brotli4j:brotli4j:1.18.0")
    compileOnly("it.unimi.dsi:fastutil:8.5.15")
    compileOnly("com.google.guava:guava:33.4.0-jre")
    compileOnly("com.google.code.gson:gson:2.12.1")
    compileOnly("com.github.oshi:oshi-core:6.9.1")
}

val targetJavaVersion = 17

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
}

tasks.jar {
    manifest {
        val map = mutableMapOf<String, String>()
        map["Specification-Title"] = rootProject.name
        map["Specification-Vendor"] = "404Setup"
        map["Specification-Version"] = version as String
        map["Specification-License"] = "The Apache License, Version 2.0"
        map["Specification-Src"] = "https://github.com/404Setup/tiny-utils"
        attributes(map)
    }

    from("LICENSE") {
        rename { "${it}_${rootProject.name}" }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
    options.release = targetJavaVersion
}

tasks.withType<ProcessResources> {
    filteringCharset = Charsets.UTF_8.name()
}

val apiAndDocs: Configuration by configurations.creating {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

configurations.api {
    extendsFrom(apiAndDocs)
}

mavenPublishing {
    coordinates(group as String, "tiny-utils", version as String)

    pom {
        name.set("TinyUtils")
        description.set("Small auxiliary development tools")
        inceptionYear.set("2025")
        url.set("https://github.com/404Setup/tiny-utils")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("404")
                name.set("404Setup")
                url.set("https://github.com/404Setup")
            }
        }
        scm {
            url.set("https://github.com/404Setup/tiny-utils")
            connection.set("scm:git:git://github.com/404Setup/tiny-utils.git")
            developerConnection.set("scm:git:ssh://git@github.com/404Setup/tiny-utils.git")
        }
    }

    publishToMavenCentral()

    signAllPublications()
}
