package uk.ac.ebi.spot.service;

import org.neo4j.ogm.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Identifier;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.MappingSource;
import uk.ac.ebi.spot.repository.MappingRepository;

import java.util.*;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Service
public class MappingService {


    @Autowired
    private IdentifierService identifierService;


    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private DatasourceService datasourceService;

    @Autowired
    Neo4jOperations neo4jOperations;

    @Autowired
    Neo4jTemplate neo4jTemplate;
//    @Autowired
//    CypherMappingQueryBuilder cypherMappingQueryBuilder;

    public MappingService() {
    }

    public void getOrCreateMappings(Collection<MappingSource> mappingSources) {

        for (MappingSource mappingSource : mappingSources) {

            List<Mapping> mappingCollection = new ArrayList<>();
            Collection<Mapping> mappings = mappingSource.getMappings();

            Datasource datasource = mappingSource.getDatasource();
            datasource = datasourceService.getOrCreateDatasource(datasource);

            System.out.println("About to process " + mappings.size() + " mappings for " + datasource.getPrefix());
            int counter = 0;
            try {
                for (Mapping mapping : mappings) {

                    Identifier fromId = identifierService.getOrCreateIdentifier(mapping.getFromIdentifier());
                    Identifier toId = identifierService.getOrCreateIdentifier(mapping.getToIdentifier());

                    mapping.setFromIdentifier(fromId);
                    mapping.setToIdentifier(toId);

                    mapping.setDatasource(datasource);
                    mappingCollection.add(mapping);

                    if (counter == 1000) {
                        System.out.print(".");
                        counter=0;
                    }
                    counter++;
                }

                mappingRepository.save(mappingCollection);
             } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public List<CypherMappingQueryBuilder.MappingResponse> getMappingsSearch(String id, int distance, String sourcePrefix) {
        Result results = neo4jOperations.query(CypherMappingQueryBuilder.getMappingQuery(id, distance, sourcePrefix), new HashMap());
        List<CypherMappingQueryBuilder.MappingResponse> target = new ArrayList<>();
        for (Map<String, Object> row : results)  {
            CypherMappingQueryBuilder.MappingResponse response = new CypherMappingQueryBuilder.MappingResponse();
            response.setIri((String) row.get("iri"));
            response.setCurie((String) row.get("curie"));
            response.setCurie((String) row.get("curie"));

//            neo4jTemplate.getDefaultConverter().
        }
//        results.forEach(target::add);
        return target;
    }


    public void dropMappingsBySource(String sourcePrefix) {
        Iterable<Mapping> mappings = mappingRepository.findAllBySourcePrefix(sourcePrefix);
        mappingRepository.delete(mappings);
    }




}
