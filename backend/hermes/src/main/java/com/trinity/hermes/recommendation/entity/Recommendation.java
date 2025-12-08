package com.trinity.hermes.Recommendation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    private Long id;
    private String name;
    private String description;
    private String status;
}
