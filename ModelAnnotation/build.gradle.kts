plugins {
    id("java")
    id("kotlin")
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    jvmToolchain(11)
}
val sourcesJar by tasks.registering(Jar::class) {
    from(sourceSets["main"].allJava)
    archiveClassifier.set("sources")
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.javadoc)
    archiveClassifier.set("javadoc")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.0.2")
    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-util:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(gradleApi())
    implementation(localGroovy())

}

publishing {
    repositories {
        maven {
            isAllowInsecureProtocol = true
//            url = uri("../localmaven")
            url = uri("http://3.0.199.193:7777/repository/maven-releases/")
//            allowInsecureProtocol = true
            if (url.toString().startsWith("http")) {
                credentials {
                    username = "admin"
                    password = "admin123"
                }
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.hz.model.annotation"
            version = "1.0.1"
            artifactId = "plugin"
            from(components["java"])
        }
    }
}