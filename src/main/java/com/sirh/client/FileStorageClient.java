package com.sirh.client;

import com.sirh.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// Client du service technique s3-file-system (repo séparé, cf.
// docs/LOTS/LOT_12_FICHIERS_CLOUD.md) — 4 opérations, miroir exact de son
// contrat REST. Le service est sans état : le bucket doit être refourni à
// chaque appel (confirmer/génererUrlTelechargement/supprimer), pas seulement
// à la création.
@FeignClient(
    name = "file-storage",
    url = "${filestorage.api.url}",
    configuration = FeignClientConfig.class)
public interface FileStorageClient {

  @PostMapping("/files/upload-requests")
  FileStorageUploadUrl demanderUpload(@RequestBody FileStorageUploadRequest request);

  @PostMapping("/files/{id}/confirmations")
  FileStorageMetadata confirmer(@PathVariable("id") String id, @RequestParam String bucket);

  @GetMapping("/files/{id}/download-url")
  FileStorageDownloadUrl genererUrlTelechargement(
      @PathVariable("id") String id, @RequestParam String bucket);

  @DeleteMapping("/files/{id}")
  void supprimer(@PathVariable("id") String id, @RequestParam String bucket);
}
