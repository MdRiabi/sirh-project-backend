package com.sirh.service;

import com.sirh.model.entity.AsyncJob;
import com.sirh.model.enums.AsyncJobStatut;
import com.sirh.model.enums.AsyncJobType;
import com.sirh.exception.AsyncJobNotFoundException;
import com.sirh.model.repository.AsyncJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AsyncJobService {

    private final AsyncJobRepository asyncJobRepository;

    public AsyncJobService(AsyncJobRepository asyncJobRepository) {
        this.asyncJobRepository = asyncJobRepository;
    }

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

    @Transactional(readOnly = true)
    public AsyncJob findById(Long id) {
        return asyncJobRepository.findById(id).orElseThrow(() -> new AsyncJobNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<AsyncJob> findByType(AsyncJobType type) {
        return asyncJobRepository.findByTypeAndDeletedAtIsNullOrderByCreatedAtDesc(type);
    }

    @Transactional
    public void marquerEnCours(Long jobId) {
        AsyncJob job = findById(jobId);
        job.setStatut(AsyncJobStatut.IN_PROGRESS);
        asyncJobRepository.save(job);
    }

    @Transactional
    public void marquerSucces(Long jobId, int nombreLignesTraitees) {
        AsyncJob job = findById(jobId);
        job.setStatut(AsyncJobStatut.SUCCESS);
        job.setProgression(100);
        job.setNombreLignesTraitees(nombreLignesTraitees);
        job.setCompletedAt(LocalDateTime.now());
        asyncJobRepository.save(job);
    }

    // Rapports asynchrones (Lot 11, com.sirh.reporting) : pas de "lignes
    // traitées" à compter, juste le lien de téléchargement du fichier généré.
    @Transactional
    public void marquerSuccesAvecResultat(Long jobId, String resultatUrl) {
        AsyncJob job = findById(jobId);
        job.setStatut(AsyncJobStatut.SUCCESS);
        job.setProgression(100);
        job.setResultatUrl(resultatUrl);
        job.setCompletedAt(LocalDateTime.now());
        asyncJobRepository.save(job);
    }

    @Transactional
    public void marquerEchec(Long jobId, Integer ligneErreur, String messageErreur) {
        AsyncJob job = findById(jobId);
        job.setStatut(AsyncJobStatut.FAILED);
        job.setLigneErreur(ligneErreur);
        job.setMessageErreur(messageErreur);
        job.setCompletedAt(LocalDateTime.now());
        asyncJobRepository.save(job);
    }
}