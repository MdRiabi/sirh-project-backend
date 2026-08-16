package com.sirh.service;

import com.sirh.annotation.Audited;
import com.sirh.model.dto.FichierDto;
import com.sirh.model.enums.EntiteType;
import com.sirh.model.entity.Fichier;
import com.sirh.model.enums.FichierCategorie;
import com.sirh.exception.FichierNotFoundException;
import com.sirh.model.repository.FichierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class FichierService {

    private final FichierRepository fichierRepository;
    private final Path storageRoot;

    public FichierService(FichierRepository fichierRepository, @Value("${storage.path}") String storagePath) {
        this.fichierRepository = fichierRepository;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Transactional
    public FichierDto upload(MultipartFile file, FichierCategorie categorie, EntiteType entiteType,
                           Long entiteId, Long uploadeParId) {
        String extension = extractExtension(file.getOriginalFilename());
        String nomStockage = UUID.randomUUID() + extension;
        String cheminRelatif = categorie.name().toLowerCase() + "/" + nomStockage;

        Path destination = storageRoot.resolve(cheminRelatif).normalize();
        try {
            Files.createDirectories(destination.getParent());
            file.transferTo(destination);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de l'écriture du fichier sur le stockage.", e);
        }

        Fichier fichier = Fichier.builder()
                .nomOriginal(file.getOriginalFilename())
                .nomStockage(nomStockage)
                .cheminRelatif(cheminRelatif)
                .typeMime(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .tailleOctets(file.getSize())
                .categorie(categorie)
                .entiteType(entiteType)
                .entiteId(entiteId)
                .uploadeParId(uploadeParId)
                .build();

        return toDto(fichierRepository.save(fichier));
    }

    @Transactional(readOnly = true)
    public Fichier getFichierByReference(UUID reference) {
        return fichierRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new FichierNotFoundException(reference));
    }

    @Transactional(readOnly = true)
    public Resource loadAsResource(Fichier fichier) {
        Path path = storageRoot.resolve(fichier.getCheminRelatif()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            throw new FichierNotFoundException(fichier.getReference());
        }
        return new FileSystemResource(path);
    }

    @Transactional(readOnly = true)
    public List<FichierDto> listByEntite(EntiteType entiteType, Long entiteId) {
        return fichierRepository.findByEntiteTypeAndEntiteIdAndDeletedAtIsNull(entiteType, entiteId)
                .stream().map(this::toDto).toList();
    }

    @Audited(action = "DELETE_FICHIER", tableCible = "fichiers")
    @Transactional
    public void deleteFichierByReference(UUID reference) {
        Fichier fichier = getFichierByReference(reference);
        Path path = storageRoot.resolve(fichier.getCheminRelatif()).normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de la suppression du fichier sur le stockage.", e);
        }
        fichier.onDelete();
        fichierRepository.save(fichier);
    }

    private FichierDto toDto(Fichier fichier) {
        return new FichierDto(
                fichier.getReference(),
                fichier.getNomOriginal(),
                fichier.getTypeMime(),
                fichier.getTailleOctets(),
                fichier.getCategorie(),
                fichier.getEntiteType(),
                fichier.getEntiteId(),
                fichier.getCreatedAt()
        );
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }
}
