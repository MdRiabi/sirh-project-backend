package com.sirh.service;

import com.sirh.model.dto.AsyncJobDto;
import com.sirh.model.entity.AsyncJob;
import com.sirh.model.enums.AsyncJobStatut;
import com.sirh.model.enums.AsyncJobType;
import com.sirh.exception.ErrorCode;
import com.sirh.exception.ResourceNotFoundException;
import com.sirh.model.repository.AsyncJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AsyncJobService {

    private final AsyncJobRepository asyncJobRepository;

    public AsyncJobService(AsyncJobRepository asyncJobRepository) {
        this.asyncJobRepository = asyncJobRepository;
    }

    // Retourne l'entité (pas le DTO) : réutilisé en interne par ImportPointagesService/
    // ReportGenerationService qui ont besoin de la référence du job juste créé.
    @Transactional
    public AsyncJob creer(AsyncJobType type, String nomFichier, Long executeParId) {
        AsyncJob job = AsyncJob.builder()
                .type(type)
                .nomFichier(nomFichier)
                .statut(AsyncJobStatut.PENDING)
                .progression(0)
                .nombreLignesTraitees(0)
                .executeParId(executeParId)
                .build();
        return asyncJobRepository.save(job);
    }

    // Retourne l'entité (pas le DTO) : réutilisé en interne par ReportGenerationService
    // pour accéder au type/statut/resultatUrl bruts du job.
    @Transactional(readOnly = true)
    public AsyncJob getAsyncJobByReference(UUID reference) {
        return asyncJobRepository.findByReferenceAndDeletedAtIsNull(reference)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ASYNC_JOB_NOT_FOUND, new Object[]{reference}));
    }

    @Transactional(readOnly = true)
    public AsyncJobDto getAsyncJobDtoByReference(UUID reference) {
        return toDto(getAsyncJobByReference(reference));
    }

    @Transactional(readOnly = true)
    public List<AsyncJobDto> findByType(AsyncJobType type) {
        return asyncJobRepository.findByTypeAndDeletedAtIsNullOrderByCreatedAtDesc(type).stream().map(this::toDto).toList();
    }

    @Transactional
    public void marquerEnCoursByReference(UUID reference) {
        AsyncJob job = getAsyncJobByReference(reference);
        job.setStatut(AsyncJobStatut.IN_PROGRESS);
        asyncJobRepository.save(job);
    }

    @Transactional
    public void marquerSuccesByReference(UUID reference, int nombreLignesTraitees) {
        AsyncJob job = getAsyncJobByReference(reference);
        job.setStatut(AsyncJobStatut.SUCCESS);
        job.setProgression(100);
        job.setNombreLignesTraitees(nombreLignesTraitees);
        job.setCompletedAt(LocalDateTime.now());
        asyncJobRepository.save(job);
    }

    // Rapports asynchrones (Lot 11, com.sirh.reporting) : pas de "lignes
    // traitées" à compter, juste le lien de téléchargement du fichier généré.
    @Transactional
    public void marquerSuccesAvecResultatByReference(UUID reference, String resultatUrl) {
        AsyncJob job = getAsyncJobByReference(reference);
        job.setStatut(AsyncJobStatut.SUCCESS);
        job.setProgression(100);
        job.setResultatUrl(resultatUrl);
        job.setCompletedAt(LocalDateTime.now());
        asyncJobRepository.save(job);
    }

    @Transactional
    public void marquerEchecByReference(UUID reference, Integer ligneErreur, String messageErreur) {
        AsyncJob job = getAsyncJobByReference(reference);
        job.setStatut(AsyncJobStatut.FAILED);
        job.setLigneErreur(ligneErreur);
        job.setMessageErreur(messageErreur);
        job.setCompletedAt(LocalDateTime.now());
        asyncJobRepository.save(job);
    }

    public AsyncJobDto toDto(AsyncJob job) {
        return new AsyncJobDto(
                job.getReference(),
                job.getType(),
                job.getNomFichier(),
                job.getStatut(),
                job.getProgression(),
                job.getResultatUrl(),
                job.getNombreLignesTraitees(),
                job.getLigneErreur(),
                job.getMessageErreur(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }
}
