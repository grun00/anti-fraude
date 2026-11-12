package tech.ada.antifraude.analise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnaliseRepository extends JpaRepository<AnaliseEntity, UUID> {
}
