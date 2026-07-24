package br.com.hvogel.filepack_api;

import java.time.Instant;
import java.util.List;

/**
 * Payload JSON do endpoint {@code GET /api/filepack/sobre}.
 */
public record AboutInfo(
		String autor,
		String versao,
		String aplicacao,
		String descricao,
		Instant dataHoraAlteracao,
		Instant consultadoEm,
		String javaVersion,
		String springBootVersion,
		List<String> formatosSuportados,
		List<String> endpoints) {
}
