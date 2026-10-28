package com.example.recruitment.controller;

import com.example.recruitment.dto.RegistrationRequest;
import com.example.recruitment.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                             @RequestParam(required = false) String logout,
                             Model model) {
        if (error != null) model.addAttribute("loginError", "Invalid username or password.");
        if (logout != null) model.addAttribute("logoutMessage", "You have been logged out.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("request", new RegistrationRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("request") RegistrationRequest request,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch",
                    "Passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerApplicant(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName());

            redirectAttributes.addFlashAttribute("success",
                    "Account created successfully! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("registration.failed", ex.getMessage());
            return "auth/register";
        }
    }
}
