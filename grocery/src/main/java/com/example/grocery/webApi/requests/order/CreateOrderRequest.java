package com.example.grocery.webApi.requests.order;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

import com.example.grocery.entity.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateOrderRequest {

    @NotNull
    private LocalDateTime createdDate = LocalDateTime.now();

    @DateTimeFormat(pattern = "yyyy-MM-dd-HH-mm-ss:")
    private LocalDateTime deliveredDate;

    // enumlardaki validation nasıldı... :)
    private OrderStatus orderStatus;

    @Positive
    private int paymentId;

    @Positive
    private int customerId;

    // private List<Product> products; değerlendir...
}
