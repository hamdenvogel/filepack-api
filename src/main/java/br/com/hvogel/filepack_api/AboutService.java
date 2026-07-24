package br.com.hvogel.filepack_api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
public class AboutService {

	private final Optional<BuildProperties> buildProperties;
	private final String autor;
	private final String aplicacao;
	private final String descricao;
	private final String fallbackVersion;

	public AboutService(
			Optional<BuildProperties> buildProperties,
			@Value("${filepack.about.autor:HV Softwares}") String autor,
			@Value("${filepack.about.aplicacao:FilePack API}") String aplicacao,
			@Value("${filepack.about.descricao:API para empacotar e descompactar arquivos (ZIP/7z)}") String descricao,
			@Value("${filepack.about.versao:0.0.1-SNAPSHOT}") String fallbackVersion) {
		this.buildProperties = buildProperties;
		this.autor = autor;
		this.aplicacao = aplicacao;
		this.descricao = descricao;
		this.fallbackVersion = fallbackVersion;
	}

	public AboutInfo getAbout() {
		String version = buildProperties.map(BuildProperties::getVersion).orElse(fallbackVersion);
		Instant buildTime = buildProperties.map(BuildProperties::getTime).orElse(Instant.parse("2026-07-19T00:00:00Z"));

		return new AboutInfo(
				autor,
				version,
				aplicacao,
				descricao,
				buildTime,
				Instant.now(),
				System.getProperty("java.version"),
				SpringBootVersion.getVersion(),
				List.of("zip", "7z"),
				List.of(
						"POST /api/filepack",
						"POST /api/filepack/unpack",
						"GET /api/filepack/sobre",
						"GET /actuator/health"));
	}
}
