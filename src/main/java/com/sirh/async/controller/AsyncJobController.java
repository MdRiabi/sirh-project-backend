package com.sirh.async.controller;

import com.sirh.async.dto.AsyncJobDto;
import com.sirh.async.entity.AsyncJobType;
import com.sirh.async.service.AsyncJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<AsyncJobDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(AsyncJobDto.from(asyncJobService.findById(id)));
    }
}