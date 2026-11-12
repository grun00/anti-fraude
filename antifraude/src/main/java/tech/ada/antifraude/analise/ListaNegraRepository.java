package tech.ada.antifraude.analise;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ListaNegraRepository extends JpaRepository<ContaBloqueadaEntity, String> {
}
