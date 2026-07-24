package br.com.hvogel.filepack_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import net.lingala.zip4j.ZipFile;

class FilePackServiceTest {

	private FilePackService service;

	@BeforeEach
	void setUp() {
		service = new FilePackService();
	}

	@Test
	void createEncryptedZipAndUnpack() throws Exception {
		MultipartFile file = textFile("nota.txt", "conteudo secreto");

		ArchiveResourceModel packed = service.createArchiveResource(
				List.of(file), "senha123", true, ArchiveFormat.ZIP);
		try {
			assertThat(packed.getFilename()).endsWith(".zip");
			assertThat(packed.getMediaType()).isEqualTo("application/zip");

			File zip = packed.getResource().getFile();
			try (ZipFile z = new ZipFile(zip, "senha123".toCharArray())) {
				assertThat(z.isEncrypted()).isTrue();
			}

			MockMultipartFile uploaded = new MockMultipartFile(
					"file", "pack.zip", "application/zip", Files.readAllBytes(zip.toPath()));
			ArchiveResourceModel unpacked = service.unpackArchiveResource(uploaded, "senha123");
			try {
				assertThat(unpacked.getFilename()).endsWith(".zip");
				try (ZipFile plain = new ZipFile(unpacked.getResource().getFile())) {
					assertThat(plain.isEncrypted()).isFalse();
					assertThat(plain.getFileHeaders()).isNotEmpty();
				}
			}
			finally {
				unpacked.cleanup();
			}
		}
		finally {
			packed.cleanup();
		}
	}

	@Test
	void createPlainZipWithoutPassword() throws Exception {
		MultipartFile file = textFile("dados.csv", "a,b,c");
		ArchiveResourceModel packed = service.createArchiveResource(
				List.of(file), null, false, ArchiveFormat.ZIP);
		try {
			try (ZipFile z = new ZipFile(packed.getResource().getFile())) {
				assertThat(z.isEncrypted()).isFalse();
			}
		}
		finally {
			packed.cleanup();
		}
	}

	@Test
	void createAndUnpackSevenZEncrypted() throws Exception {
		MultipartFile file = textFile("relatorio.txt", "relatorio 7z");
		ArchiveResourceModel packed = service.createArchiveResource(
				List.of(file), "segredo", true, ArchiveFormat.SEVEN_Z);
		try {
			assertThat(packed.getFilename()).endsWith(".7z");
			assertThat(packed.getMediaType()).isEqualTo("application/x-7z-compressed");

			MockMultipartFile uploaded = new MockMultipartFile(
					"file", "pack.7z", "application/x-7z-compressed",
					Files.readAllBytes(packed.getResource().getFile().toPath()));

			ArchiveResourceModel unpacked = service.unpackArchiveResource(uploaded, "segredo");
			try {
				assertThat(unpacked.getFilename()).endsWith(".zip");
				try (ZipFile plain = new ZipFile(unpacked.getResource().getFile())) {
					assertThat(plain.getFileHeaders()).isNotEmpty();
				}
			}
			finally {
				unpacked.cleanup();
			}
		}
		finally {
			packed.cleanup();
		}
	}

	@Test
	void createPlainSevenZ() throws Exception {
		MultipartFile file = textFile("plain.txt", "sem senha");
		ArchiveResourceModel packed = service.createArchiveResource(
				List.of(file), null, false, ArchiveFormat.SEVEN_Z);
		try {
			assertThat(packed.getFilename()).endsWith(".7z");
			assertThat(packed.getSize()).isPositive();

			MockMultipartFile uploaded = new MockMultipartFile(
					"file", "pack.7z", "application/x-7z-compressed",
					Files.readAllBytes(packed.getResource().getFile().toPath()));
			ArchiveResourceModel unpacked = service.unpackArchiveResource(uploaded, null);
			try {
				assertThat(unpacked.getSize()).isPositive();
			}
			finally {
				unpacked.cleanup();
			}
		}
		finally {
			packed.cleanup();
		}
	}

	@Test
	void encryptTrueRequiresPassword() {
		assertThatThrownBy(() -> service.createArchiveResource(
				List.of(textFile("a.txt", "x")), null, true, ArchiveFormat.ZIP))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("password");
	}

	@Test
	void emptyFilesRejected() {
		assertThatThrownBy(() -> service.createArchiveResource(List.of(), "x", true, ArchiveFormat.ZIP))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void unpackRequiresFile() {
		assertThatThrownBy(() -> service.unpackArchiveResource(null, "x"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void unpackEncryptedZipWithoutPasswordFails() throws Exception {
		ArchiveResourceModel packed = service.createArchiveResource(
				List.of(textFile("a.txt", "x")), "pwd", true, ArchiveFormat.ZIP);
		try {
			MockMultipartFile uploaded = new MockMultipartFile(
					"file", "a.zip", "application/zip",
					Files.readAllBytes(packed.getResource().getFile().toPath()));
			assertThatThrownBy(() -> service.unpackArchiveResource(uploaded, null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("senha");
		}
		finally {
			packed.cleanup();
		}
	}

	@Test
	void unpackUnsupportedFormatFails() {
		MockMultipartFile uploaded = new MockMultipartFile(
				"file", "doc.rar", "application/octet-stream", "Rar!".getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(() -> service.unpackArchiveResource(uploaded, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Formato");
	}

	private static MultipartFile textFile(String name, String content) {
		return new MockMultipartFile("files", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
	}
}
