package com.minhhai.wms.filter;

import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.AuthServiceImpl;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Chặn truy cập trái phép: yêu cầu đăng nhập và kiểm tra role cho /admin/**.
 */
public class SecurityFilter implements Filter {

    private static final String LOGIN_PATH = "/login";
    private static final String DASHBOARD_PATH = "/dashboard";
    private static final String ADMIN_PREFIX = "/admin/";
    private static final String WAREHOUSES_PREFIX = "/warehouses";
    private static final String STATIC_PREFIX = "/static/";
    private static final String LOGOUT_PATH = "/logout";
    private static final String SYSTEM_ADMIN_ROLE = "System Admin";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getServletPath() != null ? req.getServletPath() : req.getRequestURI();

        if (path.startsWith(STATIC_PREFIX) || LOGIN_PATH.equals(path) || LOGOUT_PATH.equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute(AuthServiceImpl.SESSION_USER) : null;

        if (currentUser == null) {
            resp.sendRedirect(req.getContextPath() + LOGIN_PATH);
            return;
        }

        boolean requiresSystemAdmin = path.startsWith(ADMIN_PREFIX) || path.startsWith(WAREHOUSES_PREFIX);
        if (requiresSystemAdmin && !SYSTEM_ADMIN_ROLE.equals(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + DASHBOARD_PATH);
            return;
        }

        chain.doFilter(request, response);
    }
}
