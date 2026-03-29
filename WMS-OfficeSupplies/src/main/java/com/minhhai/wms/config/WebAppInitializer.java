package com.minhhai.wms.config;

import com.minhhai.wms.filter.SecurityFilter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import jakarta.servlet.Filter;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        // Root config: JPA, DataSource
        return new Class[] { JdbcConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        // Servlet config: Thymeleaf, ViewResolver
        return new Class[] { WebConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        // URL mapping (equivalent to <url-pattern>/</url-pattern> in web.xml)
        return new String[] { "/" };
    }

    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);

        return new Filter[] { encodingFilter, new SecurityFilter() };
    }
}