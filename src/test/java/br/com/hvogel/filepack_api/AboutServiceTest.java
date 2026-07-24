package br.com.hvogel.filepack_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

class AboutServiceTest {

	@Test
	void usesFallbackWhenBuildPropertiesMissing() {
		AboutService service = new AboutService(
				Optional.empty(),
				"HV Softwares",
				"FilePack API",
				"desc",
				"0.0.2-SNAPSHOT");

		AboutInfo about = service.getAbout();

		assertThat(about.autor()).isEqualTo("HV Softwares");
		assertThat(about.versao()).isEqualTo("0.0.2-SNAPSHOT");
		assertThat(about.aplicacao()).isEqualTo("FilePack API");
		assertThat(about.descricao()).isEqualTo("desc");
		assertThat(about.dataHoraAlteracao()).isNotNull();
		assertThat(about.consultadoEm()).isNotNull();
		assertThat(about.formatosSuportados()).containsExactly("zip", "7z");
		assertThat(about.endpoints()).anyMatch(e -> e.contains("/sobre"));
		assertThat(about.javaVersion()).isNotBlank();
		assertThat(about.springBootVersion()).isNotBlank();
	}

	@Test
	void usesBuildPropertiesWhenPresent() {
		java.util.Properties props = new java.util.Properties();
		props.put("group", "br.com.hvogel");
		props.put("artifact", "filepack-api");
		props.put("name", "filepack-api");
		props.put("version", "9.9.9");
		props.put("time", "2026-07-19T18:00:00Z");

		BuildProperties buildProperties = new BuildProperties(props);
		AboutService service = new AboutService(
				Optional.of(buildProperties),
				"HV Softwares",
				"FilePack API",
				"desc",
				"0.0.0");

		AboutInfo about = service.getAbout();
		assertThat(about.versao()).isEqualTo("9.9.9");
		assertThat(about.dataHoraAlteracao()).isEqualTo(java.time.Instant.parse("2026-07-19T18:00:00Z"));
	}
}
