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
        if (!"System Admin".equals(loggedInUser.getRole()) && loggedInUser.getWarehouse() == null) {
            // Chỉ chặn các URL nghiệp vụ kho
            if (relativePath.startsWith("/purchasing/") ||
                    relativePath.startsWith("/sales/") ||
                    relativePath.startsWith("/storekeeper/") ||
                    relativePath.startsWith("/transfer/")){

                // Chuyển hướng về một trang thông báo hoặc trang lỗi 403 kèm lý do
                session.setAttribute("error", "Bạn cần được gán kho để sử dụng chức năng này.");
                httpResponse.sendRedirect(contextPath + "/403");
                return;
            }
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
        } else if (relativePath.startsWith("/purchasing/")) {
            if (!("Purchasing Staff".equals(role)) && !("Purchasing Manager".equals(role))) {
                httpResponse.sendRedirect(contextPath + "/403");
                return;
            }
            // Sub-check: /purchasing/approvals/ is Manager-only
            if (relativePath.startsWith("/purchasing/approvals")) {
                if (!"Purchasing Manager".equals(role)) {
                    httpResponse.sendRedirect(contextPath + "/403");
                    return;
                }
            }
        } else if (relativePath.startsWith("/sales/")) {
            if (!("Sales Staff".equals(role)) && !("Sales Manager".equals(role))) {
                httpResponse.sendRedirect(contextPath + "/403");
                return;
            }
            // Sub-check: /sales/approvals/ is Manager-only
            if (relativePath.startsWith("/sales/approvals")) {
                if (!"Sales Manager".equals(role)) {
                    httpResponse.sendRedirect(contextPath + "/403");
                    return;
                }
            }
        } else if (relativePath.startsWith("/storekeeper/")) {
            if (!"Storekeeper".equals(role)) {
                httpResponse.sendRedirect(contextPath + "/403");
                return;
            }
        } else if (relativePath.startsWith("/reports/")) {
            if (!"Warehouse Manager".equals(role)) {
                httpResponse.sendRedirect(contextPath + "/403");
                return;
            }
        } else if (relativePath.startsWith("/api/")) {
            // Gộp tất cả các Role được quyền dùng API chung
            if (!"Purchasing Staff".equals(role) &&
                    !"Purchasing Manager".equals(role) &&
                    !"Sales Staff".equals(role) &&
                    !"Sales Manager".equals(role) &&
                    !"Warehouse Manager".equals(role) &&
                    !"Storekeeper".equals(role)) {

                httpResponse.sendRedirect(contextPath + "/403");
                return;
            }
        } else if (relativePath.startsWith("/transfer/")) {
            if (!"Warehouse Manager".equals(role) && !"Storekeeper".equals(role)) {
                httpResponse.sendRedirect(contextPath + "/403");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
