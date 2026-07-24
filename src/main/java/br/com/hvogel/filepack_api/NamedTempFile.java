package br.com.hvogel.filepack_api;

import java.io.File;

/**
 * Arquivo temporário associado ao nome original enviado no upload.
 */
record NamedTempFile(File file, String entryName) {
}
