package com.example.grocery.webApi.responses.employee;

import java.time.LocalDate;

import com.example.grocery.entity.enums.Nationality;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GetByIdEmployeeResponse {

    private Long id;

    private Long userId;

    private Long imageId;

    private String firstName;

    private String lastName;

    private String nationalIdentity;

    private Nationality nationality;

    private LocalDate yearOfBirth;

    private double salary;
}
