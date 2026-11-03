package com.canteen.entity;

import java.time.LocalDateTime;

// One row of the "security_logs" table (an audit trail entry: logins, orders, admin actions).
public class SecurityLog {
    private Integer id;
    private String username;
    private String action;   // LOGIN_SUCCESS / LOGIN_FAILURE / ORDER_PLACED
    private String detail;
    private LocalDateTime createdAt;

    public SecurityLog() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
