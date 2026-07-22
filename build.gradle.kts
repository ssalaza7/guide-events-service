plugins {
	java
	id("org.springframework.boot") version "4.0.7"
	id("io.spring.dependency-management") version "1.1.7"
	jacoco
}

group = "com.tcc"
version = "0.0.1-SNAPSHOT"
description = "Servicio de recepcion y publicacion de eventos de estado de guias (TCC)"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("io.projectreactor.rabbitmq:reactor-rabbitmq:1.5.6")
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-redis-reactive-test")
	testImplementation("io.projectreactor:reactor-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

// pin: Boot 4 sube amqp-client a 5.27.1, incompatible con reactor-rabbitmq 1.5.6 (NoSuchMethodError en useNio)
dependencyManagement {
	dependencies {
		dependency("com.rabbitmq:amqp-client:5.14.2")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.jacocoTestReport)
	violationRules {
		rule {
			element = "BUNDLE"
			excludes = listOf("com.tcc.guideevents.GuideEventsServiceApplication")
			limit {
				counter = "LINE"
				minimum = "0.90".toBigDecimal()
			}
			limit {
				counter = "INSTRUCTION"
				minimum = "0.90".toBigDecimal()
			}
		}
	}
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification)
}
