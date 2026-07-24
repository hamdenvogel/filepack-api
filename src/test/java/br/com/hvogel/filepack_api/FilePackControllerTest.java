package br.com.hvogel.filepack_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class FilePackControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void sobreReturnsJsonWithExpectedFields() throws Exception {
		mockMvc.perform(get("/api/filepack/sobre"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.autor").value("HV Softwares"))
				.andExpect(jsonPath("$.versao").isNotEmpty())
				.andExpect(jsonPath("$.aplicacao").value("FilePack API"))
				.andExpect(jsonPath("$.dataHoraAlteracao").isNotEmpty())
				.andExpect(jsonPath("$.consultadoEm").isNotEmpty())
				.andExpect(jsonPath("$.formatosSuportados[0]").value("zip"))
				.andExpect(jsonPath("$.formatosSuportados[1]").value("7z"))
				.andExpect(jsonPath("$.endpoints").isArray())
				.andExpect(jsonPath("$.javaVersion").isNotEmpty())
				.andExpect(jsonPath("$.springBootVersion").isNotEmpty());
	}

	@Test
	void createEncryptedZip() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"files", "hello.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/filepack")
						.file(file)
						.param("password", "teste123")
						.param("encrypt", "true")
						.param("format", "zip"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".zip")))
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/zip")));
	}

	@Test
	void createPlainZip() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"files", "plain.txt", "text/plain", "plain".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/filepack")
						.file(file)
						.param("encrypt", "false")
						.param("format", "zip"))
				.andExpect(status().isOk());
	}

	@Test
	void createSevenZ() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"files", "doc.txt", "text/plain", "seven".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/filepack")
						.file(file)
						.param("password", "abc")
						.param("encrypt", "true")
						.param("format", "7z"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".7z")));
	}

	@Test
	void createWithoutPasswordWhenEncryptTrueReturns400() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"files", "a.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/filepack")
						.file(file)
						.param("encrypt", "true"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("requisicao_invalida"));
	}

	@Test
	void unsupportedFormatReturns400() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"files", "a.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/filepack")
						.file(file)
						.param("password", "x")
						.param("format", "rar"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unpackEncryptedZipRoundTrip() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"files", "round.txt", "text/plain", "roundtrip".getBytes(StandardCharsets.UTF_8));

		MvcResult packResult = mockMvc.perform(multipart("/api/filepack")
						.file(file)
						.param("password", "segredo")
						.param("encrypt", "true")
						.param("format", "zip"))
				.andExpect(status().isOk())
				.andReturn();

		byte[] zipBytes = packResult.getResponse().getContentAsByteArray();
		assertThat(zipBytes).isNotEmpty();

		MockMultipartFile archive = new MockMultipartFile(
				"file", "pack.zip", "application/zip", zipBytes);

		mockMvc.perform(multipart("/api/filepack/unpack")
						.file(archive)
						.param("password", "segredo"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/zip")));
	}

	@Test
	void healthEndpointStillUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}
}
