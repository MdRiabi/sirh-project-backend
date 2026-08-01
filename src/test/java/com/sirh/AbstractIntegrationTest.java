package com.sirh;

import com.sirh.auth.entity.Role;
import com.sirh.auth.entity.Utilisateur;
import com.sirh.auth.repository.UtilisateurRepository;
import com.sirh.personnel.entity.Employe;
import com.sirh.personnel.repository.EmployeRepository;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final String FIXTURE_PASSWORD = "Secret123!";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("sirh_test")
            .withUsername("sirh")
            .withPassword("sirh");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @TempDir
    static Path storageDir;

    @LocalServerPort
    protected int port;

    protected final TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private EmployeRepository employeRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    protected String createFixtureUser(Role role) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Employe employe = employeRepository.save(Employe.builder()
                .matriculeInterne("IT-" + suffix)
                .nom("Test")
                .prenom("User")
                .typeContrat("CDI")
                .dateEmbauche(LocalDate.now())
                .actif(true)
                .build());

        String username = "it-" + suffix;
        utilisateurRepository.save(Utilisateur.builder()
                .employe(employe)
                .username(username)
                .passwordHash(passwordEncoder.encode(FIXTURE_PASSWORD))
                .role(role)
                .compteActif(true)
                .echecsLoginConsecutifs(0)
                .build());
        return username;
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
