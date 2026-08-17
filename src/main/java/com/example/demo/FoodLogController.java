package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class FoodLogController {

    private final FoodLogRepository repository;
    
    public FoodLogController(FoodLogRepository repository){
        this.repository = repository;
    }

    @PostMapping("/logs")
    public FoodLog addLog(@RequestBody FoodLog newLog) {
        return repository.save(newLog);
    }

    @GetMapping("/logs")
    public List<FoodLog> getAllLogs() {
        return repository.findAll();
    }
}