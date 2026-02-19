package com.minhhai.wms.filter;

import com.minhhai.wms.entity.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Session-based security filter for role-based access control.
 * Checks HttpSession for "loggedInUser" attribute.
 */
public class SecurityFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String relativePath = path.substring(contextPath.length());

        // Allow public resources
        if (relativePath.startsWith("/static/")
                || relativePath.equals("/login")
                || relativePath.equals("/logout")
                || relativePath.equals("/403")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        User loggedInUser = (session != null) ? (User) session.getAttribute("loggedInUser") : null;

        if (loggedInUser == null) {
            httpResponse.sendRedirect(contextPath + "/login");
            return;
        }

        String role = loggedInUser.getRole();

        // Role-based URL access control
        if (relativePath.startsWith("/admin/")) {
            if (!"System Admin".equals(role)) {
                httpResponse.sendRedirect(contextPath + "/403");
                return;
            }
        } else if (relativePath.startsWith("/warehouse/")) {
            if (!"Warehouse Admin".equals(role)) {
                httpResponse.sendRedirect(contextPath + "/403");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
