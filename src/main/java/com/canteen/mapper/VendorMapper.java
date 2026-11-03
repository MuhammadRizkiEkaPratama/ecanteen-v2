package com.canteen.mapper;

import com.canteen.entity.VendorApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VendorMapper {

    int insertApplication(VendorApplication app);

    // Latest application for one vendor (to show their current status).
    VendorApplication findLatestByUser(@Param("userId") Integer userId);

    // All applications still waiting for the superadmin.
    List<VendorApplication> findPending();

    VendorApplication findById(@Param("id") Integer id);

    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    int rejectWithReason(@Param("id") Integer id, @Param("reason") String reason);
}
