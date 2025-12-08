package com.trinity.hermes.disruptionmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDisruptionRequest {
    private String name;
    private String description;
}
