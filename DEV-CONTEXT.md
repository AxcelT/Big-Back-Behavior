# Development Context

**Last updated:** 2026-08-08 · **Branch:** `main` · **HEAD:** `7ae30ee` · **Working tree:** clean

Working notes on the state of development — what has been decided, what is deliberately missing, and what to pick up next. [README.md](README.md) says what the project *is*; this file says where it currently *stands*. Keep it current as the layers land.

---

## 1. Where the project is right now

Eight commits over three weeks (2026-07-20 → 2026-08-02). The scaffold boots and the schema generates; nothing is reachable over HTTP.

| | |
| --- | --- |
| Boots | ✅ `./mvnw spring-boot:run`, port 8081 |
| Tests | ✅ `./mvnw test` — 1 test, `contextLoads`, passing (verified 2026-08-08) |
| Schema | ✅ `FOOD_LOG` table auto-created by Hibernate at startup |
| HTTP surface | ⚠️ `/actuator/health` only — no application endpoints exist |
| Persistence | ⚠️ H2 in-memory; all data lost on shutdown |

### Commit timeline

| Date | Commit | What it did |
| --- | --- | --- |
| 2026-07-20 | `c631ba3` | Initial commit — LICENSE, README |
| 2026-08-01 | `4b9da09` | Spring Initializr scaffold (Boot 4.1.0, Java 17) |
| 2026-08-01 | `52a8e26` | `FoodLog` entity created (PR #2) |
| 2026-08-01 | `fd232c7` | `FoodLog` fields, constructors, accessors |
| 2026-08-02 | `3b2935c` | Merge remote main |
| 2026-08-02 | `e3f0340` | Actuator added, for live introspection in the VS Code dashboard |
| 2026-08-02 | `21d7a04` | Fix: conflict markers left in `.gitignore` by the merge |
| 2026-08-02 | `7ae30ee` | The `wiki/` architecture docs |

Two author identities appear (`AxcelT`, `JuTidalgo`) — same person, different machines. Worth aligning `user.name`/`user.email` across both so history and contribution graphs stay coherent.

---

## 2. Decisions already made

These are settled; don't re-litigate them without a reason.

**Layers get built bottom-up, one at a time.** Model → Repository → Controller. Each layer's wiki page is written before or alongside the code, and its status line is updated in the same commit.

**No service layer until there is a real business rule.** A `FoodLogService` that only calls `repository.findAll()` is a pass-through — a file to open and a step to trace for nothing. It gets created in the same commit as the first non-HTTP `if` statement. Rationale in [wiki/Service-Layer.md](wiki/Service-Layer.md).

**H2 in-memory for now, PostgreSQL later.** JPA sits in between, so the swap should be configuration plus a driver dependency. In-memory keeps iteration fast and needs nothing installed.

**Entities are returned directly; no DTOs yet.** Correct while the model is three fields. Introduce DTOs when the entity gains fields that shouldn't be public, or when the API shape needs to diverge from the table.

**Wrapper types (`Long`, `Integer`) in entities, never primitives.** `null` must stay distinguishable from `0` — an unrecorded calorie count is not a zero-calorie meal.

**Actuator stays minimally exposed.** Only `health` over HTTP. `/actuator/env` and `/actuator/heapdump` are credential and memory leaks respectively.

**Maven wrapper only.** Always `./mvnw`, never a globally installed `mvn`, so everyone builds with pinned Maven 3.9.16.

---

## 3. Next up

In dependency order. Each step is small enough for one commit.

### 3.1 Repository layer — the immediate next task

Create `src/main/java/com/example/demo/FoodLogRepository.java`:

```java
public interface FoodLogRepository extends JpaRepository<FoodLog, Long> { }
```

That is the entire file — Spring Data generates the implementation at startup. Verify by restarting and confirming a `foodLogRepository` bean appears in the VS Code Spring Boot Dashboard's Beans panel. Full detail in [wiki/Repository-Layer.md](wiki/Repository-Layer.md).

### 3.2 Controller layer

`FoodLogController` with `GET /foodlogs`, `GET /foodlogs/{id}`, `POST /foodlogs`, `DELETE /foodlogs/{id}`, injecting the repository directly. A worked example is already in [wiki/Controller-Layer.md](wiki/Controller-Layer.md). This is the commit where the app becomes usable end to end.

### 3.3 First real tests

`contextLoads` proves only that the context starts. Once endpoints exist, add a `@DataJpaTest` for repository behaviour and a `@WebMvcTest` for the controller. Both test starters are already declared in `pom.xml` and unused.

### 3.4 Grow the domain toward the actual goal

The stated product is a **calorie and macro ledger**, but `FoodLog` currently tracks only `foodName` and `calories`. Reaching the goal needs, roughly in order:

- a timestamp on `FoodLog` — without one there is no "today," and no ledger
- macro fields: protein, carbohydrates, fat
- a daily budget concept to log entries against
- a `User`, once anything is multi-tenant

Each of those is an entity or field change plus a migration story — see the open question in §5 about schema management.

### 3.5 Service layer

Falls out of §3.4 naturally: "reject negative calories," "is this user over budget today," "sum today's macros." The first of those rules is the trigger to create the class.

---

## 4. Known issues and debt

Nothing here is blocking; all of it is worth clearing before the codebase grows.

**Project identity is still Initializr defaults.** `groupId` is `com.example`, `artifactId` is `demo`, and the entry point is `DemoApplication`. Renaming touches the package path, so it gets cheaper the sooner it happens — but it also rewrites every wiki code sample. Decide deliberately; don't drift into it.

**`pom.xml` has empty metadata elements.** `<name/>`, `<description/>`, `<url/>`, `<licenses><license/></licenses>`, and empty `<developers>`/`<scm>` blocks are Initializr leftovers. The empty `<license/>` in particular contradicts the MIT [LICENSE](LICENSE) file. Fill them in or delete them.

**The wiki documents a package layout that does not exist.** [wiki/Service-Layer.md](wiki/Service-Layer.md) imports `com.example.demo.model.FoodLog` and `com.example.demo.repository.FoodLogRepository`, but the real code is flat in `com.example.demo`. Pick one — flat is fine at this size, sub-packages are the convention as it grows — and make the code and the wiki agree. Whichever way, everything must stay under `com.example.demo` or component scanning silently skips it.

**The wiki exists twice, in two unconnected repositories.** Verified 2026-08-08:

- `wiki/` in this repo — plain tracked files (mode `100644`), **not** a submodule and not a symlink; there is no `.gitmodules`
- `Big-Back-Behavior.wiki.git` — the published GitHub wiki, a separate repo on branch `master`, cloned locally at `D:\GithubRepos\Big-Back-Behavior.wiki` and currently 0 ahead / 0 behind its origin

All six pages are byte-identical right now. Both received the same content on 2026-08-02 (`7ae30ee` here, `bb01763` there, same commit message), so they were synced by hand. Nothing enforces it, and an edit made through the GitHub wiki web UI will not show up here — the drift is silent in both directions.

**33 wikilinks are dead in the in-repo copy.** The pages navigate with `[[Page Name]]` syntax — 4 in Controller-Layer, 10 in Glossary, 7 in Home, 3 in Model-Layer, 4 in Repository-Layer, 5 in Service-Layer. GitHub resolves that syntax only inside a wiki (converting spaces to hyphens, so `[[Model Layer]]` finds `Model-Layer`). In `wiki/` it renders as literal text. So the published copy navigates and the repo copy does not, from identical bytes. Resolution is tied to the open question in §5.

**Only one test, and it asserts almost nothing.** See §3.3.

**No CI.** Nothing runs `./mvnw test` on push. A minimal GitHub Actions workflow (`setup-java` + `./mvnw -B test`) would catch the class of breakage that produced `21d7a04` — a merge that left conflict markers in a tracked file.

**`spring.jpa.open-in-view` warns on every startup.** Spring asks for it to be set explicitly. Setting `spring.jpa.open-in-view=false` in `application.properties` silences the warning and is the better default for an API — it stops lazy-loading queries from firing during response rendering.

**H2 console dependency present but disabled.** `spring-boot-h2console` is a declared dependency doing nothing until `spring.h2.console.enabled=true` is set. Either enable it (dev only) or drop the dependency.

**`.vscode/launch.json` points at a `.env` file that does not exist.** The `envFile` entry references `${workspaceFolder}/.env`; there is no such file. Harmless today, confusing later. Note that `.vscode/` is gitignored, so this is local-only — anyone cloning fresh gets no launch config at all.

**Local JDK is 21, `pom.xml` targets 17.** Compiling to 17 bytecode on a 21 toolchain works and the build is green, but nothing enforces the runtime version. Worth pinning intent if a second machine or CI joins.

---

## 5. Open questions

Unresolved, and each one changes what gets built:

**Which wiki copy is the source of truth?** Needs deciding before the docs grow further; see the two debt items in §4. Three ways out:

1. **Published wiki wins.** Delete `wiki/` from this repo and point README at the wiki URL. All wikilinks work, one copy to maintain, editable in-browser. Cost: docs stop versioning with the code they describe and never appear in a PR diff.
2. **Repo wins.** Keep `wiki/`, convert the 33 wikilinks to relative Markdown (`[Model Layer](Model-Layer.md)`), and either retire the published wiki or push to it from CI on merge to `main`. Docs get reviewed alongside code.
3. **Keep both by hand.** Workable at six pages, unreliable beyond that.

Leaning **2** — the working rule in [wiki/Service-Layer.md](wiki/Service-Layer.md) is that a page's status line updates in the same commit as the layer it documents, which only holds if the docs live with the code. It also repairs the broken navigation instead of leaving correctness dependent on which copy a reader opens. Note that option 1 would mean re-pointing the links in both README.md and this file.

**Schema management once PostgreSQL arrives.** Hibernate auto-DDL is fine against a throwaway in-memory database and unacceptable against a persistent one. Flyway or Liquibase should be adopted *before* the first real Postgres deployment, not after — retrofitting migrations onto an existing schema is the painful order.

**"Transactional ledger" — how literal?** Append-only entries with corrections modelled as reversing entries is a genuinely different design from mutable rows with an audit trail. It affects the `FoodLog` schema directly, so it should be settled before macros and budgets are added on top.

**Where does nutrition data come from?** Manual entry only, or an external food database? A third-party lookup introduces API keys, caching, and a client layer that none of the current architecture accounts for.

**Is this multi-user?** A `User` entity, and authentication, are large enough to be worth deciding early rather than retrofitting.

---

## 6. Environment notes

- **Repo:** `https://github.com/AxcelT/Big-Back-Behavior` · single branch `main`, no protection observed
- **Wiki repo:** `https://github.com/AxcelT/Big-Back-Behavior.wiki.git` · branch `master`, cloned at `D:\GithubRepos\Big-Back-Behavior.wiki`, in sync with origin. Separate from the main repo — pushing one does nothing to the other.
- **Local:** Windows 11, PowerShell — use `.\mvnw.cmd`; the Bash tool and `./mvnw` also work
- **JDK:** 21.0.9 LTS installed; project targets 17
- **Port:** 8081, not 8080 — set in `application.properties`
- **IDE:** VS Code with the Spring Boot Dashboard. Its Beans / Endpoint Mappings / Properties panels read from Actuator, which is why `e3f0340` added it. Those panels are the fastest way to confirm a new bean or route registered.
- **Offline builds:** the local `~/.m2` cache is populated; `./mvnw -o test` works without a network.
- **`gh` CLI is not authenticated** on this machine, so issue and PR state could not be read. If GitHub issues are being used for planning, they are not reflected in this document.
