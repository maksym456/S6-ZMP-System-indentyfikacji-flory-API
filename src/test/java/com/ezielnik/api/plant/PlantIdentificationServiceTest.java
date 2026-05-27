package com.ezielnik.api.plant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantIdentificationServiceTest {

    private PlantIdentificationService service;

    @Mock
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        service = new PlantIdentificationService("test-api-key");
    }

    @Test
    void identify_emptyFile_returnsEmptyResult() {
        MockMultipartFile emptyFile = new MockMultipartFile("photo", new byte[0]);
        PlantIdentificationService.IdentificationResult result = service.identify(emptyFile);
        assertThat(result.isRecognized()).isFalse();
        assertThat(result.detectedSpecies()).isNull();
    }

    @Test
    void identify_ioExceptionFromGetBytes_returnsEmptyResult() throws IOException {
        when(multipartFile.getBytes()).thenThrow(new IOException("disk read error"));

        PlantIdentificationService.IdentificationResult result = service.identify(multipartFile);
        assertThat(result.isRecognized()).isFalse();
    }

    @Test
    void identificationResult_empty_isNotRecognized() {
        PlantIdentificationService.IdentificationResult empty = PlantIdentificationService.IdentificationResult.empty();
        assertThat(empty.isRecognized()).isFalse();
        assertThat(empty.detectedSpecies()).isNull();
        assertThat(empty.confidence()).isNull();
        assertThat(empty.speciesId()).isNull();
        assertThat(empty.family()).isNull();
        assertThat(empty.genus()).isNull();
        assertThat(empty.commonNames()).isNull();
    }

    @Test
    void identificationResult_withSpecies_isRecognized() {
        PlantIdentificationService.IdentificationResult result = new PlantIdentificationService.IdentificationResult(
                "Rosa canina", 0.95, "12345", "Rosaceae", "Rosa", "Dog rose"
        );
        assertThat(result.isRecognized()).isTrue();
        assertThat(result.detectedSpecies()).isEqualTo("Rosa canina");
        assertThat(result.confidence()).isEqualTo(0.95);
    }

    @Test
    void identify_networkFailure_returnsEmptyResult() throws IOException {
        // When PlantNet API is unreachable, the service catches the exception and returns empty
        when(multipartFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(multipartFile.getBytes()).thenReturn(new byte[]{1, 2, 3});

        PlantIdentificationService.IdentificationResult result = service.identify(multipartFile);
        // The HTTP call to plantnet will fail in tests; the catch block returns empty
        assertThat(result.isRecognized()).isFalse();
    }
}
