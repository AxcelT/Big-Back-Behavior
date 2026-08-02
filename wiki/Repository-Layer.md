# Repository Layer

**Its one job: read and write model objects to the database.**

This is the only layer allowed to talk to the database. Everything above it asks the repository for data and never touches SQL.

Status in this repo: ❌ **not built yet.** This is the next thing to write.

---

## The whole thing is one empty interface

```java
package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {
}
```

That is the complete file. No method bodies, no SQL, no implementation class.

This is the single most surprising thing in Spring for newcomers, so it is worth being precise about what happens: **at startup, Spring Data generates an implementation of this interface for you and registers it as a bean.** The generated object knows how to run every query, because the method names describe them.

An `interface` in Java is a contract — a list of method signatures with no code behind them. Normally you write a class that implements it. Here, you do not have to.

### What `<FoodLog, Long>` means

Those are **generics** — type parameters that tell a general-purpose type what it is working with. Read it as: *a repository of `FoodLog` objects whose primary key is a `Long`*.

The `Long` must match the type of the `@Id` field in [[Model Layer]]. If they disagree, the app fails at startup.

---

## What you get for free

Extending `JpaRepository` inherits roughly 20 working methods:

| Method | What it does |
| --- | --- |
| `findAll()` | every row |
| `findById(1L)` | one row by primary key, wrapped in `Optional` |
| `save(foodLog)` | insert if new, update if it has an ID |
| `saveAll(list)` | batch insert |
| `deleteById(1L)` | delete one row |
| `delete(foodLog)` | delete by object |
| `count()` | number of rows |
| `existsById(1L)` | true / false |
| `findAll(Sort.by("calories"))` | sorted results |
| `findAll(Pageable)` | paginated results |

`save()` handling both insert and update is worth noting: it checks whether the ID is `null`. New object, `null` ID, so it inserts and the database assigns one. Existing object with an ID, so it updates.

### About `Optional`

`findById` returns `Optional<FoodLog>`, not `FoodLog`. This is Java's way of saying "there might be nothing here" in the type system, so you cannot forget to handle the missing case:

```java
FoodLog log = repository.findById(1L)
        .orElseThrow(() -> new RuntimeException("Not found"));
```

It exists to prevent `NullPointerException`, the most common runtime error in Java.

---

## Custom queries by naming a method

When the built-in methods are not enough, you declare a method and Spring writes the query from its **name**. Still no body:

```java
public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {

    List<FoodLog> findByFoodName(String foodName);

    List<FoodLog> findByCaloriesGreaterThan(Integer calories);

    List<FoodLog> findByFoodNameContainingIgnoreCase(String fragment);

    long countByCaloriesLessThan(Integer calories);
}
```

Spring parses the name at startup and generates the SQL. The vocabulary includes `findBy`, `countBy`, `deleteBy`, `And`, `Or`, `Between`, `LessThan`, `GreaterThan`, `Like`, `Containing`, `IgnoreCase`, `OrderBy`.

**The field name in the method must match the field name in the entity.** `findByFoodName` works because `FoodLog` has a `foodName` field. Typo it as `findByFoodNam` and the application refuses to start with a clear error — the mistake is caught at boot, not in production.

For anything too complex to express in a method name, write the query explicitly:

```java
@Query("SELECT SUM(f.calories) FROM FoodLog f")
Integer totalCalories();
```

Note that is **JPQL**, not SQL — it queries Java classes and fields (`FoodLog`, `f.calories`), not tables and columns. Add `nativeQuery = true` if you need real SQL.

---

## How other layers get hold of it

You never write `new FoodLogRepository()`. You declare that you need one, as a constructor parameter, and Spring supplies it:

```java
@RestController
public class FoodLogController {

    private final FoodLogRepository repository;

    public FoodLogController(FoodLogRepository repository) {
        this.repository = repository;
    }
}
```

This is **dependency injection**. Nothing in your code calls that constructor — Spring does, at startup, passing in the repository it generated. See [[Glossary]].

`final` means the field is assigned once and never reassigned. It is the conventional style for injected dependencies.

---

## Rules for this layer

**Do:**
- Keep one repository per entity
- Prefer method-name queries over `@Query` — they cannot drift out of sync with the entity
- Return `Optional` for single results and `List` for many
- Name it `<Entity>Repository`

**Do not:**
- Put business rules here — a repository fetches and stores, it does not decide
- Return web-specific types
- Write an implementation class — if you find yourself doing that, the logic belongs in the [[Service Layer]]

---

## Common mistakes

| Mistake | What happens |
| --- | --- |
| `class` instead of `interface` | Spring cannot generate an implementation |
| Wrong ID type in the generics | Startup failure |
| Misspelled field in a method name | Startup failure with a readable message |
| Calling `new FoodLogRepository()` | Will not compile — you cannot instantiate an interface |
| Forgetting to inject it | `NullPointerException` at the first call |

---

## Verifying it works

Once the file exists, restart and check the **Beans** panel in the VS Code Spring Boot Dashboard. A `foodLogRepository` bean will have appeared, even though you wrote no class. That bean is the generated implementation.

---

Next: [[Controller Layer]] — exposing this over HTTP. Or read [[Service Layer]] to understand what sits between them and why we are skipping it for now.
