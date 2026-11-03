package com.canteen.entity;

import java.time.LocalDateTime;

// One row of the "vendor_applications" table (a vendor's request to open a stall, reviewed by the superadmin).
public class VendorApplication {
    private Integer id;
    private Integer userId;
    private String stallName;
    private String stallDescription;
    private String ownerName;
    private String phone;
    private String status;   // PENDING / APPROVED / REJECTED
    private String rejectReason;
    private LocalDateTime createdAt;

    public VendorApplication() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getStallName() { return stallName; }
    public void setStallName(String stallName) { this.stallName = stallName; }

    public String getStallDescription() { return stallDescription; }
    public void setStallDescription(String stallDescription) { this.stallDescription = stallDescription; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
