package com.canteen.service;

import com.canteen.entity.User;
import com.canteen.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LdapService ldapService;

    public UserService(UserMapper userMapper, BCryptPasswordEncoder passwordEncoder,
                       LdapService ldapService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.ldapService = ldapService;
    }

    // Returns false if the username is already taken.
    public boolean register(String username, String email, String rawPassword) {
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            return false;
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole("student");
        userMapper.insertUser(user);
        return true;
    }

    // Same as register, but the new account is a vendor pending approval.
    public boolean registerVendor(String username, String email, String rawPassword) {
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            return false;
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole("vendor");
        userMapper.insertUser(user);
        return true;
    }

    // Returns the User on success, or null if login fails.
    public User login(String username, String rawPassword) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return null;
        }

        // Users marked use_ldap are checked by the directory ONLY. If LDAP
        // says no, we stop here and never try the local hash. Falling back
        // would let someone whose password was already changed in LDAP get in
        // with their old local password.
        if (Boolean.TRUE.equals(user.getUseLdap())) {
            if (ldapService.cekSandi(username, rawPassword)) {
                return user;
            }
            return null;
        }

        if (passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    // #7 change your own password. Returns false if the old password is wrong.
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return false;
        }
        // LDAP users have no local password to change: theirs lives in the
        // directory. Without this guard the code below always fails with
        // "old password is wrong", which is misleading — the old password is
        // not wrong, it is simply not stored here.
        if (Boolean.TRUE.equals(user.getUseLdap())) {
            return false;
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return false;
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        return true;
    }

    // ---- superadmin: manage users ----
    public List<User> getAllUsers() {
        return userMapper.findAllUsers();
    }

    // Suspend / activate an account. A superadmin cannot deactivate another superadmin.
    public void setActive(Integer id, boolean active) {
        User target = userMapper.findById(id);
        if (target == null || "superadmin".equals(target.getRole())) {
            return;
        }
        userMapper.setActive(id, active);
    }

    // Reset a user's password to a new value (by the superadmin), without the old password.
    // Returns false if the target doesn't exist, is a superadmin, or the password is empty.
    public boolean resetPassword(Integer id, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) return false;
        User target = userMapper.findById(id);
        if (target == null || "superadmin".equals(target.getRole())) return false;
        userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
        return true;
    }
}
