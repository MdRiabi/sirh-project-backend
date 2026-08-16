package com.sirh.controller;

import com.sirh.common.dto.ListingPageDto;
import com.sirh.common.dto.SingleObjectDto;
import com.sirh.security.SirhUserDetails;
import com.sirh.model.dto.NotificationDto;
import com.sirh.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ListingPageDto<NotificationDto>> list(@AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(ListingPageDto.of(notificationService.mesNotifications(principal.getUserId())));
    }

    @PutMapping("/{reference}/lue")
    public ResponseEntity<SingleObjectDto<NotificationDto>> marquerNotificationLueByReference(@PathVariable UUID reference,
                                                        @AuthenticationPrincipal SirhUserDetails principal) {
        return ResponseEntity.ok(SingleObjectDto.of(notificationService.marquerNotificationLueByReference(reference, principal.getUserId())));
    }
}
