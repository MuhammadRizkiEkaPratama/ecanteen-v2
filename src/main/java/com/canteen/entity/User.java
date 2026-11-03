package com.canteen.entity;

// One row of the "users" table (a login account: student / vendor / admin / superadmin).
public class User {
    private Integer id;
    private String username;
    private String passwordHash;
    private String email;
    private String role;
    private Integer stallId;
    private Boolean isActive;
    // true = password is checked by LDAP, not by the local hash
    private Boolean useLdap;

    public User() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getStallId() { return stallId; }
    public void setStallId(Integer stallId) { this.stallId = stallId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getUseLdap() { return useLdap; }
    public void setUseLdap(Boolean useLdap) { this.useLdap = useLdap; }
}
