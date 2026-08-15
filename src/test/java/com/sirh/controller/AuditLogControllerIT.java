package com.sirh.controller;

import com.sirh.AbstractIntegrationTest;
import com.sirh.model.entity.AuditLog;
import com.sirh.model.repository.AuditLogRepository;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// cahier-technique.md §2.2/§4.8 : GET /audit-logs SUPERUSER exclusivement, RH
// retiré (seule exception à "RH ⊂ SUPERUSER" dans tout le projet).
class AuditLogControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String login(String username) {
        return restTemplate.postForEntity(baseUrl("/auth/login"), new LoginRequest(username, FIXTURE_PASSWORD), TokenResponse.class)
                .getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void rechercher_rh_estRefuseAvec403() {
        String token = login(createFixtureUser(Role.ROLE_RH));

        ResponseEntity<Map> reponse = restTemplate.exchange(baseUrl("/audit-logs"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rechercher_superuser_voitLesLogsEtFiltreParAction() {
        auditLogRepository.save(AuditLog.builder().action("CLOTURER_PAIE").tableCible("cloture_paie").build());
        auditLogRepository.save(AuditLog.builder().action("CHECK_OUT_OUTILLAGE").tableCible("outillage_mouvements").build());
        String token = login(createFixtureUser(Role.ROLE_SUPERUSER));

        ResponseEntity<Map> reponseComplete = restTemplate.exchange(baseUrl("/audit-logs"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);
        assertThat(reponseComplete.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) reponseComplete.getBody().get("totalElements")).longValue()).isGreaterThanOrEqualTo(2);

        ResponseEntity<Map> reponseFiltree = restTemplate.exchange(baseUrl("/audit-logs?action=CLOTURER_PAIE"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);
        assertThat(reponseFiltree.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) reponseFiltree.getBody().get("totalElements")).longValue()).isEqualTo(1);
    }
}
