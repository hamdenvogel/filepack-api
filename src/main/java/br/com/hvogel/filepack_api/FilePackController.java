package br.com.hvogel.filepack_api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/filepack")
public class FilePackController {

	private final FilePackService filePackService;
	private final AboutService aboutService;

	public FilePackController(FilePackService filePackService, AboutService aboutService) {
		this.filePackService = filePackService;
		this.aboutService = aboutService;
	}

	/**
	 * Empacota arquivos em ZIP ou 7z.
	 *
	 * @param files   arquivos de entrada
	 * @param password senha (obrigatória se encrypt=true)
	 * @param encrypt  true = com senha (padrão); false = sem senha
	 * @param format   {@code zip} (padrão) ou {@code 7z}
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Resource> createArchive(
			@RequestParam("files") List<MultipartFile> files,
			@RequestParam(value = "password", required = false) String password,
			@RequestParam(value = "encrypt", defaultValue = "true") boolean encrypt,
			@RequestParam(value = "format", defaultValue = "zip") String format) throws IOException {

		ArchiveFormat archiveFormat = ArchiveFormat.from(format);
		ArchiveResourceModel archive = filePackService.createArchiveResource(files, password, encrypt, archiveFormat);
		return toDownloadResponse(archive);
	}

	/**
	 * Descompacta ZIP/7z (com ou sem senha) e devolve um ZIP sem senha com o conteúdo.
	 */
	@PostMapping(value = "/unpack", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/zip")
	public ResponseEntity<Resource> unpackArchive(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "password", required = false) String password) throws IOException {

		ArchiveResourceModel archive = filePackService.unpackArchiveResource(file, password);
		return toDownloadResponse(archive);
	}

	@GetMapping("/sobre")
	public AboutInfo sobre() {
		return aboutService.getAbout();
	}

	private ResponseEntity<Resource> toDownloadResponse(ArchiveResourceModel archive) throws IOException {
		try {
			Path path = archive.getResource().getFile().toPath();
			byte[] bytes = Files.readAllBytes(path);
			ByteArrayResource resource = new ByteArrayResource(bytes);

			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archive.getFilename() + "\"")
					.contentType(MediaType.parseMediaType(archive.getMediaType()))
					.contentLength(bytes.length)
					.body(resource);
		}
		finally {
			archive.cleanup();
		}
	}
}
