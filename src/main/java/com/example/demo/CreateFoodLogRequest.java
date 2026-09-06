package com.example.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class CreateFoodLogRequest {

    @NotBlank(message = "Food name cannot be empty")
    private String foodName;

    @Positive(message = "Calories must be greater than zero")
    private Integer calories;


    public CreateFoodLogRequest() {
    }

    public CreateFoodLogRequest(String foodName, Integer calories) {
        this.foodName = foodName;
        this.calories = calories;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }
}
