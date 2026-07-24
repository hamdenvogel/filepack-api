package br.com.hvogel.filepack_api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.core.io.Resource;

/**
 * Encapsula um arquivo de arquivo (ZIP/7z) e gerencia a limpeza dos temporários.
 */
public class ArchiveResourceModel {

	private final Resource resource;
	private final String filename;
	private final long size;
	private final String mediaType;
	private final List<File> tempFilesToCleanup;

	public ArchiveResourceModel(Resource resource, String filename, long size, String mediaType,
			List<File> tempFilesToCleanup) {
		this.resource = resource;
		this.filename = filename;
		this.size = size;
		this.mediaType = mediaType;
		this.tempFilesToCleanup = tempFilesToCleanup;
	}

	public Resource getResource() {
		return resource;
	}

	public String getFilename() {
		return filename;
	}

	public long getSize() {
		return size;
	}

	public String getMediaType() {
		return mediaType;
	}

	public void cleanup() {
		for (File tempFile : tempFilesToCleanup) {
			try {
				Files.deleteIfExists(tempFile.toPath());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
