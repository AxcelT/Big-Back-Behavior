# Glossary

Terms that come up constantly in this codebase, in plain language.

---

## Core Spring ideas

### Bean

Any object that Spring creates and manages for you, rather than one you create with `new`. Controllers, services, and repositories are all beans.

Spring keeps them in a container and hands them out where they are needed. The **Beans** panel in the VS Code Spring Boot Dashboard is a live list of every bean in the running application.

An `@Entity` is **not** a bean. Entities are data Spring stores; beans are components Spring runs. There is one `FoodLogRepository` bean and potentially thousands of `FoodLog` objects.

### Dependency injection (DI)

The practice of *declaring what you need* instead of *creating it yourself*.

```java
public FoodLogController(FoodLogRepository repository) {
    this.repository = repository;
}
```

Nothing in the codebase calls that constructor. Spring does, at startup, passing in the repository bean it created. The class states its requirement; the framework satisfies it.

This is the one concept with no equivalent in a typical Flask or scripting codebase, where you would create an object at module level and import it wherever needed.

Why it is worth the indirection: you can hand a class a fake dependency in tests, and swapping an implementation touches one place instead of every call site.

### IoC container

"Inversion of Control container" — the machinery that holds the beans and performs the injection. When people say "the Spring container," this is it. You rarely interact with it directly.

### Annotation

The `@Something` syntax. **An annotation does nothing on its own.** It is a label attached to a class, method, or field. Some other code reads it later and reacts.

This is genuinely different from a Python decorator, which is a function that runs and replaces the thing beneath it. `@Entity` executes no code — Hibernate scans for it at startup and acts on what it finds.

### Component scanning

At startup, Spring walks through `DemoApplication`'s package **and every package beneath it**, looking for classes labelled `@RestController`, `@Service`, `@Component`, and similar, and turns them into beans.

The practical consequence: **your classes must live under `com.example.demo`.** A controller in an unrelated package is never registered, produces no error, and quietly 404s.

---

## Persistence

### JPA

*Jakarta Persistence API.* A specification — a set of interfaces and annotations (`@Entity`, `@Id`) that describe how Java objects map to database tables. JPA itself is only a contract; it does not run anything.

### Hibernate

The library that actually implements JPA. It generates the SQL, creates the tables, and moves data back and forth. Spring Boot includes it by default, which is why Hibernate appears throughout your startup logs even though nothing imports it directly.

### ORM

*Object-Relational Mapper.* The general category Hibernate belongs to: software that translates between objects in memory and rows in a relational database, so you write Java instead of SQL. SQLAlchemy is the Python equivalent.

### Entity

A class annotated `@Entity`, mapping to one database table. One instance equals one row. See [[Model Layer]].

### Repository

An interface extending `JpaRepository` that Spring implements for you at startup. The only layer permitted to touch the database. See [[Repository Layer]].

### JPQL

The query language used in `@Query` annotations. It looks like SQL but operates on Java class and field names rather than tables and columns:

```java
@Query("SELECT SUM(f.calories) FROM FoodLog f")
```

`FoodLog` is the class, `f.calories` the field. Hibernate translates it into real SQL for whichever database is in use, which is what makes swapping databases cheap.

### Transaction

A group of database operations that either all succeed or all get undone. Marked with `@Transactional`. Repository writes are individually transactional already; you need the annotation when one logical operation spans several writes.

### H2

A lightweight database that runs **inside** your application's memory. No installation, no separate process. Everything is discarded on shutdown, which makes it ideal for learning and testing and unsuitable for production.

The eventual production target for this project is PostgreSQL. In principle that swap is a configuration change, because JPA sits in between.

---

## Web

### Embedded Tomcat

The web server, running as a library inside the application rather than as separate software you install and deploy into. Started by `main()`. It is why `./mvnw spring-boot:run` gives you a working server with no setup.

### DispatcherServlet

Spring's front door. Every HTTP request hits it first, and it decides which controller method to call based on the URL and verb. Roughly, the routing table.

