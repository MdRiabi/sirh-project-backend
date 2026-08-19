package com.sirh.service;

import com.sirh.annotation.Audited;
import com.sirh.client.FileStorageClient;
import com.sirh.client.FileStorageUploadRequest;
import com.sirh.exception.ConflictException;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.dto.FichierDemandeUploadRequest;
import com.sirh.model.dto.FichierDownloadUrlDto;
import com.sirh.model.dto.FichierDto;
import com.sirh.model.dto.FichierUploadUrlDto;
import com.sirh.model.entity.Fichier;
import com.sirh.model.enums.EntiteType;
import com.sirh.model.enums.FichierCategorie;
import com.sirh.model.enums.FichierStatut;
import com.sirh.model.repository.FichierRepository;
import feign.FeignException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FichierService {

  private final FichierRepository fichierRepository;
  private final FileStorageClient fileStorageClient;
  private final ModelMapper modelMapper;
  private final String bucketDocumentsRh;
  private final String bucketUploads;

  public FichierService(
      FichierRepository fichierRepository,
      FileStorageClient fileStorageClient,
      ModelMapper modelMapper,
      @Value("${storage.buckets.documents-rh}") String bucketDocumentsRh,
      @Value("${storage.buckets.uploads}") String bucketUploads) {
    this.fichierRepository = fichierRepository;
    this.fileStorageClient = fileStorageClient;
    this.modelMapper = modelMapper;
    this.bucketDocumentsRh = bucketDocumentsRh;
    this.bucketUploads = bucketUploads;
  }

  @Transactional
  public FichierUploadUrlDto demanderUpload(
      FichierDemandeUploadRequest request, Long uploadeParId) {
    String bucket = resoudreBucket(request.categorie());
    var uploadUrl =
        fileStorageClient.demanderUpload(new FileStorageUploadRequest(bucket, request.typeMime()));

    Fichier fichier =
        Fichier.builder()
            .nomOriginal(request.nomOriginal())
            .nomStockage(uploadUrl.key())
            .fichierExterneId(uploadUrl.id())
            .typeMime(request.typeMime())
            .tailleOctets(request.tailleOctets())
            .categorie(request.categorie())
            .statut(FichierStatut.EN_ATTENTE)
            .entiteType(request.entiteType())
            .entiteId(request.entiteId())
            .uploadeParId(uploadeParId)
            .build();
    fichier = fichierRepository.save(fichier);

    FichierUploadUrlDto dto = new FichierUploadUrlDto();
    dto.setReference(fichier.getReference());
    dto.setUploadUrl(uploadUrl.uploadUrl());
    dto.setExpiresAt(LocalDateTime.ofInstant(uploadUrl.expiresAt(), ZoneId.systemDefault()));
    return dto;
  }

  @Transactional
  public FichierDto confirmerUpload(UUID reference) {
    Fichier fichier = getFichierByReference(reference);
    if (fichier.getStatut() != FichierStatut.DISPONIBLE) {
      String bucket = resoudreBucket(fichier.getCategorie());
      try {
        var metadata = fileStorageClient.confirmer(fichier.getFichierExterneId(), bucket);
        fichier.setTailleOctets(metadata.sizeBytes());
        fichier.setStatut(FichierStatut.DISPONIBLE);
      } catch (FeignException.NotFound e) {
        fichier.setStatut(FichierStatut.ECHEC);
      }
      fichier = fichierRepository.save(fichier);
    }
    return toDto(fichier);
  }

  @Transactional(readOnly = true)
  public Fichier getFichierByReference(UUID reference) {
    return fichierRepository
        .findByReferenceAndDeletedAtIsNull(reference)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ErrorCode.FICHIER_NOT_FOUND, new Object[] {reference}));
  }

  @Transactional(readOnly = true)
  public FichierDownloadUrlDto genererUrlTelechargement(UUID reference) {
    Fichier fichier = getFichierByReference(reference);
    if (fichier.getStatut() != FichierStatut.DISPONIBLE) {
      throw new ConflictException(
          ErrorCode.FICHIER_INDISPONIBLE, new Object[] {fichier.getStatut()});
    }
    String bucket = resoudreBucket(fichier.getCategorie());
    var url = fileStorageClient.genererUrlTelechargement(fichier.getFichierExterneId(), bucket);

    FichierDownloadUrlDto dto = new FichierDownloadUrlDto();
    dto.setUrl(url.downloadUrl());
    dto.setExpiresAt(LocalDateTime.ofInstant(url.expiresAt(), ZoneId.systemDefault()));
    return dto;
  }

  @Transactional(readOnly = true)
  public List<FichierDto> listByEntite(EntiteType entiteType, Long entiteId) {
    return fichierRepository
        .findByEntiteTypeAndEntiteIdAndDeletedAtIsNull(entiteType, entiteId)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Audited(action = "DELETE_FICHIER", tableCible = "fichiers")
  @Transactional
  public void deleteFichierByReference(UUID reference) {
    Fichier fichier = getFichierByReference(reference);
    fichier.onDelete();
    fichierRepository.save(fichier);
  }

  private String resoudreBucket(FichierCategorie categorie) {
    return switch (categorie) {
      case CONTRAT, DIPLOME, CIN, JUSTIFICATIF, BULLETIN_PAIE -> bucketDocumentsRh;
      case AVATAR, PIECE_JOINTE -> bucketUploads;
    };
  }

  private FichierDto toDto(Fichier fichier) {
    return modelMapper.map(fichier, FichierDto.class);
  }
}
