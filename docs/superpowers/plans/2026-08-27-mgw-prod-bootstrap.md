# mgw-prod Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a runnable Spring Boot project connected to a local MySQL database, so the `users`/`marketplace`/`challenges` module plans have a project to build into.

**Architecture:** Single Maven/Spring Boot project at the repo root (`projects/mgw-prod/`), matching the exact stack the cátedra's own Clase 3 skeleton generated (see `facultad/aplicaciones-interactivas/clases/2026-08-20-clase-03/demo/pom.xml`): Spring Boot 4.1.0, Java 21, Lombok. Package root is `com.mgwprod`.

**Tech Stack:** Java 21, Spring Boot 4.1.0 (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`), MySQL 9 (`com.mysql:mysql-connector-j`), Lombok, Maven.

## Global Constraints

- Java version: **21** (matches cátedra skeleton).
- Spring Boot parent version: **4.1.0** (matches cátedra skeleton — do not use an older 3.x tutorial's artifact names; this version splits the old `spring-boot-starter-web` into `spring-boot-starter-webmvc`, and splits `spring-boot-starter-test` into `spring-boot-starter-data-jpa-test` + `spring-boot-starter-webmvc-test`).
- Lombok is available and wired into both `compile` and `test-compile` via `maven-compiler-plugin` annotationProcessorPaths (copied verbatim from the cátedra skeleton) — use Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`) instead of hand-written boilerplate.
- **Two deliberate additions beyond the cátedra skeleton**, both justified in the design spec (`docs/superpowers/specs/2026-08-25-mgw-prod-tpo-design.md`):
  - `spring-boot-starter-validation` — needed for `@Valid`/Bean Validation on request DTOs (not pulled in by `webmvc` alone in this Spring Boot version).
  - No Spring Security. Auth is homemade (see the `users` module plan) using only the JDK's `java.security.MessageDigest` — no BCrypt, no `spring-security-crypto`, to avoid depending on anything not seen in class.
- MySQL local connection matches Clase 2's `DataBaseConnection.java` convention: user `root`, password `admin`, host `localhost:3306`. Database name: `mgw_prod`.
- Package root: `com.mgwprod`.
- All commands in this plan assume the working directory is the project root: `projects/mgw-prod/`.
- Work happens on a branch (never commit to `main` directly). Create `feature/etapa1-bootstrap` from the current `docs/tpo-design` branch before starting Task 1.

---

### Task 1: Project skeleton, MySQL connectivity, and smoke test

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/mgwprod/MgwProdApplication.java`
- Create: `src/main/resources/application.properties`
- Create: `src/test/java/com/mgwprod/MgwProdApplicationTests.java`

**Interfaces:**
- Produces: a bootable Spring Boot app on port 8080, connected to a MySQL database named `mgw_prod`, with Lombok and Bean Validation available for every later task/module to use.

Note on TDD sequencing: this task is pure scaffolding — there is no business behavior to write a failing test against before the project exists. The usual "write the failing test first" step is replaced with "create the skeleton, then verify it boots and connects" (manual check), followed by an automated regression test that keeps this guarantee going forward.

- [ ] **Step 1: Create the branch**

```bash
cd "projects/mgw-prod"
git checkout docs/tpo-design
git checkout -b feature/etapa1-bootstrap
```

- [ ] **Step 2: Create the local MySQL database**

Run this against your local MySQL server (same `root`/`admin` credentials as Clase 2's `GestorDeInventario`):

```bash
mysql -u root -padmin -e "CREATE DATABASE IF NOT EXISTS mgw_prod;"
```

Expected: no error. If MySQL isn't running, start it first (same local MySQL service used in Clase 2).

- [ ] **Step 3: Write `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.0</version>
		<relativePath/>
	</parent>
	<groupId>com.mgwprod</groupId>
	<artifactId>mgw-prod</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>mgw-prod</name>
	<description>Music Discovery and Challenge Platform - TPO Aplicaciones Interactivas</description>

	<properties>
		<java.version>21</java.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<executions>
					<execution>
						<id>default-compile</id>
						<phase>compile</phase>
						<goals>
							<goal>compile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
					<execution>
						<id>default-testCompile</id>
						<phase>test-compile</phase>
						<goals>
							<goal>testCompile</goal>
						</goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
```

- [ ] **Step 4: Write `src/main/resources/application.properties`**

```properties
spring.application.name=mgw-prod
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/mgw_prod
spring.datasource.username=root
spring.datasource.password=admin

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

- [ ] **Step 5: Write the main application class**

`src/main/java/com/mgwprod/MgwProdApplication.java`:

```java
package com.mgwprod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MgwProdApplication {
    public static void main(String[] args) {
        SpringApplication.run(MgwProdApplication.class, args);
    }
}
```

- [ ] **Step 6: Run the app and verify it boots and connects to MySQL**

```bash
mvn spring-boot:run
```

Expected: console shows `Started MgwProdApplication` with no stack trace, and no `Communications link failure` / connection errors. Stop it with Ctrl+C once confirmed.

- [ ] **Step 7: Write the automated smoke test**

`src/test/java/com/mgwprod/MgwProdApplicationTests.java`:

```java
package com.mgwprod;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MgwProdApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 8: Run the test suite and verify it passes**

```bash
mvn test
```

Expected: `BUILD SUCCESS`, 1 test run, 0 failures. (Requires the local MySQL server from Step 2 to be running — this test boots the full Spring context, including the datasource connection.)

- [ ] **Step 9: Commit**

```bash
git add pom.xml src/main/java/com/mgwprod/MgwProdApplication.java src/main/resources/application.properties src/test/java/com/mgwprod/MgwProdApplicationTests.java
git commit -m "feat: bootstrap Spring Boot project with MySQL connectivity"
```

---

## Self-Review

**Spec coverage:** This plan covers only the shared infra prerequisite (project skeleton, DB connectivity) — not a spec requirement by itself, but the foundation every `users`/`marketplace`/`challenges` task depends on. No gaps for what this plan is scoped to cover.

**Placeholder scan:** No TBD/TODO; every step has concrete file content.

**Type consistency:** N/A (single task, no cross-task types yet — `com.mgwprod` package root is the only contract handed to later plans).
