plugins {
  java
}

group = "dev.gigglebyte"
version = "1.0.0"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

repositories {
  maven {
    url = uri("https://repo.papermc.io/repository/maven-public/")
  }
  mavenCentral()
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testImplementation("org.mockito:mockito-core:5.14.2")
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.release.set(21)
}

tasks.jar {
  archiveBaseName.set("LanternEmbers")
}

sourceSets.named("test") {
  java.srcDir("../proof/src/test/java")
}

tasks.test {
  useJUnitPlatform()
}
