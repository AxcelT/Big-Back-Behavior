# Big Back Behavior — Developer Wiki

A calorie and macro tracking API built with Java 17, Spring Boot 4, and (currently) an in-memory H2 database.

This wiki exists to explain **how the application is organised** so that anyone picking up the codebase knows where a given piece of code belongs and why.

If you are new to Spring Boot, read the pages in order. If you just need a definition, jump to the [[Glossary]].

---

## The four layers

A Spring Boot application is conventionally split into four layers. Each layer has one job and only talks to the layer directly below it.

| Layer | Its one job | Answers the question | Status in this repo |
| --- | --- | --- | --- |
| [[Model Layer]] | Define the data | *What is a food log?* | ✅ `FoodLog.java` |
| [[Repository Layer]] | Read and write the data | *How do I get it out of the database?* | ❌ Not built yet |
| [[Service Layer]] | Apply business rules | *Is this allowed? What does it mean?* | ❌ Deliberately skipped for now |
| [[Controller Layer]] | Speak HTTP | *What URL does the outside world call?* | ❌ Not built yet |

The rule that makes this useful: **dependencies point downward only.**

The controller knows about the service. The service knows about the repository. The repository knows about the model. Nothing ever points back up — a model must never import a controller. Keeping the arrows one-directional is what stops the codebase turning into a knot.

---

## How a request travels

When something calls `GET /foodlogs`, this is the path it takes:

```
HTTP request
    ↓
Embedded Tomcat            (provided by Spring — the web server)
    ↓
DispatcherServlet          (provided by Spring — matches URL to code)
    ↓
Controller                 (your code — reads the request)
    ↓
Service                    (your code — applies rules)   [optional]
    ↓
Repository                 (your code — one interface)
    ↓
Hibernate → H2 database    (provided by Spring — runs the SQL)
```

The response travels back up the same path. Objects returned by the controller are automatically converted to JSON.

Only the middle three boxes are code anyone on this project writes. Everything else comes from the framework.

---

## Current state of the codebase

```
src/main/java/com/example/demo/
├── DemoApplication.java     the entry point
└── FoodLog.java             the model
```

The application boots, connects to H2, and creates a `FOOD_LOG` table from `FoodLog.java`. But there is **no way to reach that table over HTTP yet** — no repository, no controller. Visiting `http://localhost:8081/` returns Spring's default error page because nothing is mapped to it.

Next step: build the [[Repository Layer]], then the [[Controller Layer]]. The [[Service Layer]] can wait until there are real rules to enforce.

---

## Running it

```bash
./mvnw spring-boot:run
```

The app starts on **port 8081** (set in `src/main/resources/application.properties`, not the Spring default of 8080).

Health check, which works today:

```bash
curl http://localhost:8081/actuator/health
```

---

## A note on why the layers exist at all

It is completely possible to write a working API in one file — read the request, run the SQL, return JSON, all in a single method. For a five-endpoint app that is genuinely fine.

The layers earn their keep when:

- **Testing.** You can test business rules without starting a web server or a database, by handing the service a fake repository.
- **Change.** Swapping H2 for PostgreSQL should touch configuration and nothing else. Adding a mobile client should not require touching database code.
- **Finding things.** In a codebase with forty files, "validation lives in the service layer" means you know where to look without searching.

The cost is more files. The benefit shows up later. Do not add a layer before it does something — an empty service class that only forwards calls is noise.

---

## If you are coming from Python / Flask

The concepts map almost one to one:

| Flask | Spring Boot |
| --- | --- |
| SQLAlchemy `db.Model` | `@Entity` (the model) |
| `db.session.query(...)` | a repository interface |
| a plain helper module | a `@Service` class |
| `@app.get("/x")` view function | `@GetMapping("/x")` in a `@RestController` |
| `jsonify(obj)` | just `return obj` |
| `requirements.txt` | `pom.xml` |

The one idea Flask has no equivalent for is **dependency injection** — see [[Glossary]].
