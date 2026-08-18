package com.sirh.service;

import com.sirh.annotation.Audited;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FichierService {

  private final FichierRepository fichierRepository;
  private final StoragePort storagePort;
  private final ModelMapper modelMapper;
  private final String bucketDocumentsRh;
  private final String bucketUploads;
  private final Duration dureeValiditeUpload;
  private final Duration dureeValiditeDownload;

  public FichierService(
      FichierRepository fichierRepository,
      StoragePort storagePort,
      ModelMapper modelMapper,
      @Value("${storage.s3.buckets.documents-rh}") String bucketDocumentsRh,
      @Value("${storage.s3.buckets.uploads}") String bucketUploads,
      @Value("${storage.s3.presigned-upload-ttl-minutes}") long uploadTtlMinutes,
      @Value("${storage.s3.presigned-download-ttl-minutes}") long downloadTtlMinutes) {
    this.fichierRepository = fichierRepository;
    this.storagePort = storagePort;
    this.modelMapper = modelMapper;
    this.bucketDocumentsRh = bucketDocumentsRh;
    this.bucketUploads = bucketUploads;
    this.dureeValiditeUpload = Duration.ofMinutes(uploadTtlMinutes);
    this.dureeValiditeDownload = Duration.ofMinutes(downloadTtlMinutes);
  }

  @Transactional
  public FichierUploadUrlDto demanderUpload(
      FichierDemandeUploadRequest request, Long uploadeParId) {
    String bucket = resoudreBucket(request.categorie());
    String extension = extraireExtension(request.nomOriginal());
    String cle = request.categorie().name().toLowerCase() + "/" + UUID.randomUUID() + extension;

    Fichier fichier =
        Fichier.builder()
            .nomOriginal(request.nomOriginal())
            .nomStockage(cle.substring(cle.lastIndexOf('/') + 1))
            .objetCle(cle)
            .bucket(bucket)
            .typeMime(request.typeMime())
            .tailleOctets(request.tailleOctets())
            .categorie(request.categorie())
            .statut(FichierStatut.EN_ATTENTE)
            .entiteType(request.entiteType())
            .entiteId(request.entiteId())
            .uploadeParId(uploadeParId)
            .build();
    fichier = fichierRepository.save(fichier);

    var uploadUrl =
        storagePort.creerUrlUploadPresignee(bucket, cle, request.typeMime(), dureeValiditeUpload);

    FichierUploadUrlDto dto = new FichierUploadUrlDto();
    dto.setReference(fichier.getReference());
    dto.setUploadUrl(uploadUrl.toString());
    dto.setExpiresAt(LocalDateTime.now().plus(dureeValiditeUpload));
    return dto;
  }

  @Transactional
  public FichierDto confirmerUpload(UUID reference) {
    Fichier fichier = getFichierByReference(reference);
    if (fichier.getStatut() != FichierStatut.DISPONIBLE) {
      Optional<Long> tailleReelle =
          storagePort.tailleObjet(fichier.getBucket(), fichier.getObjetCle());
      if (tailleReelle.isPresent()) {
        fichier.setTailleOctets(tailleReelle.get());
        fichier.setStatut(FichierStatut.DISPONIBLE);
      } else {
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
    var url =
        storagePort.creerUrlTelechargementPresignee(
            fichier.getBucket(), fichier.getObjetCle(), dureeValiditeDownload);

    FichierDownloadUrlDto dto = new FichierDownloadUrlDto();
    dto.setUrl(url.toString());
    dto.setExpiresAt(LocalDateTime.now().plus(dureeValiditeDownload));
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

  private String extraireExtension(String nomOriginal) {
    if (nomOriginal == null) {
      return "";
    }
    int dotIndex = nomOriginal.lastIndexOf('.');
    return dotIndex >= 0 ? nomOriginal.substring(dotIndex) : "";
  }
}
