# Controller Layer

**Its one job: translate HTTP into method calls, and objects back into HTTP.**

The controller is the boundary between the outside world and the application. It is the only layer that knows URLs, HTTP verbs, and status codes exist.

Status in this repo: ❌ **not built yet.** This is why `http://localhost:8081/` currently returns Spring's default error page — no controller means no routes.

---

## A complete controller

```java
package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/foodlogs")
public class FoodLogController {

    private final FoodLogRepository repository;

    public FoodLogController(FoodLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<FoodLog> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public FoodLog getOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodLog create(@RequestBody FoodLog log) {
        return repository.save(log);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
```

Four endpoints, no SQL, no manual JSON handling.

> Once a [[Service Layer]] exists, the controller injects the service instead of the repository. Everything else stays the same.

---

## The annotations

### `@RestController`

Two things at once: it registers the class as a bean so Spring finds it, and it declares that **return values are the response body**, serialized to JSON.

Without the `Rest` part (plain `@Controller`), Spring would interpret a returned `String` as the name of an HTML template to render. `@RestController` is what you want for an API.

### `@RequestMapping("/foodlogs")`

A prefix applied to every method in the class. Each method's own mapping is appended to it, so `@GetMapping("/{id}")` becomes `GET /foodlogs/{id}`.

### `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`

Map an HTTP verb and path to a method. With no argument, the method handles the class-level path exactly.

### `@PathVariable`

Binds a `{placeholder}` in the URL to a method parameter. `GET /foodlogs/7` calls `getOne(7L)`. Spring converts the text `"7"` into a `Long` automatically, and returns `400 Bad Request` if it cannot.

### `@RequestBody`

Takes the JSON body of the request and builds an object from it. Posting `{"foodName":"Rice","calories":200}` produces a `FoodLog` with those values set. This is Jackson working in reverse — it matches JSON keys to setter methods.

### `@ResponseStatus`

Sets the status code for a successful response. Without it, everything returns `200 OK`. Correct REST practice is `201 Created` after a `POST` and `204 No Content` after a `DELETE`.

---

## What happens to the return value

The method returns a `List<FoodLog>` — a plain Java object. Jackson converts it to JSON by calling the getters on each `FoodLog`:

```json
[
  { "id": 1, "foodName": "Rice", "calories": 200 },
  { "id": 2, "foodName": "Egg",  "calories": 78  }
]
```

**The JSON keys come from the getter names**, not the field names. `getFoodName()` produces `"foodName"`. This is why the getters in [[Model Layer]] are not optional.

No `jsonify`, no serializer configuration, no `to_dict()` method. Returning the object is enough.

---

## Trying it out

Start the app, then:

```bash
curl -X POST http://localhost:8081/foodlogs -H "Content-Type: application/json" -d '{"foodName":"Rice","calories":200}'
```

```bash
curl http://localhost:8081/foodlogs
```

The `Content-Type` header matters — without it Spring returns `415 Unsupported Media Type` because it does not know the body is JSON.

Remember the database is in memory: restart the app and the data is gone.

---

## Rules for this layer

**Do:**
- Keep methods short — a few lines each. A controller method that is 40 lines long is holding business logic that belongs in a [[Service Layer]]
- Return the right status codes
- Name it `<Entity>Controller`
- Group related endpoints under one `@RequestMapping`

**Do not:**
- Put validation or calculation here
- Call the database directly once a service exists
- Return raw entities forever — see the note on DTOs below

---

## Common mistakes

| Mistake | What happens |
| --- | --- |
| Using `@Controller` instead of `@RestController` | Spring looks for an HTML template and fails |
| Forgetting `@RequestBody` | The object arrives with every field `null` |
| Forgetting `Content-Type: application/json` | `415 Unsupported Media Type` |
| Two methods mapped to the same verb and path | Startup failure: ambiguous mapping |
| Entity has no getters | Response is `[{},{}]` |
| Controller placed outside the `DemoApplication` package tree | Silently never registered — no error, just a 404 |

That last one deserves emphasis. `@SpringBootApplication` scans its own package **and everything beneath it**. `FoodLogController` must live in `com.example.demo` or a sub-package. Put it somewhere else and Spring will not find it, and nothing will tell you why.

---

## One thing to know for later: DTOs

The example above returns `FoodLog` — the database entity — straight to the client. This is fine now and normal in small projects, but it couples your API's public shape to your database schema. Rename a column and every client breaks. Add a `passwordHash` field and it appears in JSON responses.

The eventual fix is a **DTO** (Data Transfer Object): a separate class describing what the API exposes, with the controller mapping between the two. Worth introducing when the entity grows fields that should not be public, or when the API shape needs to diverge from the table.

Not now. Returning entities is the right call while the model is three fields.

---

## Verifying it works

After adding the file, restart and check the VS Code Spring Boot Dashboard. Under **Endpoint Mappings** you will see `/foodlogs` alongside the framework's built-in routes, and **Beans** will include `foodLogController`.

---

Back to [[Home]] · Glossary of terms: [[Glossary]]
