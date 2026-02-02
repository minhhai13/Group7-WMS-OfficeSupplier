package com.group5.wms.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

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
}