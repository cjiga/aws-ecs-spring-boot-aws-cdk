package com.cjiga.productsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy
@SpringBootApplication
public class ProductsserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductsserviceApplication.class, args);
	}

}
