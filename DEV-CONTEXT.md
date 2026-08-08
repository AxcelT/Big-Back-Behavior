# Development Context

**Last updated:** 2026-08-08 · **Branch:** `main` (the only branch) · **HEAD:** `7ae30ee` · **Working tree:** uncommitted docs changes, see §1

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
| 2026-08-02 | `7ae30ee` | The `wiki/` architecture docs — since removed from the repo, see below |

Two author identities appear (`AxcelT`, `JuTidalgo`) — same person, different machines. Worth aligning `user.name`/`user.email` across both so history and contribution graphs stay coherent.

### Staged, not yet committed

A docs pass sitting in the index as of 2026-08-08:

- `README.md` rewritten from the two-line stub
- `DEV-CONTEXT.md` added (this file)
- all six `wiki/*.md` deleted, per the decision in §2

The pages are not lost — they live in the wiki repository, and in this repo's history at `7ae30ee`.

---

## 2. Decisions already made

These are settled; don't re-litigate them without a reason.

**Layers get built bottom-up, one at a time.** Model → Repository → Controller. Each layer's wiki page is written before or alongside the code, and its status line is updated as part of the same piece of work.

**The GitHub wiki is the only home for architecture docs** *(decided 2026-08-08)*. The duplicate `wiki/` directory was deleted from this repo. Until then the same six pages existed in two unconnected repositories — no submodule, no symlink, synced by hand — and the pages navigate with `[[Page Name]]` links, which GitHub resolves **only** inside a wiki. In the repo copy all 33 of them rendered as literal text, so identical bytes gave working navigation in one place and dead navigation in the other. One copy removes both the drift risk and the broken links.

The cost, accepted knowingly: docs no longer version alongside the code they describe, never appear in a PR diff, and cannot be updated in the same commit as a layer. A page's status line and the code it describes can now drift apart, and nothing will catch it. Keeping the wiki current is a manual discipline — see §4.

**No service layer until there is a real business rule.** A `FoodLogService` that only calls `repository.findAll()` is a pass-through — a file to open and a step to trace for nothing. It gets created in the same commit as the first non-HTTP `if` statement. Rationale on the [Service Layer](https://github.com/AxcelT/Big-Back-Behavior/wiki/Service-Layer) page.

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

That is the entire file — Spring Data generates the implementation at startup. Verify by restarting and confirming a `foodLogRepository` bean appears in the VS Code Spring Boot Dashboard's Beans panel. Full detail on the [Repository Layer](https://github.com/AxcelT/Big-Back-Behavior/wiki/Repository-Layer) page.

### 3.2 Controller layer

`FoodLogController` with `GET /foodlogs`, `GET /foodlogs/{id}`, `POST /foodlogs`, `DELETE /foodlogs/{id}`, injecting the repository directly. A worked example is already on the [Controller Layer](https://github.com/AxcelT/Big-Back-Behavior/wiki/Controller-Layer) page. This is the commit where the app becomes usable end to end.

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

**The wiki documents a package layout that does not exist.** The [Service Layer](https://github.com/AxcelT/Big-Back-Behavior/wiki/Service-Layer) page imports `com.example.demo.model.FoodLog` and `com.example.demo.repository.FoodLogRepository`, but the real code is flat in `com.example.demo`. Pick one — flat is fine at this size, sub-packages are the convention as it grows — and make the code and the wiki agree. Whichever way, everything must stay under `com.example.demo` or component scanning silently skips it.

**Wiki staleness is now unenforceable.** With the docs in a separate repository (§2), no commit, diff, or test can notice that a page contradicts the code — the item above is exactly that failure, and it predates the split. The only backstop is remembering to push the wiki when a layer lands. If this bites more than once, the fix is a CI job that fails when `src/main/java` changes without a corresponding wiki push, or moving the docs back in-repo with the wikilinks converted to relative Markdown.

**Nothing prevents a `wiki/` directory reappearing in this repo.** `.gitignore` has no entry for it. A stray clone or a restored deletion would be picked up as tracked files and quietly recreate the two-copy problem. Left unguarded for now, deliberately — an ignore rule would also silently swallow a future decision to move the docs back in-repo.

**Only one test, and it asserts almost nothing.** See §3.3.

**No CI.** Nothing runs `./mvnw test` on push. A minimal GitHub Actions workflow (`setup-java` + `./mvnw -B test`) would catch the class of breakage that produced `21d7a04` — a merge that left conflict markers in a tracked file.

**`spring.jpa.open-in-view` warns on every startup.** Spring asks for it to be set explicitly. Setting `spring.jpa.open-in-view=false` in `application.properties` silences the warning and is the better default for an API — it stops lazy-loading queries from firing during response rendering.

**H2 console dependency present but disabled.** `spring-boot-h2console` is a declared dependency doing nothing until `spring.h2.console.enabled=true` is set. Either enable it (dev only) or drop the dependency.

**`.vscode/launch.json` points at a `.env` file that does not exist.** The `envFile` entry references `${workspaceFolder}/.env`; there is no such file. Harmless today, confusing later. Note that `.vscode/` is gitignored, so this is local-only — anyone cloning fresh gets no launch config at all.

**Local JDK is 21, `pom.xml` targets 17.** Compiling to 17 bytecode on a 21 toolchain works and the build is green, but nothing enforces the runtime version. Worth pinning intent if a second machine or CI joins.

---

## 5. Open questions

Unresolved, and each one changes what gets built:

**Schema management once PostgreSQL arrives.** Hibernate auto-DDL is fine against a throwaway in-memory database and unacceptable against a persistent one. Flyway or Liquibase should be adopted *before* the first real Postgres deployment, not after — retrofitting migrations onto an existing schema is the painful order.

**"Transactional ledger" — how literal?** Append-only entries with corrections modelled as reversing entries is a genuinely different design from mutable rows with an audit trail. It affects the `FoodLog` schema directly, so it should be settled before macros and budgets are added on top.

**Where does nutrition data come from?** Manual entry only, or an external food database? A third-party lookup introduces API keys, caching, and a client layer that none of the current architecture accounts for.

**Is this multi-user?** A `User` entity, and authentication, are large enough to be worth deciding early rather than retrofitting.

---

## 6. Environment notes

- **Repo:** `https://github.com/AxcelT/Big-Back-Behavior` · single branch `main`, no protection observed
- **Wiki repo:** `https://github.com/AxcelT/Big-Back-Behavior.wiki.git` · branch `master`, cloned at `D:\GithubRepos\Big-Back-Behavior.wiki`, in sync with origin. The sole home of the architecture docs. Separate from the main repo — pushing one does nothing to the other, and it has no CI or review.
- **Local:** Windows 11, PowerShell — use `.\mvnw.cmd`; the Bash tool and `./mvnw` also work
- **JDK:** 21.0.9 LTS installed; project targets 17
- **Port:** 8081, not 8080 — set in `application.properties`
- **IDE:** VS Code with the Spring Boot Dashboard. Its Beans / Endpoint Mappings / Properties panels read from Actuator, which is why `e3f0340` added it. Those panels are the fastest way to confirm a new bean or route registered.
- **Offline builds:** the local `~/.m2` cache is populated; `./mvnw -o test` works without a network.
- **`gh` CLI is not authenticated** on this machine, so issue and PR state could not be read. If GitHub issues are being used for planning, they are not reflected in this document.
