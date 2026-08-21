# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build state

`main` builds and tests green as of `970782a` (PR #8). All four layers exist and `POST /logs` / `GET /logs` are verified working end to end.

Historical note: `FoodLogRepository.java` was merged as a 0-byte file in `6c8dc1d` (PR #6), which broke the build for two commits until PR #8 filled it in. There is no CI, so nothing caught it.

## Commands

All Maven goes through the wrapper so everyone builds with the pinned Maven 3.9.16 — never a globally installed `mvn`. On Windows PowerShell use `.\mvnw.cmd`; the Bash tool and `./mvnw` also work.

```bash
./mvnw spring-boot:run
```

```bash
./mvnw test
```

```bash
./mvnw -Dtest=DemoApplicationTests#contextLoads test
```

```bash
./mvnw clean package
```

The local `~/.m2` cache is populated, so `./mvnw -o test` and `-o compile` work offline. **`spring-boot:run` does not** — the plugin needs artifacts (`spring-boot-loader-tools`, `maven-shade-plugin`) that were never cached, so run it online.

The app listens on **port 8081**, not 8080 (`src/main/resources/application.properties`). Health check: `curl http://localhost:8081/actuator/health`.

## Architecture

A calorie/macro tracking API. Four Spring layers, dependencies pointing downward only — a controller may know a service; a model must never know a controller exists.

```
HTTP → Tomcat/DispatcherServlet → Controller → (Service) → Repository → Hibernate → H2
```

Currently `FoodLogController` injects `FoodLogRepository` directly by constructor; there is no service layer. All five classes are flat in `com.example.demo` — the wiki's code samples show `com.example.demo.model` / `.repository` sub-packages that do not exist. Whichever layout wins, **everything must stay under `com.example.demo`** or component scanning silently skips it (no error, just a 404).

Live endpoints: `POST /logs`, `GET /logs`. Everything else, including `/`, returns Spring's default error page.

## Spring Boot 4 starter renames

Boot 4.1.0. Most tutorials are Boot 3; when a dependency "doesn't exist," this is usually why:

| Boot 3 | Boot 4 (this project) |
| --- | --- |
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-test` | split — `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` |
| H2 console bundled | separate `spring-boot-h2console` module |

Both test starters are declared and currently unused — `@DataJpaTest` and `@WebMvcTest` are available without touching `pom.xml`.

## Settled decisions

Do not re-litigate these without a reason; rationale is in [DEV-CONTEXT.md](DEV-CONTEXT.md) §2.

- **Bottom-up, one layer per commit:** Model → Repository → Controller.
- **No service layer until there is a real business rule.** A `FoodLogService` that only forwards to the repository is a file to open for nothing. It gets created in the same commit as the first non-HTTP `if`.
- **Wrapper types (`Long`, `Integer`) in entities, never primitives** — `null` must stay distinguishable from `0`; an unrecorded calorie count is not a zero-calorie meal.
- **Entities are returned directly; no DTOs yet.** Introduce them when the entity gains non-public fields or the API shape diverges from the table.
- **H2 in-memory now, PostgreSQL later.** All data is lost on shutdown, intentionally. Adopt Flyway/Liquibase *before* the first persistent database — Hibernate auto-DDL is unacceptable against one.
- **Actuator stays minimally exposed** — `health` only over HTTP. `/actuator/env` leaks credentials, `/actuator/heapdump` hands out a memory snapshot.
- H2 console dependency is present but off. To browse the schema add `spring.h2.console.enabled=true`, restart, open `http://localhost:8081/h2-console` with JDBC URL `jdbc:h2:mem:testdb`, user `sa`, no password.

## Documentation discipline

**Check the wiki before answering anything architectural.** It is the only home for architecture docs, and it is expected to grow — new pages land there, not in this repo. Nothing in this repository explains *why* a layer is shaped the way it is; the wiki does.

It is a separate repository (`Big-Back-Behavior.wiki.git`, branch `master`) cloned locally at `D:\GithubRepos\Big-Back-Behavior.wiki`, so it is on disk and greppable rather than needing a fetch:

```bash
grep -ril "topic" D:/GithubRepos/Big-Back-Behavior.wiki
```

Pages as of this writing: `Home`, `Model-Layer`, `Repository-Layer`, `Service-Layer`, `Controller-Layer`, `Glossary` — list the directory rather than trusting that set, since it is being added to. The local clone can also be behind, because the wiki is editable directly in the GitHub web UI; `git -C D:/GithubRepos/Big-Back-Behavior.wiki pull` before relying on it.

Pushing this repo does nothing to it, and it has no CI or review — a push is live immediately. Do not recreate a `wiki/` directory here; `.gitignore` deliberately does not guard against it.

**The wiki pages carry no build-status lines** — they were removed so the pages stay conceptual. Shipping a layer therefore does not oblige a wiki push; edit a page when the design changes or an explanation is wrong. Wiki code samples are deliberately idealized and linked to the real files; where a sample and the source differ, that is tracked work, not a doc bug.

The two in-repo docs describe the code rather than the architecture, and both are **currently stale**: [README.md](README.md) and [DEV-CONTEXT.md](DEV-CONTEXT.md) still list the repository and controller layers as unbuilt, and DEV-CONTEXT's HEAD/commit timeline stops at `7ae30ee`. Refresh them when touching the layers they describe.

## Conventions

Conventional-commit prefixes throughout the history: `feat:`, `fix:`, `chore:`, `docs:`. Work lands via PR from a `feat-*` branch. Run `./mvnw test` before opening one.
