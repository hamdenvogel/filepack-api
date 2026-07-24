package br.com.hvogel.filepack_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ArchiveFormatTest {

	@Test
	void fromNullOrBlankDefaultsToZip() {
		assertThat(ArchiveFormat.from(null)).isEqualTo(ArchiveFormat.ZIP);
		assertThat(ArchiveFormat.from("")).isEqualTo(ArchiveFormat.ZIP);
		assertThat(ArchiveFormat.from("   ")).isEqualTo(ArchiveFormat.ZIP);
	}

	@ParameterizedTest
	@ValueSource(strings = { "zip", "ZIP", " Zip " })
	void fromZip(String value) {
		assertThat(ArchiveFormat.from(value)).isEqualTo(ArchiveFormat.ZIP);
	}

	@ParameterizedTest
	@CsvSource({ "7z, SEVEN_Z", "7Z, SEVEN_Z", "seven_z, SEVEN_Z", "sevenz, SEVEN_Z", "seven-z, SEVEN_Z" })
	void fromSevenZ(String value, ArchiveFormat expected) {
		assertThat(ArchiveFormat.from(value)).isEqualTo(expected);
	}

	@Test
	void fromUnsupportedThrows() {
		assertThatThrownBy(() -> ArchiveFormat.from("rar"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("rar");
	}

	@Test
	void metadata() {
		assertThat(ArchiveFormat.ZIP.getExtension()).isEqualTo("zip");
		assertThat(ArchiveFormat.ZIP.getMediaType()).isEqualTo("application/zip");
		assertThat(ArchiveFormat.SEVEN_Z.getExtension()).isEqualTo("7z");
		assertThat(ArchiveFormat.SEVEN_Z.getMediaType()).isEqualTo("application/x-7z-compressed");
	}
}
