package uk.ac.ebi.spot.service;

import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.model.Mapping;

import java.util.Collection;

/**
 * @author Simon Jupp
 * @date 14/06/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class CypherMappingQueryBuilder {

    private static String MATCH_MAPPING =
            "MATCH (i:Identifier)-[*0..1]-(u:Uri)-[mapping:ALTID|:XREF*..%s]-(mapped)-[*0..1]-(mappedUri:Uri)-[:DATASOURCE]-(mappedTargetDatasource)\n" +
            "WITH u, mappedTargetDatasource, i, mappedUri, mapping\n" +
            "MATCH (mappedUri)-[:ALTID]-(prefixedId:PrefixedCurie)\n" +
            "WITH u, mappedTargetDatasource, i, mappedUri, mapping, prefixedId\n" +
            "MATCH (mappedFromDatasource)-[:DATASOURCE]-(u)\n";

    private static String WHERE_MAPPING = "WHERE i.identifier = '%s' ";
    private static String WHERE_OPTIONAL_SOURCE = "AND not mappedTargetDatasource.prefix = mappedFromDatasource.prefix ";
    private static String WHERE_WITH_SOURCE = "AND mappedTargetDatasource.prefix = '%s' ";
    private static String RETURN  = "RETURN DISTINCT mappedUri.identifier as iri, prefixedId.identifier as curie, collect(mapping) as mappings";

    public  CypherMappingQueryBuilder () {

    }

    public static String getMappingQuery (String id) {
        return getMappingQuery(id, null);
    }

    public static String getMappingQuery (String id, String sourcePrefix) {
        return getMappingQuery(id, -1, sourcePrefix);
    }

    public static String getMappingQuery (String id, int distance, String sourcePrefix) {
        String query = "";

        if (distance < 0) {
            query = MATCH_MAPPING.replace("%s", "2");
        } else {
            query = MATCH_MAPPING.replace("%s", Integer.toString(distance));
        }
        query += WHERE_MAPPING.replace("%s", id);

        if (sourcePrefix != null) {
            query += WHERE_WITH_SOURCE.replace("%s", sourcePrefix);
        } else {
            query += WHERE_OPTIONAL_SOURCE;
        }

        query +=RETURN;

        System.out.println(query);
        return query;

    }

    public static MappingResponse getMappingResponse (String uri, String curie, Collection<Mapping> mappings) {
        return new MappingResponse();
    }

    public static  class MappingResponse {

        private String iri;
        private String curie;
        private Collection<Mapping> mappings;

        public MappingResponse() {
        }

        public String getIri() {
            return iri;
        }

        public void setIri(String iri) {
            this.iri = iri;
        }

        public String getCurie() {
            return curie;
        }

        public void setCurie(String curie) {
            this.curie = curie;
        }

        public Collection<Mapping> getMappings() {
            return mappings;
        }

        public void setMappings(Collection<Mapping> mappings) {
            this.mappings = mappings;
        }
    }


}
