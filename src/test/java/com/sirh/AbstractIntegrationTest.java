package com.sirh;

import com.sirh.common.dto.SingleObjectDto;
import com.sirh.model.dto.LoginRequest;
import com.sirh.model.dto.TokenResponse;
import com.sirh.model.enums.Role;
import com.sirh.model.entity.Utilisateur;
import com.sirh.model.repository.UtilisateurRepository;
import com.sirh.model.entity.Employe;
import com.sirh.model.repository.EmployeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final String FIXTURE_PASSWORD = "Secret123!";

    // Démarrage manuel (pas @Testcontainers/@Container) : constaté en CI comme
    // en local, l'extension JUnit5 arrête ce conteneur entre certaines classes
    // concrètes même si le champ est `static` et partagé via une classe
    // abstraite commune -- symptôme observé : un nouveau conteneur (nouveau
    // port) est recréé avant chaque classe suivante, et le HikariPool de la
    // classe en cours échoue à valider ses connexions juste après ("this
    // connection has been closed"), chaque test attendant le plein timeout
    // HikariCP (30s) avant d'échouer. Un bloc statique classique ne dépend
    // d'aucun hook de cycle de vie de framework et ne démarre qu'une seule
    // fois par classloader — pattern officiellement documenté par
    // Testcontainers pour un singleton partagé entre classes de test
    // (java.testcontainers.org/test_framework_integration/manual_lifecycle_control).
    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?> REDIS;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("sirh_test")
                .withUsername("sirh")
                .withPassword("sirh");
        POSTGRES.start();

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                .withExposedPorts(6379);
        REDIS.start();
    }

    @TempDir
    static Path storageDir;

    @LocalServerPort
    protected int port;

    // Factory par défaut de TestRestTemplate (JDK HttpURLConnection) : bug connu
    // sur les requêtes POST/PUT avec corps + réponse non-2xx en connexion
    // conservée keep-alive -- "cannot retry due to server authentication, in
    // streaming mode" (HttpRetryException). Se manifestait de façon
    // intermittente sur les tests attendant un 401/404/409 après un POST
    // (login, autorisations...). JdkClientHttpRequestFactory (java.net.http,
    // Spring Framework 6.1+, aucune dépendance supplémentaire) n'a pas cette
    // limitation.
    protected final TestRestTemplate restTemplate =
            new TestRestTemplate(new RestTemplateBuilder()
                    .requestFactory((Supplier<ClientHttpRequestFactory>) JdkClientHttpRequestFactory::new));

    @Autowired
    private EmployeRepository employeRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Tables transactionnelles à vider avant chaque test pour garantir
    // l'isolation entre classes IT (résidus jusque-là masqués par le bug de
    // conteneur ci-dessus : chaque classe recevait un conteneur/schéma neuf).
    // Exclues volontairement : employes/departements/utilisateurs (fixtures de
    // test à matricule aléatoire, jamais de collision, pas besoin d'y toucher)
    // et parametres_globaux/pointeuses/shifts (seuils métier et référentiels
    // seedés une fois par Flyway V3/V4/V6-9, aucune migration ne les
    // réinsère — un TRUNCATE ... CASCADE sur employes les viderait aussi via
    // leur FK, d'où leur exclusion complète du nettoyage).
    private static final List<String> TABLES_A_NETTOYER = List.of(
            "async_jobs", "audit_logs", "autorisations", "cloture_paie", "contrats",
            "demandes_deverrouillage", "evaluation_competences", "evaluations", "evenements_360",
            "fichiers", "historique_soldes", "indisponibilites", "jours_feries", "notifications",
            "outillage_mouvements", "outillages", "plannings", "plans_developpement",
            "pointages_bruts", "taches_renouvellement"
    );

    @BeforeEach
    void nettoyerLaBaseEntreLesTests() {
        jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", TABLES_A_NETTOYER) + " RESTART IDENTITY");
    }

    // Déballe l'enveloppe SingleObjectDto<TokenResponse> de POST /auth/login —
    // aide partagée pour éviter à chaque IT de reconstruire l'appel exchange()
    // avec ParameterizedTypeReference.
    protected TokenResponse login(String username, String password) {
        return restTemplate.exchange(
                        baseUrl("/auth/login"), HttpMethod.POST, new HttpEntity<>(new LoginRequest(username, password)),
                        new ParameterizedTypeReference<SingleObjectDto<TokenResponse>>() {})
                .getBody().data();
    }

    protected String createFixtureUser(Role role) {
        return createFixtureUserWithEmploye(role).username();
    }

    protected record FixtureUser(String username, UUID employeId) {}

    protected FixtureUser createFixtureUserWithEmploye(Role role) {
        Employe employe = createFixtureEmploye();

        String username = "it-" + employe.getMatriculeInterne();
        utilisateurRepository.save(Utilisateur.builder()
                .employe(employe)
                .username(username)
                .passwordHash(passwordEncoder.encode(FIXTURE_PASSWORD))
                .role(role)
                .compteActif(true)
                .echecsLoginConsecutifs(0)
                .build());
        return new FixtureUser(username, employe.getId());
    }

    protected Employe createFixtureEmploye() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return employeRepository.save(Employe.builder()
                .matriculeInterne("IT-" + suffix)
                .nom("Test")
                .prenom("User")
                .typeContrat("CDI")
                .dateEmbauche(LocalDate.now())
                .actif(true)
                .build());
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("storage.path", () -> storageDir.toString());
    }

    protected String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
