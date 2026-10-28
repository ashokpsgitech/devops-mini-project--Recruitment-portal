package com.example.recruitment.service;

import com.example.recruitment.audit.AuditService;
import com.example.recruitment.entity.ApplicantProfile;
import com.example.recruitment.entity.Role;
import com.example.recruitment.entity.User;
import com.example.recruitment.repository.ApplicantProfileRepository;
import com.example.recruitment.repository.RoleRepository;
import com.example.recruitment.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApplicantProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       ApplicantProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * Registers a new user with the specified role.
     * Supports APPLICANT, RECRUITER, and HIRING_MANAGER.
     */
    @Transactional
    public User registerUser(String username, String email, String rawPassword,
                             String firstName, String lastName, String roleName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with email '" + email + "' already exists.");
        }

        String targetRole = (roleName != null && !roleName.isBlank()) ? roleName.trim().toUpperCase() : "APPLICANT";
        if (!targetRole.equals("APPLICANT") && !targetRole.equals("RECRUITER") && !targetRole.equals("HIRING_MANAGER")) {
            targetRole = "APPLICANT";
        }

        final String finalRoleName = targetRole;
        Role role = roleRepository.findByName(finalRoleName)
                .orElseThrow(() -> new IllegalStateException(finalRoleName + " role not seeded."));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.getRoles().add(role);
        user = userRepository.save(user);

        ApplicantProfile profile = new ApplicantProfile();
        profile.setUser(user);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profileRepository.save(profile);

        auditService.log("USER_REGISTERED", "User", user.getId(),
                "New user registered: " + username + " with role " + finalRoleName);
        return user;
    }

    /**
     * Registers a new public applicant account (delegates to registerUser).
     */
    @Transactional
    public User registerApplicant(String username, String email, String rawPassword,
                                   String firstName, String lastName) {
        return registerUser(username, email, rawPassword, firstName, lastName, "APPLICANT");
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}
