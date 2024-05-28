package com.cjiga.productsservice.products.exceptions;

import com.cjiga.productsservice.products.enums.ProductsErrors;
import org.springframework.lang.Nullable;

public class ProductException extends  Exception{

    private final ProductsErrors productsErrors;

    @Nullable
    private final String productId;


    public ProductException(ProductsErrors productsErrors, @Nullable String productId) {
        this.productsErrors = productsErrors;
        this.productId = productId;
    }

    public ProductsErrors getProductsErrors() {
        return productsErrors;
    }

    @Nullable
    public String getProductId() {
        return productId;
    }
}
