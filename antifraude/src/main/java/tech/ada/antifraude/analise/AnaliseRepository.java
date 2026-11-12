package tech.ada.antifraude.analise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AnaliseRepository extends JpaRepository<AnaliseEntity, UUID> {

    /** Quantas transações da conta já foram analisadas entre "de" e "ate" (a regra de frequência). */
    long countByContaOrigemAndDataHoraBetween(String contaOrigem, LocalDateTime de, LocalDateTime ate);
}
