package dz.gam.poste.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base des tests d'intégration : conteneurs RabbitMQ + PostgreSQL PARTAGÉS (singletons,
 * démarrés une seule fois pour toute la suite via le bloc statique, nettoyés à l'arrêt de la
 * JVM par Ryuk). On évite ainsi le démarrage/arrêt répété d'un conteneur RabbitMQ par classe
 * d'IT, qui provoquait des coupures de connexion intermittentes en exécution groupée.
 *
 * <p>{@code disabledWithoutDocker = true} : sans Docker, les IT sont ignorées (le bloc
 * statique n'est alors pas exécuté). Les paramètres spécifiques (délais des mocks) restent
 * déclarés dans chaque classe d'IT via son propre {@code @DynamicPropertySource}.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    static final RabbitMQContainer RABBITMQ;
    static final PostgreSQLContainer<?> POSTGRES;

    static {
        RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withDatabaseName("poste").withUsername("poste").withPassword("poste");
        RABBITMQ.start();
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void infraProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }
}
