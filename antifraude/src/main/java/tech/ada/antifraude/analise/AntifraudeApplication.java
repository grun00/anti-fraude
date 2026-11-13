package tech.ada.antifraude.analise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AntifraudeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AntifraudeApplication.class, args);
    }
}
