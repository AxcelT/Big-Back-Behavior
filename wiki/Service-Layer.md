# Service Layer

**Its one job: hold the business rules.**

The service layer is where decisions live. Not "how do I fetch a row" (that is the [[Repository Layer]]) and not "what URL is this" (that is the [[Controller Layer]]), but *what the application actually means* — what is allowed, what happens when, and what several steps combined add up to.

Status in this repo: ❌ **not built, and deliberately so.** Read the section on when to add it.

---

## What belongs here

Anything that would still be true if this were a desktop app instead of a web API:

- **Validation beyond field types.** "Calories cannot be negative." "A food name cannot be blank."
- **Calculations.** "What is today's total?" "Is this user over their daily budget?"
- **Multi-step operations.** "Save the log, then recalculate the daily total, then check the streak." Either all of it happens or none of it does.
- **Coordinating several repositories.** Anything touching both `FoodLog` and, later, `User` or `DailyBudget`.

## What does not belong here

- HTTP status codes, request parsing, URL paths — [[Controller Layer]]
- SQL and query construction — [[Repository Layer]]
- Field definitions — [[Model Layer]]

---

## Why this project does not have one yet

Right now the intended behaviour of `GET /foodlogs` is, in full: *return every food log*. A service class for that would look like this:

```java
@Service
public class FoodLogService {

    private final FoodLogRepository repository;

    public FoodLogService(FoodLogRepository repository) {
        this.repository = repository;
    }

    public List<FoodLog> findAll() {
        return repository.findAll();   // adds nothing
    }
}
```

That class has no reason to exist. It forwards a call and returns the result. It is a file to open, a name to remember, and a step to trace through, in exchange for nothing.

This pattern has a name — a **pass-through service** — and it is the single most common source of "why does this Java project have so many files?" Tutorials show a service layer because real applications need one, and readers copy it before they have any logic to put in it.

**Add the service layer the first time you write a rule.** Not before. That moment is close — the first `if` statement that is not about HTTP belongs in a service.

---

## What it will look like when it is needed

The moment the project needs "reject negative calories and reject blank names," the layer earns its place:

```java
package com.example.demo.service;

import com.example.demo.model.FoodLog;
import com.example.demo.repository.FoodLogRepository;
import org.springframework.stereotype.Service;

@Service
public class FoodLogService {

    private final FoodLogRepository repository;

    public FoodLogService(FoodLogRepository repository) {
        this.repository = repository;
    }

    public FoodLog create(FoodLog log) {
        if (log.getCalories() != null && log.getCalories() < 0) {
            throw new IllegalArgumentException("Calories cannot be negative");
        }
        if (log.getFoodName() == null || log.getFoodName().isBlank()) {
            throw new IllegalArgumentException("Food name is required");
        }
        return repository.save(log);
    }

    public int totalCalories() {
        return repository.findAll().stream()
                .filter(log -> log.getCalories() != null)
                .mapToInt(FoodLog::getCalories)
                .sum();
    }
}
```

Now there is something here that neither a repository nor a controller should own.

---

## The annotations

### `@Service`

Marks the class as a bean, so Spring creates one instance at startup and can inject it into controllers.

Technically `@Component` would work identically — `@Service` is the same thing with a more specific name. Use `@Service` anyway: it tells a reader what kind of class this is, and tooling groups by it.

### `@Transactional`

Wraps a method in a database transaction. Every write inside either succeeds together or is rolled back together.

```java
@Transactional
public void logMealAndUpdateStreak(FoodLog log) {
    repository.save(log);
    streakRepository.increment(log.getUserId());   // if this throws,
}                                                  // the save is undone
```

For single-statement operations you do not need it — repository writes are already transactional. You need it as soon as one logical operation spans two or more writes.

---

## Why bother, once you do need it

The strongest argument is testability. Business rules in a service can be tested without a web server or a database:

```java
@Test
void rejectsNegativeCalories() {
    FoodLogService service = new FoodLogService(new FakeRepository());
    assertThrows(IllegalArgumentException.class,
            () -> service.create(new FoodLog("Rice", -5)));
}
```

That test runs in milliseconds. The same rule buried in a controller method would require booting the whole application and issuing an HTTP request to reach it.

The second argument is reuse. If a scheduled job, a CLI command, and an HTTP endpoint all need to create a food log, they can all call the same service. Logic living in a controller is reachable only over HTTP.

---

## Rules for this layer

**Do:**
- Create one only when there is a rule to enforce
- Name it `<Entity>Service`
- Depend on repositories, never on controllers
- Keep methods named after the operation in business terms (`logMeal`), not database terms (`insertRow`)

**Do not:**
- Create one per entity out of habit
- Return HTTP types like `ResponseEntity`
- Let it import anything web-related

---

## The honest summary

The layer is real and you will need it. It is also the layer most often added too early. This project will add it in the same commit as its first business rule, and not before.

---

Next: [[Controller Layer]] — the outermost layer, and the one to build after the repository.
