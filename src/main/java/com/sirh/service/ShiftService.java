package com.sirh.service;

import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.dto.ShiftDto;
import com.sirh.model.dto.ShiftRequest;
import com.sirh.model.entity.Shift;
import com.sirh.model.repository.ShiftRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftService {

  private final ShiftRepository shiftRepository;
  private final ModelMapper modelMapper;

  @Transactional(readOnly = true)
  public List<ShiftDto> findAll() {
    return shiftRepository.findAll().stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public ShiftDto getShiftByReference(UUID reference) {
    return toDto(getShiftEntityByReference(reference));
  }

  @Transactional
  public ShiftDto create(ShiftRequest request) {
    Shift shift =
        Shift.builder()
            .nom(request.nom())
            .typeRotation(request.typeRotation())
            .heureDebut(request.heureDebut())
            .heureFin(request.heureFin())
            .build();
    return toDto(shiftRepository.save(shift));
  }

  private Shift getShiftEntityByReference(UUID reference) {
    return shiftRepository
        .findByReferenceAndDeletedAtIsNull(reference)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(ErrorCode.SHIFT_NOT_FOUND, new Object[] {reference}));
  }

  private ShiftDto toDto(Shift s) {
    return modelMapper.map(s, ShiftDto.class);
  }
}
