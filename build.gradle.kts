plugins {
  kotlin("jvm") version "2.3.0"
}

group = "coroutine"
version = "0.0.1"

repositories {
  mavenCentral()
}

dependencies {
  implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  testImplementation(platform("org.junit:junit-bom:6.0.1"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core:3.27.6")
  testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(25)
}
