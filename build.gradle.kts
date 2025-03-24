plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.3.6"
	id("io.spring.dependency-management") version "1.1.7"
	`maven-publish`
}

group = "com.mcp.server"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["springAiVersion"] = "1.0.0-M6"

dependencies {
	implementation("org.springframework.ai:spring-ai-mcp-server-spring-boot-starter")
	implementation("org.springframework:spring-web")
	implementation("io.kubernetes:client-java:18.0.1")
	implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

publishing {
	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/OWNER/REPOSITORY")
			credentials {
				username = System.getenv("GITHUB_ACTOR")
				password = System.getenv("GITHUB_TOKEN")
			}
		}
	}
	publications {
		create<MavenPublication>("gpr") {
			from(components["java"])
		}
	}
}
