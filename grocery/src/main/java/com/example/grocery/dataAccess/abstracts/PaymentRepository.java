package com.example.grocery.dataAccess.abstracts;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.grocery.entity.concretes.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
