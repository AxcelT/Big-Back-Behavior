# Big Back Behavior

A calorie and macro tracking API, built as a deliberate exercise in enterprise Java patterns.

The long-term goal is a transactional ledger for nutrition: every meal is an append-only entry against a daily calorie and macro budget, rather than a mutable running total. The project is being built layer by layer, and the architecture docs describe each layer's purpose *before* it is written — the documentation is part of the exercise, not an afterthought.

📖 **The architecture docs live on the [GitHub Wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki)** — that is the single source of truth for them. See [Documentation](#documentation) for the page index.

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

Each layer has a full wiki page explaining its one job, its annotations, the rules for what belongs in it, and the mistakes that produce confusing errors — see [Documentation](#documentation) below.

---

## Documentation

The architecture docs live on the **[GitHub Wiki](https://github.com/AxcelT/Big-Back-Behavior/wiki)**, and only there. This repo holds code and the two files at its root; it deliberately keeps no copy of the wiki pages.

| Page | Covers |
| --- | --- |
| [Home](https://github.com/AxcelT/Big-Back-Behavior/wiki/Home) | how the layers fit together, and a Flask → Spring translation table |
| [Model Layer](https://github.com/AxcelT/Big-Back-Behavior/wiki/Model-Layer) | entities, `@Entity`, why `Integer` and not `int` |
| [Repository Layer](https://github.com/AxcelT/Big-Back-Behavior/wiki/Repository-Layer) | `JpaRepository`, derived queries, `Optional` |
| [Service Layer](https://github.com/AxcelT/Big-Back-Behavior/wiki/Service-Layer) | business rules, `@Transactional`, and why it is intentionally absent |
| [Controller Layer](https://github.com/AxcelT/Big-Back-Behavior/wiki/Controller-Layer) | HTTP mapping, JSON serialization, DTOs |
| [Glossary](https://github.com/AxcelT/Big-Back-Behavior/wiki/Glossary) | beans, DI, JPA vs Hibernate, JPQL, starters |

All of it is written for someone whose background is Python/Flask rather than Java.

Two docs stay in the repo, because they describe the code rather than the architecture:

- **README.md** (this file) — what the project is and how to run it
- **[DEV-CONTEXT.md](DEV-CONTEXT.md)** — where development currently stands, decisions made, and what to pick up next

To edit the wiki, use the web UI or clone it — it is a **separate repository**, so it has its own history and is untouched by anything you push to `main`:

```bash
git clone https://github.com/AxcelT/Big-Back-Behavior.wiki.git
```

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

If you add a layer, update that layer's wiki page status line as part of the same piece of work — the docs tracking reality is the point of them. Because the wiki is a separate repository, this cannot ride along in the code commit; it is a second push, and nothing will remind you. Treat it as part of "done."

The wiki has no CI and no review — a push to its `master` is live immediately.
