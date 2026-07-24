package br.com.hvogel.filepack_api;

import java.util.Locale;

/**
 * Formatos de arquivo suportados pela FilePack API.
 *
 * <ul>
 *   <li>{@link #ZIP} — formato ZIP clássico (Zip4j), com AES opcional</li>
 *   <li>{@link #SEVEN_Z} — formato 7z (Apache Commons Compress), com AES-256 opcional</li>
 * </ul>
 *
 * Formatos fora desta lista (ex.: {@code tar.gz}, {@code rar}) não são suportados.
 */
public enum ArchiveFormat {

	ZIP("zip", "application/zip"),
	SEVEN_Z("7z", "application/x-7z-compressed");

	private final String extension;
	private final String mediaType;

	ArchiveFormat(String extension, String mediaType) {
		this.extension = extension;
		this.mediaType = mediaType;
	}

	public String getExtension() {
		return extension;
	}

	public String getMediaType() {
		return mediaType;
	}

	/**
	 * Aceita valores como {@code zip}, {@code ZIP}, {@code 7z}, {@code seven_z}, {@code 7Z}.
	 */
	public static ArchiveFormat from(String value) {
		if (value == null || value.isBlank()) {
			return ZIP;
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
		return switch (normalized) {
			case "zip" -> ZIP;
			case "7z", "seven_z", "sevenz" -> SEVEN_Z;
			default -> throw new IllegalArgumentException(
					"Formato não suportado: '" + value + "'. Use zip ou 7z.");
		};
	}
}
