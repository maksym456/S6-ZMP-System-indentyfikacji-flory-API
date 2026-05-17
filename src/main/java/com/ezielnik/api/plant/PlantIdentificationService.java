package com.ezielnik.api.plant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class PlantIdentificationService {

    private static final String PLANTNET_URL = "https://my-api.plantnet.org/v2/identify/all";

    private final RestClient restClient;
    private final String apiKey;

    public PlantIdentificationService(@Value("${plantnet.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    public IdentificationResult identify(MultipartFile photo) {
        byte[] bytes;
        try {
            bytes = photo.getBytes();
        } catch (IOException e) {
            return IdentificationResult.empty();
        }

        String filename = photo.getOriginalFilename() != null ? photo.getOriginalFilename() : "photo.jpg";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("images", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        try {
            PlantNetResponse response = restClient.post()
                    .uri(PLANTNET_URL + "?api-key=" + apiKey + "&nb-results=1&lang=en")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PlantNetResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return IdentificationResult.empty();
            }

            PlantNetResult top = response.results().getFirst();
            PlantNetSpecies s = top.species();
            if (s == null) return IdentificationResult.empty();

            String detectedSpecies = s.scientificNameWithoutAuthor();
            if (detectedSpecies == null || detectedSpecies.isBlank()) return IdentificationResult.empty();

            String speciesId = s.gbif() != null ? s.gbif().id() : null;
            String family = s.family() != null ? s.family().scientificName() : null;
            String genus = s.genus() != null ? s.genus().scientificName() : null;
            String commonNames = s.commonNames() != null && !s.commonNames().isEmpty()
                    ? String.join(", ", s.commonNames())
                    : null;

            return new IdentificationResult(detectedSpecies, top.score(), speciesId, family, genus, commonNames);

        } catch (Exception e) {
            return IdentificationResult.empty();
        }
    }

    public record IdentificationResult(
            String detectedSpecies, Double confidence,
            String speciesId, String family, String genus, String commonNames
    ) {
        public boolean isRecognized() {
            return detectedSpecies != null;
        }

        public static IdentificationResult empty() {
            return new IdentificationResult(null, null, null, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlantNetResponse(List<PlantNetResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlantNetResult(Double score, PlantNetSpecies species) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlantNetSpecies(
            String scientificNameWithoutAuthor,
            PlantNetTaxon genus,
            PlantNetTaxon family,
            List<String> commonNames,
            PlantNetGbif gbif
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlantNetTaxon(String scientificName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlantNetGbif(String id) {}
}