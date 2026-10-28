package com.example.recruitment.controller;

import com.example.recruitment.entity.User;
import com.example.recruitment.notification.NotificationService;
import com.example.recruitment.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listNotifications(@RequestParam(defaultValue = "0") int page,
                                    Authentication auth,
                                    Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        var pageable = PageRequest.of(page, 15);
        model.addAttribute("notifications", notificationService.getForUser(user.getId(), pageable));
        model.addAttribute("unreadCount", notificationService.countUnread(user.getId()));
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        notificationService.markRead(id, user.getId());
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(Authentication auth, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        notificationService.markAllRead(user.getId());
        redirectAttributes.addFlashAttribute("success", "All notifications marked as read.");
        return "redirect:/notifications";
    }
}
