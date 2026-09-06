package com.example.demo;

public class CreateFoodLogRequest {

    private String foodName;
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
