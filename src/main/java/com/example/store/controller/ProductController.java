package com.example.store.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
   Note that I am keeping in line with the style of the other controllers.
   From a RESTful naming standard this endpoint should be `/products'.
   Also, I would write a ProductService and not pass entities outside that
   tier of the application but, given that the '/orders' endpoint needs updating
   I will code as per the other controllers.
*/
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Validated
public class ProductController {}
