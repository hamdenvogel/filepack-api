package br.com.hvogel.filepack_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;

class ArchiveResourceModelTest {

	@Test
	void gettersAndCleanupDeleteTempFiles() throws Exception {
		File temp = Files.createTempFile("model-", ".tmp").toFile();
		Files.writeString(temp.toPath(), "conteudo");

		ArchiveResourceModel model = new ArchiveResourceModel(
				new FileSystemResource(temp),
				"pack.zip",
				temp.length(),
				"application/zip",
				List.of(temp));

		assertThat(model.getFilename()).isEqualTo("pack.zip");
		assertThat(model.getSize()).isEqualTo(temp.length());
		assertThat(model.getMediaType()).isEqualTo("application/zip");
		assertThat(model.getResource().exists()).isTrue();

		model.cleanup();
		assertThat(temp).doesNotExist();
	}
}
