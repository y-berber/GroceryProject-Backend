package com.example.grocery.webApi.requests.supplier;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DeleteSupplierRequest {

    @Min(value = 1)
    private Long id;
}
