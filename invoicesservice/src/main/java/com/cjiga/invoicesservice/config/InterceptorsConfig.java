package com.cjiga.invoicesservice.config;

import com.cjiga.invoicesservice.invoices.interceptors.InvoicesInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorsConfig implements WebMvcConfigurer {

    private final InvoicesInterceptor invoicesInterceptor;

    @Autowired
    public InterceptorsConfig(InvoicesInterceptor invoicesInterceptor) {
        this.invoicesInterceptor = invoicesInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.invoicesInterceptor).addPathPatterns("/api/invoices/**");
    }
}
