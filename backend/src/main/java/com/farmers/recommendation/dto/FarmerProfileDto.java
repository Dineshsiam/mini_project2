package com.farmers.recommendation.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerProfileDto {

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Gender is required")
    private String gender;

    @Min(value = 1, message = "Age must be greater than 0")
    private int age;

    @NotBlank(message = "Occupation is required")
    private String occupation;

    @NotBlank(message = "Category is required")
    private String category; // e.g. SC, ST, OBC, General

    @Min(value = 0, message = "Income cannot be negative")
    private double income;

    @Min(value = 0, message = "Land holding cannot be negative")
    private double landHolding; // in acres

    private boolean disability;

    private String education;

    private String keywords; // Optional search query like "drip irrigation" or "goat"
}
