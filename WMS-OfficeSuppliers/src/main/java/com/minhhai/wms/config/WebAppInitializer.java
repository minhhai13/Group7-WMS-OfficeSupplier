package com.minhhai.wms.config;

import com.minhhai.wms.filter.SecurityFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import jakarta.servlet.Filter;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        // Cấu hình cho tầng dữ liệu (JPA, DataSource)
        return new Class[] { JpaConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        // Cấu hình cho tầng Web (Thymeleaf, ViewResolver)
        return new Class[] { WebConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        // Tương đương với <url-pattern>/</url-pattern> trong web.xml
        return new String[] { "/" };
    }

    @Override
    protected Filter[] getServletFilters() {
        return new Filter[] { new SecurityFilter() };
    }
}