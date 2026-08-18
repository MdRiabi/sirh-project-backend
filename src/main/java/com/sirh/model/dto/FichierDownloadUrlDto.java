package com.sirh.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FichierDownloadUrlDto {
  @JsonProperty("url")
  private String url;

  @JsonProperty("expires_at")
  private LocalDateTime expiresAt;
}
