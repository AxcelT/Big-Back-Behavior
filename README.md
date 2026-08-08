# Big Back Behavior

A calorie and macro tracking API, built as a deliberate exercise in enterprise Java patterns.

The long-term goal is a transactional ledger for nutrition: every meal is an append-only entry against a daily calorie and macro budget, rather than a mutable running total. The project is being built layer by layer, and the architecture docs describe each layer's purpose *before* it is written — the documentation is part of the exercise, not an afterthought.

📖 **Read the docs on the [GitHub Wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki).** A copy also lives in [`wiki/`](wiki/) in this repo; see [Documentation](#documentation) for why there are two and which to trust.

> **Status: early scaffold.** The application boots, creates its database schema, and answers a health check. There is no HTTP access to the data yet. See [Current state](#current-state) for exactly what exists.

---

## Stack

| Piece | Choice | Note |
| --- | --- | --- |
| Language | Java 17 | `java.version` in `pom.xml`; JDK 21 also builds it fine |
| Framework | Spring Boot 4.1.0 | Starters were renamed in Boot 4 — see [Gotchas](#gotchas) |
| Persistence | Spring Data JPA / Hibernate | No SQL is written by hand |
| Database | H2, in-memory | **Current.** PostgreSQL is the production target |
| Web | Spring MVC (embedded Tomcat) | `spring-boot-starter-webmvc` |
| Ops | Spring Boot Actuator | `/actuator/health` only, by default |
| Build | Maven via the wrapper (`mvnw`) | Maven 3.9.16 pinned |
| License | MIT | see [LICENSE](LICENSE) |

---

## Running it

No installation beyond a JDK — the Maven wrapper fetches everything else.

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```bash
.\mvnw.cmd spring-boot:run
```

The app listens on **port 8081**, not Spring's default 8080 (set in [`src/main/resources/application.properties`](src/main/resources/application.properties)).

Verify it is up:

```bash
curl http://localhost:8081/actuator/health
```

Expected: `{"status":"UP"}`. Anything else — including `http://localhost:8081/` — returns Spring's default error page, because no controller is mapped yet. That is expected, not a bug.

### Tests

```bash
./mvnw test
```

One test, `DemoApplicationTests.contextLoads`, which asserts the Spring context starts. It passes.

### Packaging

```bash
./mvnw clean package
```

Produces an executable jar in `target/`, runnable with `java -jar`.

---

## Current state

```
src/main/java/com/example/demo/
├── DemoApplication.java     entry point — @SpringBootApplication
└── FoodLog.java             the only entity: id, foodName, calories

src/main/resources/
└── application.properties   app name + port 8081

src/test/java/com/example/demo/
└── DemoApplicationTests.java

wiki/                        layer-by-layer architecture docs
```

Of the four conventional Spring layers, only the model exists:

| Layer | Status |
| --- | --- |
| Model | ✅ `FoodLog.java` |
| Repository | ❌ next to build |
| Service | ❌ deliberately deferred until there is a real business rule |
| Controller | ❌ blocked on the repository |

At startup Hibernate reads `FoodLog` and creates a `FOOD_LOG` table with `ID`, `FOOD_NAME`, and `CALORIES` columns. The table is real and populated-able from Java, but unreachable over HTTP.

For what comes next and why, see [DEV-CONTEXT.md](DEV-CONTEXT.md).

---

## Architecture

Four layers, dependencies pointing downward only. A controller may know about a service; a model must never know a controller exists.

```
HTTP request
    ↓
Embedded Tomcat → DispatcherServlet     (framework)
    ↓
Controller    speaks HTTP               (not built)
    ↓
Service       business rules            (not built, on purpose)
    ↓
Repository    database access           (not built)
    ↓
Hibernate → H2                          (framework)
```

Each layer has a full page explaining its one job, its annotations, the rules for what belongs in it, and the mistakes that produce confusing errors. See [Documentation](#documentation) below.

---

## Documentation

Every page exists in **two places**, and they are separate git repositories that are kept in step by hand:

| | Published wiki | In-repo copy |
| --- | --- | --- |
| Where | [github.com/AxcelT/Big-Back-Behavior/wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki) | [`wiki/`](wiki/) |
| Repo | `Big-Back-Behavior.wiki.git` (branch `master`) | this repo (branch `main`) |
| Cross-page links | ✅ work | ❌ render as literal `[[Page Name]]` |
| Edit in browser | ✅ | ❌ |
| Reviewed in PRs | ❌ | ✅ |

The pages use `[[Wiki Link]]` syntax, which GitHub only resolves inside a wiki. **Prefer the published wiki for reading** — the in-repo copy has the same words but dead navigation.

The contents are byte-identical as of 2026-08-08. Nothing enforces that, so treat any difference as drift rather than intent, and see [DEV-CONTEXT.md](DEV-CONTEXT.md) for the open decision on which copy becomes the source of truth.

| Page | Covers |
| --- | --- |
| Home ([wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki/Home) · [repo](wiki/Home.md)) | how the layers fit together, and a Flask → Spring translation table |
| Model Layer ([wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki/Model-Layer) · [repo](wiki/Model-Layer.md)) | entities, `@Entity`, why `Integer` and not `int` |
| Repository Layer ([wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki/Repository-Layer) · [repo](wiki/Repository-Layer.md)) | `JpaRepository`, derived queries, `Optional` |
| Service Layer ([wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki/Service-Layer) · [repo](wiki/Service-Layer.md)) | business rules, `@Transactional`, and why it is intentionally absent |
| Controller Layer ([wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki/Controller-Layer) · [repo](wiki/Controller-Layer.md)) | HTTP mapping, JSON serialization, DTOs |
| Glossary ([wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki/Glossary) · [repo](wiki/Glossary.md)) | beans, DI, JPA vs Hibernate, JPQL, starters |

All of it is written for someone whose background is Python/Flask rather than Java.

---

## Gotchas

**Spring Boot 4 renamed the starters.** Most tutorials and Stack Overflow answers are for Boot 3. If a dependency "doesn't exist," this is usually why:

| Boot 3 (most tutorials) | Boot 4 (this project) |
| --- | --- |
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-test` | split — `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` |
| H2 console bundled | separate `spring-boot-h2console` module |

**Classes must live under `com.example.demo`.** Component scanning starts at `DemoApplication`'s package and walks downward. A controller outside that tree is never registered — no error, just a silent 404.

**The database is in memory.** Everything written is discarded when the app stops. Intentional for now.

**The H2 console dependency is present but not switched on.** To browse the schema, add `spring.h2.console.enabled=true` to `application.properties`, restart, and open `http://localhost:8081/h2-console` with JDBC URL `jdbc:h2:mem:testdb`, user `sa`, no password.

**Never expose all actuator endpoints publicly.** `/actuator/env` leaks configuration including credentials; `/actuator/heapdump` hands out a memory snapshot. Only `health` is web-exposed by default — keep it that way.

---

## Contributing

Conventional-commit prefixes are used throughout the history: `feat:`, `fix:`, `chore:`, `docs:`.

Before opening a PR, run `./mvnw test`.

If you add a layer, update that layer's page status line in the same change — the docs tracking reality is the point of them. **Update both copies**, or they drift: `wiki/` in this repo, and the published wiki, which is its own clone:

```bash
git clone https://github.com/AxcelT/Big-Back-Behavior.wiki.git
```

The wiki clone has no CI and no review — a push to its `master` is live immediately.
