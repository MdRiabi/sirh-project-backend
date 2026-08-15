package com.sirh.controller;

import com.sirh.model.dto.AsyncJobDto;
import com.sirh.model.enums.AsyncJobType;
import com.sirh.service.AsyncJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/async-jobs")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class AsyncJobController {

    private final AsyncJobService asyncJobService;

    public AsyncJobController(AsyncJobService asyncJobService) {
        this.asyncJobService = asyncJobService;
    }

    @GetMapping
    public ResponseEntity<List<AsyncJobDto>> list(@RequestParam AsyncJobType type) {
        List<AsyncJobDto> dtos = asyncJobService.findByType(type).stream().map(AsyncJobDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{reference}")
    public ResponseEntity<AsyncJobDto> getAsyncJobByReference(@PathVariable UUID reference) {
        return ResponseEntity.ok(AsyncJobDto.from(asyncJobService.getAsyncJobByReference(reference)));
    }
}