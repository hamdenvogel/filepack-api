package br.com.hvogel.filepack_api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

@Service
public class FilePackService {

	public ArchiveResourceModel createArchiveResource(List<MultipartFile> multipartFiles, String password,
			boolean encrypt, ArchiveFormat format) throws IOException {
		validatePackRequest(multipartFiles, password, encrypt);

		List<File> cleanup = new ArrayList<>();
		try {
			List<NamedTempFile> namedFiles = toNamedTempFiles(multipartFiles, cleanup);
			File archive = switch (format) {
				case ZIP -> createZip(namedFiles, password, encrypt);
				case SEVEN_Z -> createSevenZ(namedFiles, password, encrypt);
			};
			cleanup.add(archive);

			String downloadName = "filepack-" + UUID.randomUUID() + "." + format.getExtension();
			Resource resource = new FileSystemResource(archive);
			return new ArchiveResourceModel(resource, downloadName, archive.length(), format.getMediaType(),
					new ArrayList<>(cleanup));
		}
		catch (IOException | RuntimeException e) {
			cleanupFiles(cleanup);
			if (e instanceof IOException io) {
				throw io;
			}
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * Extrai um ZIP ou 7z (com ou sem senha) e devolve um ZIP sem senha com o conteúdo.
	 */
	public ArchiveResourceModel unpackArchiveResource(MultipartFile archiveFile, String password) throws IOException {
		if (archiveFile == null || archiveFile.isEmpty()) {
			throw new IllegalArgumentException("Arquivo de arquivo (ZIP/7z) é obrigatório.");
		}

		List<File> cleanup = new ArrayList<>();
		try {
			String originalName = archiveFile.getOriginalFilename() != null ? archiveFile.getOriginalFilename() : "archive";
			File uploaded = File.createTempFile("unpack-src-", "-" + sanitize(originalName));
			archiveFile.transferTo(uploaded);
			cleanup.add(uploaded);

			File extractDir = Files.createTempDirectory("unpack-out-").toFile();
			cleanup.add(extractDir);

			ArchiveFormat detected = detectFormat(originalName, uploaded);
			List<File> extracted = switch (detected) {
				case ZIP -> extractZip(uploaded, password, extractDir, cleanup);
				case SEVEN_Z -> extractSevenZ(uploaded, password, extractDir, cleanup);
			};

			if (extracted.isEmpty()) {
				throw new IllegalArgumentException("O arquivo não contém entradas extraíveis.");
			}

			List<NamedTempFile> named = extracted.stream()
					.map(f -> new NamedTempFile(f, f.getName()))
					.toList();
			File plainZip = createZip(named, null, false);
			cleanup.add(plainZip);

			String downloadName = "unpacked-" + UUID.randomUUID() + ".zip";
			Resource resource = new FileSystemResource(plainZip);
			return new ArchiveResourceModel(resource, downloadName, plainZip.length(), ArchiveFormat.ZIP.getMediaType(),
					new ArrayList<>(cleanup));
		}
		catch (ZipException e) {
			cleanupFiles(cleanup);
			throw new IllegalArgumentException("Falha ao descompactar ZIP: verifique a senha e o arquivo. (" + e.getMessage() + ")", e);
		}
		catch (IOException | RuntimeException e) {
			cleanupFiles(cleanup);
			if (e instanceof IllegalArgumentException iae) {
				throw iae;
			}
			if (e instanceof IOException io) {
				throw io;
			}
			throw new IOException(e.getMessage(), e);
		}
	}

	private void validatePackRequest(List<MultipartFile> multipartFiles, String password, boolean encrypt) {
		if (multipartFiles == null || multipartFiles.isEmpty()
				|| multipartFiles.stream().allMatch(f -> f == null || f.isEmpty())) {
			throw new IllegalArgumentException("Envie ao menos um arquivo em 'files'.");
		}
		if (encrypt && !StringUtils.hasText(password)) {
			throw new IllegalArgumentException("Parâmetro 'password' é obrigatório quando encrypt=true.");
		}
	}

	private List<NamedTempFile> toNamedTempFiles(List<MultipartFile> multipartFiles, List<File> cleanup)
			throws IOException {
		List<NamedTempFile> namedFiles = new ArrayList<>();
		for (MultipartFile multipartFile : multipartFiles) {
			if (multipartFile == null || multipartFile.isEmpty()) {
				continue;
			}
			String original = multipartFile.getOriginalFilename();
			String entryName = StringUtils.hasText(original) ? sanitize(original) : "file-" + UUID.randomUUID();
			File tempFile = File.createTempFile("upload-", "-" + entryName);
			multipartFile.transferTo(tempFile);
			cleanup.add(tempFile);
			namedFiles.add(new NamedTempFile(tempFile, entryName));
		}
		if (namedFiles.isEmpty()) {
			throw new IllegalArgumentException("Envie ao menos um arquivo em 'files'.");
		}
		return namedFiles;
	}

	private File createZip(List<NamedTempFile> files, String password, boolean encrypt) throws IOException {
		ZipParameters params = new ZipParameters();
		if (encrypt) {
			params.setEncryptFiles(true);
			params.setEncryptionMethod(EncryptionMethod.AES);
		}

		File target = File.createTempFile("archive-", ".zip");
		try (ZipFile zipFile = encrypt
				? new ZipFile(target, password.toCharArray())
				: new ZipFile(target)) {
			for (NamedTempFile named : files) {
				ZipParameters entryParams = new ZipParameters(params);
				entryParams.setFileNameInZip(named.entryName());
				zipFile.addFile(named.file(), entryParams);
			}
		}
		return target;
	}

	private File createSevenZ(List<NamedTempFile> files, String password, boolean encrypt) throws IOException {
		File target = File.createTempFile("archive-", ".7z");
		char[] pwd = encrypt ? password.toCharArray() : null;
		try (SevenZOutputFile sevenZ = encrypt
				? new SevenZOutputFile(target, pwd)
				: new SevenZOutputFile(target)) {
			for (NamedTempFile named : files) {
				SevenZArchiveEntry entry = sevenZ.createArchiveEntry(named.file(), named.entryName());
				sevenZ.putArchiveEntry(entry);
				sevenZ.write(Files.readAllBytes(named.file().toPath()));
				sevenZ.closeArchiveEntry();
			}
		}
		return target;
	}

	private List<File> extractZip(File archive, String password, File extractDir, List<File> cleanup)
			throws IOException {
		boolean encrypted = StringUtils.hasText(password);
		try (ZipFile zipFile = encrypted
				? new ZipFile(archive, password.toCharArray())
				: new ZipFile(archive)) {
			if (zipFile.isEncrypted() && !encrypted) {
				throw new IllegalArgumentException("Este ZIP está protegido por senha. Informe o parâmetro 'password'.");
			}
			zipFile.extractAll(extractDir.getAbsolutePath());
		}

		List<File> extracted = listFilesRecursively(extractDir);
		cleanup.addAll(extracted);
		return extracted;
	}

	private List<File> extractSevenZ(File archive, String password, File extractDir, List<File> cleanup)
			throws IOException {
		char[] pwd = StringUtils.hasText(password) ? password.toCharArray() : null;
		List<File> extracted = new ArrayList<>();
		try (SevenZFile sevenZFile = pwd != null
				? SevenZFile.builder().setFile(archive).setPassword(pwd).get()
				: SevenZFile.builder().setFile(archive).get()) {
			SevenZArchiveEntry entry;
			while ((entry = sevenZFile.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				File out = new File(extractDir, sanitize(entry.getName()));
				out.getParentFile().mkdirs();
				try (OutputStream os = new FileOutputStream(out)) {
					InputStream content = sevenZFile.getInputStream(entry);
					if (content != null) {
						content.transferTo(os);
					}
				}
				extracted.add(out);
				cleanup.add(out);
			}
		}
		catch (IOException e) {
			if (StringUtils.hasText(password)) {
				throw new IllegalArgumentException(
						"Falha ao descompactar 7z: verifique a senha e o arquivo. (" + e.getMessage() + ")", e);
			}
			throw e;
		}
		return extracted;
	}

	private ArchiveFormat detectFormat(String originalName, File uploaded) throws IOException {
		String lower = originalName.toLowerCase();
		if (lower.endsWith(".7z")) {
			return ArchiveFormat.SEVEN_Z;
		}
		if (lower.endsWith(".zip")) {
			return ArchiveFormat.ZIP;
		}
		byte[] header = Files.readAllBytes(uploaded.toPath());
		if (header.length >= 6 && header[0] == '7' && header[1] == 'z'
				&& (header[2] & 0xFF) == 0xBC && (header[3] & 0xFF) == 0xAF
				&& (header[4] & 0xFF) == 0x27 && (header[5] & 0xFF) == 0x1C) {
			return ArchiveFormat.SEVEN_Z;
		}
		if (header.length >= 2 && header[0] == 'P' && header[1] == 'K') {
			return ArchiveFormat.ZIP;
		}
		throw new IllegalArgumentException("Formato não reconhecido. Envie um arquivo .zip ou .7z.");
	}

	private List<File> listFilesRecursively(File dir) {
		List<File> result = new ArrayList<>();
		File[] children = dir.listFiles();
		if (children == null) {
			return result;
		}
		for (File child : children) {
			if (child.isDirectory()) {
				result.addAll(listFilesRecursively(child));
			}
			else {
				result.add(child);
			}
		}
		return result;
	}

	private String sanitize(String name) {
		String base = new File(name).getName();
		return base.replaceAll("[\\\\/]+", "_");
	}

	private void cleanupFiles(List<File> files) {
		for (File file : files) {
			try {
				if (file != null && file.isDirectory()) {
					deleteDirectory(file);
				}
				else if (file != null) {
					Files.deleteIfExists(file.toPath());
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void deleteDirectory(File dir) throws IOException {
		File[] children = dir.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory()) {
					deleteDirectory(child);
				}
				else {
					Files.deleteIfExists(child.toPath());
				}
			}
		}
		Files.deleteIfExists(dir.toPath());
	}
}
