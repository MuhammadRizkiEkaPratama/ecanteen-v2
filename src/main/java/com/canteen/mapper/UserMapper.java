package com.canteen.mapper;

import com.canteen.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    int insertUser(User user);

    // ---- superadmin: kelola user ----
    List<User> findAllUsers();

    User findById(@Param("id") Integer id);

    int setActive(@Param("id") Integer id, @Param("isActive") Boolean isActive);

    // Used when superadmin approves a vendor: promote to 'admin' + assign a stall.
    int updateRoleAndStall(@Param("id") Integer id,
                           @Param("role") String role,
                           @Param("stallId") Integer stallId);

    int updatePassword(@Param("id") Integer id, @Param("passwordHash") String passwordHash);
}
