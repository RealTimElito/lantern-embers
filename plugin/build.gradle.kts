plugins {
  java
}

group = "dev.gigglebyte"
version = "1.0.0"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

repositories {
  maven {
    url = uri("https://repo.papermc.io/repository/maven-public/")
  }
  mavenCentral()
}

val paper262: Configuration by configurations.creating

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testImplementation("org.mockito:mockito-core:5.14.2")
  paper262("io.papermc.paper:paper-api:26.2.build.133-stable")
  paper262("org.jetbrains:annotations:26.0.2")
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.release.set(21)
}

tasks.jar {
  archiveBaseName.set("LanternEmbers")
}

val compileJava262 = tasks.register<JavaCompile>("compileJava262") {
  source = sourceSets.main.get().allJava
  classpath = paper262
  destinationDirectory.set(layout.buildDirectory.dir("classes/java/main262"))
  options.encoding = "UTF-8"
  options.release.set(21)
  javaCompiler.set(javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(25))
  })
}

val processResources262 = tasks.register<Copy>("processResources262") {
  from(sourceSets.main.get().resources) {
    exclude("plugin.yml")
  }
  from(sourceSets.main.get().resources) {
    include("plugin.yml")
    filter { line ->
      if (line.trimStart().startsWith("api-version:")) "api-version: '26.2'" else line
    }
  }
  into(layout.buildDirectory.dir("resources/main262"))
}

tasks.register<Jar>("jar262") {
  dependsOn(compileJava262, processResources262)
  archiveBaseName.set("LanternEmbers")
  archiveClassifier.set("26.2")
  from(compileJava262.flatMap { it.destinationDirectory })
  from(processResources262)
}

sourceSets.named("test") {
  java.srcDir("../proof/src/test/java")
}

tasks.test {
  useJUnitPlatform()
}
