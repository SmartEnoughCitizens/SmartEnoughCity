package com.trinity.hermes.dataanalyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", schema = "external_data")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 200)
    private String lastName;

    @Column(name = "role", length = 50)
    private String role;

    @Column(name = "installed_date")
    private LocalDateTime installedDate;

    @Column(name = "is_active")
    private Boolean isActive;
}