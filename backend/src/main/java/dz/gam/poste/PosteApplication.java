package dz.gam.poste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Point d'entrée du Poste de travail unifié (couche infrastructure). */
@SpringBootApplication
public class PosteApplication {

    public static void main(String[] args) {
        SpringApplication.run(PosteApplication.class, args);
    }
}