It is built lazily — you will see `Initializing Spring DispatcherServlet` in the logs on the **first** request, not at startup.

### Controller

The class holding the endpoints. See [[Controller Layer]].

### Endpoint

One URL plus verb combination that the application answers, such as `GET /foodlogs`.

### Jackson

The library that converts Java objects to JSON and back, automatically. It finds properties by looking for getter methods, so `getFoodName()` produces a `"foodName"` key. Also works in reverse for `@RequestBody`.

### Serialization

Converting an in-memory object into a transmittable format — here, JSON. Deserialization is the reverse.

### DTO

*Data Transfer Object.* A class describing the shape your API exposes, kept separate from the database entity so the two can change independently. Not used in this project yet — see the note at the end of [[Controller Layer]].

---

## Build and tooling

### Maven

The build tool. Downloads dependencies, compiles, runs tests, packages the app. Roughly pip and a build backend combined.

### POM

*Project Object Model* — the `pom.xml` file. Declares the project's identity, its Java version, and its dependencies. Comparable to `pyproject.toml`.

### Maven wrapper (`mvnw`)

The scripts and `.mvn/` folder committed alongside the code. They download and use a pinned Maven version, so everyone builds with the same tool without installing anything. Always use `./mvnw` rather than a globally installed `mvn`.

### Starter

A dependency that pulls in a coherent set of libraries for one job. `spring-boot-starter-data-jpa` brings Hibernate, connection pooling, transaction management, and the Spring Data layer as one entry.

> **Note for Spring Boot 4:** the starters were renamed. What most tutorials call `spring-boot-starter-web` is `spring-boot-starter-webmvc` here, and the H2 console moved into its own `spring-boot-h2console` module. If a guide says a dependency is missing, this is usually why.

### Actuator

An optional dependency exposing operational endpoints about the running app — health, metrics, configuration, bean list. Only `/actuator/health` is reachable over HTTP by default, on purpose.

The VS Code Spring Boot Dashboard reads its Beans, Endpoint Mappings, Properties, and Memory panels from it.

Never expose all actuator endpoints publicly: `/actuator/env` reveals configuration including credentials, and `/actuator/heapdump` hands out a snapshot of application memory.

### `application.properties`

Configuration for the running application: port, database settings, logging levels. Lives in `src/main/resources/`. This project sets `server.port=8081`.

---

## Java language terms

### Interface

A contract — a list of method signatures with no implementations. Normally a class implements it. Spring Data is unusual in generating the implementation for you. Closest Python analogues are `Protocol` and abstract base classes.

### Generics

Type parameters in angle brackets. `JpaRepository<FoodLog, Long>` means *a repository of `FoodLog` with a `Long` key*; `List<FoodLog>` is a list containing `FoodLog` objects. The compiler enforces it, so a type mismatch is a build error rather than a runtime surprise.

### Overloading

Several methods sharing a name but differing in parameter types. `FoodLog` has two constructors this way. Java picks the match based on the arguments you pass. Python has no equivalent — you would use default arguments instead.

### `final`

Applied to a field, it means "assign once, never reassign." The conventional style for injected dependencies.

### Wrapper types

`Long` and `Integer` are objects and can be `null`. `long` and `int` are primitives and cannot. Entities use the wrappers so that "not set" stays distinguishable from zero. See [[Model Layer]].

### `Optional`

A container that either holds a value or is explicitly empty. `findById` returns `Optional<FoodLog>` so the "not found" case cannot be silently ignored. It exists to prevent `NullPointerException`.

### `NullPointerException`

The error from calling a method on a `null` reference. The most common runtime failure in Java.

---

## Where terms are used in depth

- [[Model Layer]] — entities, annotations, wrapper types
- [[Repository Layer]] — interfaces, generics, derived queries
- [[Service Layer]] — transactions, business rules
- [[Controller Layer]] — HTTP mapping, serialization
- [[Home]] — how the layers fit together
