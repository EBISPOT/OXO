package uk.ac.ebi.spot.service;

import org.neo4j.ogm.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @date 14/06/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Service
public class CypherQueryService implements MappingQueryService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    Neo4jOperations neo4jOperations;


    private static String MATCH_MAPPING =
            " MATCH path= allShortestPaths( (ft:Term)-[m:MAPPING*1..%s]-(tt:Term))\n";

    private static String WHERE_MAPPING =
            " WHERE ft.curie = '%s' " +
                    "WITH tt,path, extract ( r in m |  r.sourcePrefix) as source\n" +
                    "MATCH (tt)-[HAS_SOURCE]-(td)\n";

    private static String RETURN  =
            " UNWIND source as source1\n" +
                    "RETURN tt.curie as curie, tt.label as label, collect (distinct td.prefix) as datasources, collect (distinct source1) as mappingSources, length(path) as dist\n" +
                    "ORDER BY dist";

    public CypherQueryService() {

    }

    private String getMappingQuery (String id, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix) {
        String query = "";

        if (distance < 0) {
            query = MATCH_MAPPING.replace("%s", "2");
        } else {
            query = MATCH_MAPPING.replace("%s", Integer.toString(distance));
        }
        query += WHERE_MAPPING.replace("%s", id);

        if (sourcePrefix.size() + targetPrefix.size() > 0) {
            query += "WHERE ";

            String sourceFilter = "";
            if (!sourcePrefix.isEmpty()) {
                sourceFilter = sourcePrefix.stream()
                        .map(sourcePrefixWrap)
                        .collect(Collectors.joining(" OR "));
            }

            String targetFilter = "";
            if (!targetPrefix.isEmpty()) {
                targetFilter = targetPrefix.stream()
                        .map(targetPrefixWrap)
                        .collect(Collectors.joining(" OR "));
            }

            if (!sourcePrefix.isEmpty() && !targetPrefix.isEmpty())  {
                String unionQuery = String.format("( %s ) AND ( %s) ", sourceFilter, targetFilter);
                query += unionQuery;
            }
            else  {
                query += sourceFilter  + " " + targetFilter;
            }
        }

        query +=RETURN;

        log.debug(query);
        return query;

    }

    private static Function<String,String> sourcePrefixWrap = new Function<String,String>() {
        @Override public String apply(String s) {
            return new StringBuilder().append("'").append(s).append("'").append(" in source").toString();
        }
    };

    private static Function<String,String> targetPrefixWrap = new Function<String,String>() {
        @Override public String apply(String s) {
            return new StringBuilder().append("'").append(s).append("'").append(" in td.alternatePrefix").toString();
        }
    };

    @Override
    public List<MappingResponse> getMappingResponseSearch(String id, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix) {

        String query = getMappingQuery(id, distance, sourcePrefix, targetPrefix);


        Result results = neo4jOperations.query(query, Collections.EMPTY_MAP);

        List<MappingResponse> target = new ArrayList<>();
        for (Map<String, Object> row : results)  {
            MappingResponse response = new MappingResponse();
            response.setCurie((String) row.get("curie"));
            response.setLabel((String) row.get("label"));
            response.setSourcePrefixes( Arrays.asList((String[]) row.get("mappingSources")));
            response.setTargetPrefixes( Arrays.asList((String[]) row.get("datasources")));
            response.setDistance(Integer.parseInt(row.get("dist").toString()));
            target.add(response);
        }
        return target;

    }


    private static String SUMMARY_MAPPING_QUERY =
            "MATCH (fd:Datasource)<-[:HAS_SOURCE]-(:Term)-[m:MAPPING]->(:Term)-[:HAS_SOURCE]->(td:Datasource)\n" +
                    "WITH { source : (fd.sourceType+'.'+fd.prefix), size: count(distinct m), target : (td.sourceType+'.'+td.prefix)} as row\n" +
                    "RETURN collect(row) as result";

    @Override
    public Object getMappingSummary() {
        Result results = neo4jOperations.query(SUMMARY_MAPPING_QUERY, new HashMap());

        for (Map<String, Object> row : results)  {
            return row.get("result");
        }
        return "";
    }

    private static String SUMMARY_GRAPH_QUERY =
            "MATCH path= shortestPath( (ft:Term)-[m:MAPPING*1..5]-(tt:Term))\n" +
                    "WHERE ft.curie = {curie}\n" +
                    "UNWIND nodes(path) as n\n" +
                    "UNWIND rels(path) as r\n" +
                    "WITH n, r\n" +
                    "MATCH (n)-[HAS_SOURCE]-(d:Datasource)\n" +
                    "RETURN {nodes: collect( distinct {id: n.curie, group : d.prefix}), links: collect (distinct {source: startNode(r).curie, target: endNode(r).curie, mappingSource: r.sourcePrefix}  )} as result";
    @Override
    public Object getMappingSummaryGraph(String curie) {
        HashMap params = new HashMap();
        params.put("curie", curie) ;
        Result results = neo4jOperations.query(SUMMARY_GRAPH_QUERY,params );
        for (Map<String, Object> row : results)  {
            return row.get("result");
        }
        return "";
    }
}
