package com.sirh.controller;

import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.NotificationDto;
import com.sirh.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(@AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(notificationService.mesNotifications(principal.getUserId()).stream()
                .map(NotificationDto::from).toList());
    }

    @PutMapping("/{id}/lue")
    public ResponseEntity<NotificationDto> marquerLue(@PathVariable Long id,
                                                        @AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(NotificationDto.from(notificationService.marquerLue(id, principal.getUserId())));
    }
}
