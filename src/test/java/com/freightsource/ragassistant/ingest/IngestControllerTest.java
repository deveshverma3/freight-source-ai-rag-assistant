package com.freightsource.ragassistant.ingest;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.freightsource.ragassistant.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngestController.class)
@Import(SecurityConfig.class)
class IngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentIngestor ingestor;

    @Test
    void rejectsUploadsWithNoCredentials() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notes.md", "text/markdown", "content".getBytes());

        mockMvc.perform(multipart("/ingest/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ingestsAnUploadedFileToATempPathPreservingItsExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "policy.md", "text/markdown", "content".getBytes());
        when(ingestor.ingest(org.mockito.ArgumentMatchers.any(Path.class))).thenReturn(3);

        mockMvc.perform(multipart("/ingest/upload").file(file).with(httpBasic("admin", "changeme")))
                .andExpect(status().isOk())
                .andExpect(content().string("Ingested 3 chunks from policy.md"));

        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        verify(ingestor).ingest(pathCaptor.capture());
        assertThat(pathCaptor.getValue().toString()).endsWith(".md");
    }

    @Test
    void rejectsAnUploadWithNoFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "text/markdown", "content".getBytes());

        mockMvc.perform(multipart("/ingest/upload").file(file).with(httpBasic("admin", "changeme")))
                .andExpect(status().isBadRequest());
    }
}
