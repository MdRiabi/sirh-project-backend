package com.sirh.service;

import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.dto.PointeuseDto;
import com.sirh.model.entity.Pointeuse;
import com.sirh.model.repository.PointeuseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointeuseService {

  private final PointeuseRepository pointeuseRepository;
  private final ModelMapper modelMapper;

  @Transactional(readOnly = true)
  public List<PointeuseDto> findAll() {
    return pointeuseRepository.findAll().stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public Optional<Pointeuse> findByAdresseIp(String adresseIp) {
    return pointeuseRepository.findByAdresseIpAndDeletedAtIsNull(adresseIp);
  }

  @Transactional
  public Pointeuse enregistrerSynchroByReference(UUID reference) {
    Pointeuse pointeuse =
        pointeuseRepository
            .findByReferenceAndDeletedAtIsNull(reference)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ErrorCode.POINTEUSE_NOT_FOUND, new Object[] {reference}));
    pointeuse.setStatutEnLigne(true);
    pointeuse.setDerniereSynchro(LocalDateTime.now());
    return pointeuseRepository.save(pointeuse);
  }

  private PointeuseDto toDto(Pointeuse pointeuse) {
    return modelMapper.map(pointeuse, PointeuseDto.class);
  }
}
