package com.sirh.controller;

import com.sirh.model.dto.PointeuseDto;
import com.sirh.service.PointeuseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pointeuses")
@PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_SUPERUSER')")
public class PointeuseController {

    private final PointeuseService pointeuseService;

    public PointeuseController(PointeuseService pointeuseService) {
        this.pointeuseService = pointeuseService;
    }

    @GetMapping
    public ResponseEntity<List<PointeuseDto>> list() {
        List<PointeuseDto> dtos = pointeuseService.findAll().stream().map(PointeuseDto::from).toList();
        return ResponseEntity.ok(dtos);
    }
}