package com.canteen.service;

import com.canteen.entity.Stall;
import com.canteen.entity.VendorApplication;
import com.canteen.mapper.MenuMapper;
import com.canteen.mapper.UserMapper;
import com.canteen.mapper.VendorMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VendorService {

    private final VendorMapper vendorMapper;
    private final MenuMapper menuMapper;
    private final UserMapper userMapper;

    public VendorService(VendorMapper vendorMapper, MenuMapper menuMapper, UserMapper userMapper) {
        this.vendorMapper = vendorMapper;
        this.menuMapper = menuMapper;
        this.userMapper = userMapper;
    }

    // Vendor submits a stall application (goes to superadmin as PENDING).
    // Returns null on success; non-null = error message.
    public String apply(Integer userId, String stallName, String stallDescription,
                        String ownerName, String phone) {
        VendorApplication existing = vendorMapper.findLatestByUser(userId);
        if (existing != null && "PENDING".equals(existing.getStatus())) {
            return "Your application is already in the queue; please wait for the administrator's approval.";
        }
        if (existing != null && "APPROVED".equals(existing.getStatus())) {
            return "Your stall has already been approved. You'll be redirected to the vendor dashboard automatically.";
        }
        VendorApplication app = new VendorApplication();
        app.setUserId(userId);
        app.setStallName(stallName);
        app.setStallDescription(stallDescription);
        app.setOwnerName(ownerName);
        app.setPhone(phone);
        vendorMapper.insertApplication(app);
        return null;
    }

    public VendorApplication getApplicationForUser(Integer userId) {
        return vendorMapper.findLatestByUser(userId);
    }

    public List<VendorApplication> getPendingApplications() {
        return vendorMapper.findPending();
    }

    // Superadmin approves: create the stall, promote the user to 'admin' with that stall.
    public void approve(Integer applicationId) {
        VendorApplication app = vendorMapper.findById(applicationId);
        if (app == null || !"PENDING".equals(app.getStatus())) {
            return;
        }
        Stall stall = new Stall();
        stall.setName(app.getStallName());
        stall.setDescription(app.getStallDescription());
        stall.setIsActive(true);
        stall.setDiscountPercent(BigDecimal.ZERO);
        menuMapper.insertStall(stall);

        userMapper.updateRoleAndStall(app.getUserId(), "admin", stall.getId());
        vendorMapper.updateStatus(applicationId, "APPROVED");
    }

    public void reject(Integer applicationId, String reason) {
        VendorApplication app = vendorMapper.findById(applicationId);
        if (app == null || !"PENDING".equals(app.getStatus())) {
            return;
        }
        vendorMapper.rejectWithReason(applicationId, reason);
    }
}
