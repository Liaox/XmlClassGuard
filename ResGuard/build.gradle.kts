plugins {
    id("java")
    id("kotlin")
    id("maven-publish")
}
//group = "io.github.goldfish07.reschiper"
//version = "0.1.0-rc4"

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

//repositories {
//    mavenCentral()
//    google()
//}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(gradleApi())
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.android.tools.build:gradle:7.0.2")
    implementation("com.android.tools.build:bundletool:1.13.2")
    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("io.grpc:grpc-protobuf:1.59.1")
    implementation("com.android.tools.build:aapt2-proto:7.0.2-7396180")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.dom4j:dom4j:2.1.4")
    implementation("com.google.auto.value:auto-value:1.5.4")
    annotationProcessor("com.google.auto.value:auto-value:1.5.4")
}

publishing {
    repositories {
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
    }
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.overseas.reschiper"
            version = "0.1.0-rc4"
            artifactId = "plugin"
            from(components["java"])
        }
    }
}