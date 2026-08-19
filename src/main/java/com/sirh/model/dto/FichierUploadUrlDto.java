package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FichierUploadUrlDto {
  @JsonProperty("reference")
  private UUID reference;

  @JsonProperty("upload_url")
  private String uploadUrl;

  @JsonProperty("expires_at")
  private LocalDateTime expiresAt;
}
