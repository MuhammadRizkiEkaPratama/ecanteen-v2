package com.canteen.interceptor;

import com.canteen.entity.User;
import com.canteen.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    public LoginInterceptor(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Pages that require login should not be cached by the browser.
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("loginUser") : null;

        // Not logged in -> go to the login page.
        if (user == null) {
            response.sendRedirect("/login");
            return false;
        }

        // Re-check the account status from the DB so suspended accounts are blocked
        // immediately, without waiting for the session to expire.
        User fresh = userMapper.findById(user.getId());
        if (fresh == null || Boolean.FALSE.equals(fresh.getIsActive())) {
            session.invalidate();
            response.sendRedirect("/login?suspended=1");
            return false;
        }
        // Sync the session if the role changed (e.g. a vendor approved into an admin).
        if (!fresh.getRole().equals(user.getRole())) {
            session.setAttribute("loginUser", fresh);
            user = fresh;
        }

        String uri = request.getRequestURI();
        String role = user.getRole();

        // Check which area is allowed for the role.
        // If they enter the wrong area -> redirect to their own role's home page.

        if (uri.startsWith("/superadmin")) {
            // Superadmin only.
            if (!"superadmin".equals(role)) {
                response.sendRedirect(getHomePage(role));
                return false;
            }
        } else if (uri.startsWith("/admin")) {
            // Admin only (an approved vendor).
            if (!"admin".equals(role)) {
                response.sendRedirect(getHomePage(role));
                return false;
            }
        } else if (uri.equals("/vendor") || uri.startsWith("/vendor/")) {
            // Vendor only (pending, not yet approved).
            if (!"vendor".equals(role)) {
                response.sendRedirect(getHomePage(role));
                return false;
            }
        } else {
            // Customer pages: /menu, /cart, /order, /review, /profile.
            // Admins, superadmins, and pending vendors are not allowed in.
            if (!"student".equals(role)) {
                response.sendRedirect(getHomePage(role));
                return false;
            }
        }

        return true;
    }

    // Home page for each role (used to redirect when entering the wrong area).
    private String getHomePage(String role) {
        if ("superadmin".equals(role)) return "/superadmin";
        if ("admin".equals(role))      return "/admin";
        if ("vendor".equals(role))     return "/vendor";
        return "/menu";
    }
}
