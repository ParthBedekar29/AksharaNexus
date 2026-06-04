package com.example.nexusa.University.Service;

import com.example.nexusa.Model.University;
import com.example.nexusa.Model.UniversityDomain;
import com.example.nexusa.Repository.UniversityDomainRepository;
import com.example.nexusa.Repository.UniversityRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
@Service
public class RorService {
    private final WebClient webClient;
    private final UniversityRepository universityRepository;
    private final UniversityDomainRepository universityDomainRepository;

    public RorService(WebClient webClient, UniversityRepository universityRepository, UniversityDomainRepository universityDomainRepository) {
        this.webClient = webClient;
        this.universityRepository = universityRepository;
        this.universityDomainRepository = universityDomainRepository;
    }
    @Transactional
    public void seedUniversities(){
        if (universityRepository.count() > 0) return;

        int page = 1;
        boolean hasMore = true;

        while(hasMore){
            JsonNode seedResponse=webClient.get()
                    .uri("/v2/organizations?filter=country.country_code:IN,types:education&query.advanced=domains:*&page=" + page)
                    .retrieve().bodyToMono(JsonNode.class).block();
            JsonNode items = seedResponse.get("items");

            if (items == null || items.isEmpty()) {
                hasMore = false;
                break;
            }

            for(JsonNode item:items){

                String name=null;
                String rorId=null;
                University university=new University();
                for (JsonNode nameObj : item.get("names")) {
                    for (JsonNode type : nameObj.get("types")) {
                        if (type.asText().equals("ror_display")) {
                            name = nameObj.get("value").asText();
                            break;
                        }
                    }
                    if (name != null) break;
                }

                String fullId = item.get("id").asText();
                rorId = fullId.substring(fullId.lastIndexOf('/') + 1);

                university.setName(name);
                university.setRorId(rorId);

                universityRepository.save(university);
                for (JsonNode domainObj : item.get("domains")) {
                    if (domainObj.asText().isBlank()) continue;

                    if (universityDomainRepository.findByDomain(domainObj.asText()).isEmpty()) {
                        UniversityDomain universityDomain = new UniversityDomain();
                        universityDomain.setDomain(domainObj.asText());
                        universityDomain.setUniversity(university);
                        universityDomainRepository.save(universityDomain);
                    }
                }


            }
            page++;
        }

    }
}
