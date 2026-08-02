# Model Layer

**Its one job: define what a thing *is*.**

Also called the *entity* layer or the *domain* layer. A model class describes a shape of data — its fields, their types, and how it maps to a database table. It contains no HTTP code, no SQL, and ideally no business logic.

Status in this repo: ✅ **built** — `src/main/java/com/example/demo/FoodLog.java`

---

## The class we have

```java
package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class FoodLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String foodName;
    private Integer calories;

    public FoodLog() {
    }

    public FoodLog(String foodName, Integer calories) {
        this.foodName = foodName;
        this.calories = calories;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }
}
```

From these ~50 lines, the framework creates a database table automatically. No SQL is written anywhere in this project.

---

## What each annotation does

An **annotation** (the `@Something` syntax) does nothing on its own. It is a label. Some other library reads it later and reacts. Here, Hibernate is the reader.

### `@Entity`

Marks the class as something that maps to a database table. On startup, Hibernate scans the project, finds this label, and creates a table.

The table name is derived from the class name: `FoodLog` becomes `FOOD_LOG`. Field names convert the same way — `foodName` becomes a `FOOD_NAME` column.

You can override this with `@Table(name = "food_logs")` if you need a specific name.

### `@Id`

Marks the primary key. Every entity must have exactly one.

### `@GeneratedValue(strategy = GenerationType.IDENTITY)`

Tells the database to assign the ID itself, using auto-increment. You never set an ID by hand — you save an object with a `null` id, and the database fills it in.

---

## Why `Long` and `Integer`, not `long` and `int`

This is a small decision with real consequences.

Java has two flavours of number type:

- `long` / `int` — **primitives.** Always hold a value. Cannot be `null`. Default to `0`.
- `Long` / `Integer` — **object wrappers.** Can be `null`.

Entities use the capitalised wrapper types on purpose:

- `Long id` is `null` before an object is saved. That is how you tell "this is new" from "this came from the database." With a primitive `long`, a brand-new object would have `id = 0`, which is indistinguishable from a real record with ID 0.
- `Integer calories` can be `null`, meaning "not recorded." With `int`, an unrecorded value would silently become `0` calories, which is a different and wrong claim.

**Rule of thumb: use wrapper types in entities. Use primitives for local variables and counters.**

---

## Why two constructors

```java
public FoodLog() { }                                    // no arguments
public FoodLog(String foodName, Integer calories) { }   // two arguments
```

Java allows several methods with the same name as long as their parameter types differ. This is called **overloading**, and the compiler picks the right one based on what you pass.

- The **no-argument constructor is required by JPA.** Hibernate creates a blank object and then fills each field in. Without it, the application fails at startup.
- The **two-argument constructor is a convenience** for your own code and tests: `new FoodLog("Rice", 200)`.

If you add a constructor with arguments and forget the empty one, Java stops generating the default, and Hibernate breaks. That error message is confusing the first time you see it — this is the cause.

---

## Why getters and setters

They look like pointless boilerplate. They are not optional here, for two reasons:

1. **Hibernate uses them** to read and write field values when moving data to and from the database.
2. **Jackson uses them** to produce JSON. Your API's field names come from the getter names — `getFoodName()` produces `"foodName"` in the response.

Delete the getters and your API returns `{}` for every record. The fields are `private`, so nothing outside the class can see them any other way.

> **Later:** the [Lombok](https://projectlombok.org/) library can generate all of this from a single `@Data` annotation. Worth adopting eventually. Write it by hand a couple of times first, so you know what is being generated.

You may also wonder why this is not a Java `record`, which exists precisely for short data classes. JPA entities cannot be records: they need mutable fields and a no-argument constructor, and records have neither.

---

## Rules for this layer

**Do:**
- Keep it to fields, constructors, getters, setters, and `equals`/`hashCode`
- Use wrapper types so `null` stays meaningful
- Add validation annotations (`@NotNull`, `@Positive`) when input validation is introduced
- Name the class after the real-world thing, in the singular: `FoodLog`, not `FoodLogs` or `FoodLogData`

**Do not:**
- Put database queries here — that is the [[Repository Layer]]
- Put business rules here — that is the [[Service Layer]]
- Import anything web-related — a model must not know HTTP exists

---

## Common mistakes

| Mistake | What happens |
| --- | --- |
| Forgetting the no-argument constructor | Application fails to start |
| Using `int` instead of `Integer` | Missing values silently become `0` |
| Forgetting `@Id` | Startup error: no identifier specified |
| Deleting getters | API returns empty JSON objects |
| Adding a `@RestController` method here | Layers blur; the class becomes untestable |

---

## Seeing the table it produces

Enable the H2 console:

```properties
spring.h2.console.enabled=true
```

Restart, open `http://localhost:8081/h2-console`, and set the JDBC URL to `jdbc:h2:mem:testdb` (user `sa`, no password). You will find a `FOOD_LOG` table with `ID`, `FOOD_NAME`, and `CALORIES` columns — generated entirely from the Java class.

Note that the database is **in memory**: everything is discarded when the app stops. That is intentional for now.

---

Next: [[Repository Layer]] — how to actually read and write these objects.
