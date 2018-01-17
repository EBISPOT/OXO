package uk.ac.ebi.spot.service;

import org.neo4j.ogm.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.util.MappingDistance;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @since 14/06/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Service
public class CypherQueryService implements MappingQueryService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    Neo4jOperations neo4jOperations;

    SimpleCache<String, LinkedHashMap<String, SearchResult>> cypherSearchResultsCache;

    public CypherQueryService() {

        cypherSearchResultsCache = new SimpleCache<String, LinkedHashMap<String, SearchResult>>(120, 60, 20);
    }

    /**
     * Query builder for getting mappings for a particular resource
     */

    private static String MAPPING_BY_DATASOURCE_QUERY_PART1="MATCH (fd:Datasource)<-[:HAS_SOURCE]-(ft:Term),(td:Datasource)<-[:HAS_SOURCE]-(tt:Term)\n";

    private static String MAPPING_BY_DATASOURCE_QUERY_PART2= "WITH ft, fd, tt, td\n" +
            "MATCH path =  (ft)-[m:MAPPING*1..%s]-(tt) \n" +
            "WHERE fd <> td\n" +
            "WITH ft, tt, td, length(path) as length, extract ( r in m |  r.sourcePrefix) as edges\n";


    private static String MAPPING_BY_DATASOURCE_QUERY_PART3= "UNWIND edges as edge\n" +
            "WITH ft, tt, length,  td.prefix as targetPrefix, collect (distinct edge) as edgeSource ORDER BY ft.curie, length\n" +  // order to get shortest path first
            "WITH ft, tt, collect ( [length, targetPrefix, edgeSource])[0] as shortest\n" +   // move forward with only shortest path
//            "UNWIND tmp as shortest\n" +
            "RETURN distinct (ft.curie) as fromCurie, ft.label as fromLabel, tt.curie as curie, tt.label as label, shortest[0] as dist, shortest[1] as datasources, shortest[2] as mappingSources";
//            "RETURN distinct(ft.curie) as fromCurie, ft.label as fromLabel, tt.curie as curie, tt.label as label, MIN(length) as dist, td.prefix as datasources, collect (distinct source1) as mappingSources ORDER BY fromCurie";

    private String getMappingQuery2 (String id, String fromDatasource, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix) {

        if (id == null && fromDatasource == null && sourcePrefix.isEmpty()) {
            log.error("You must supply either an input id, mapping source or datasource to get mappings");
            throw new RuntimeException("You must supply either an input id, mapping source or datasource to get mappings");
        }

        // build the query
        String query = MAPPING_BY_DATASOURCE_QUERY_PART1;
        // filter from and target datasources

        // query by id
        if (id != null) {
            query += "WHERE ft.curie = '" + id + "' ";
        }
        // query by datasource
        else if (fromDatasource != null) {
            query += "WHERE fd.prefix = '" + fromDatasource + "'";
        }

        // add list of targets
        if (!targetPrefix.isEmpty()) {

            String targetFilter = targetPrefix.stream()
                    .map(targetPrefixWrap)
                    .collect(Collectors.joining(" OR "));

            query+= " AND ( " +targetFilter + ")\n";
        }

        // add distance

        if (distance < 0) {
            query += MAPPING_BY_DATASOURCE_QUERY_PART2.replace("%s", Integer.toString(MappingDistance.DEFAULT_MAPPING_DISTANCE));
        } else {
            query += MAPPING_BY_DATASOURCE_QUERY_PART2.replace("%s", Integer.toString(distance));
        }

        // add any source filters
        if (!sourcePrefix.isEmpty()) {
            String sourceFilter = sourcePrefix.stream()
                    .map(sourcePrefixWrap)
                    .collect(Collectors.joining(" OR "));

            query+= "WHERE (" + sourceFilter + ")\n";
        }

        query +=MAPPING_BY_DATASOURCE_QUERY_PART3;

        log.debug("\n"+query);
        return query;

    }

    @Override
    public SearchResult getMappingResponseSearchById(String id, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix) {

        String query = getMappingQuery2(id, null, distance, sourcePrefix, targetPrefix);
        Result results = neo4jOperations.query(query, Collections.EMPTY_MAP);

        List<MappingResponse> target = new ArrayList<>();

        SearchResult searchResult = new SearchResult(id, null,null, null, target);
        for (Map<String, Object> row : results)  {
            MappingResponse response = new MappingResponse();
            response.setCurie((String) row.get("curie"));
            response.setLabel((String) row.get("label"));
            response.setSourcePrefixes( Arrays.asList((String[]) row.get("mappingSources")));
            response.setTargetPrefix( (String) row.get("datasources"));
            response.setDistance(Integer.parseInt(row.get("dist").toString()));
            target.add(response);
            searchResult.setCurie((String) row.get("fromCurie"));
            searchResult.setLabel((String) row.get("fromLabel"));
        }
        searchResult.setMappingResponseList(target);
        return searchResult;

    }


    @Override
    public Page<SearchResult> getMappingResponseSearchByDatasource(String fromDatasource, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix, Pageable pageable) {

        String query = getMappingQuery2(null, fromDatasource, distance, sourcePrefix, targetPrefix);

        LinkedHashMap<String, SearchResult> idToSearchResultMap = new LinkedHashMap<String, SearchResult>();

        if (cypherSearchResultsCache.get(query) != null) {
            idToSearchResultMap =  cypherSearchResultsCache.get(query);
        } else {
            Result results = neo4jOperations.query(query, Collections.EMPTY_MAP);


            for (Map<String, Object> row : results)  {
                String fromCurie = (String) row.get("fromCurie");
                String fromLabel = (String) row.get("fromLabel");

                if (!idToSearchResultMap.containsKey(fromCurie)) {
                    idToSearchResultMap.put(fromCurie, new SearchResult(null, fromDatasource, fromCurie, fromLabel, new ArrayList<MappingResponse>()));
                }


                MappingResponse response = new MappingResponse();
                response.setCurie((String) row.get("curie"));
                response.setLabel((String) row.get("label"));
                response.setSourcePrefixes( Arrays.asList((String[]) row.get("mappingSources")));
                response.setTargetPrefix( (String) row.get("datasources"));
                response.setDistance(Integer.parseInt(row.get("dist").toString()));

                idToSearchResultMap.get(fromCurie).getMappingResponseList().add(response);
            }

            cypherSearchResultsCache.put(query, idToSearchResultMap);
        }

        int size =  pageable.getOffset() + pageable.getPageSize() > idToSearchResultMap.values().size() ? idToSearchResultMap.values().size() : pageable.getOffset() + pageable.getPageSize();
        return new PageImpl<SearchResult>(
                new ArrayList<SearchResult>(idToSearchResultMap.values()).subList(pageable.getOffset(), size),
                pageable,
                idToSearchResultMap.keySet().size()

        );

    }

    /////////////////////////////// end block for get query by datasoruce /////////////////////////////////////////////////////////////////////////////////



    private static String SUMMARY_MAPPING_QUERY =
            "MATCH (fd:Datasource)<-[:HAS_SOURCE]-()-[m:MAPPING]->()-[:HAS_SOURCE]->(td)\n" +
                    " WHERE_CLAUSE \n"+
                    "WITH { source : fd.prefix,  sourceType : fd.sourceType, size: count(distinct m), target : td.prefix, targetType : td.sourceType} as row, count(distinct m) as count\n" +
                    "WHERE  count > 1 " +
                    "RETURN collect(row) as result";
    private static String SUMMARY_WHERE_CLAUSE = " WHERE fd.prefix = {source} or m.sourcePrefix = {source} or td.prefix = {source} ";
    @Override
    public Object getMappingSummary(String sourcePrefix) {

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("source", sourcePrefix);
        return getMappingSummaryQuery (SUMMARY_MAPPING_QUERY.replace("WHERE_CLAUSE", SUMMARY_WHERE_CLAUSE), params);

    }

    @Override
    public Object getMappingSummary() {
        return getMappingSummaryQuery (SUMMARY_MAPPING_QUERY.replace("WHERE_CLAUSE", ""), new HashMap());
    }

    public Object getMappingSummaryQuery(String query, Map params) {
        Result results = neo4jOperations.query(query, params);

        for (Map<String, Object> row : results)  {
            return row.get("result");
        }
        return "";
    }

    private static String SUMMARY_GRAPH_QUERY =
            "MATCH path= (ft:Term)-[m:MAPPING*1..%s]-(tt:Term)\n" +
                    "WHERE ft.curie = {curie}\n" +
                    "UNWIND nodes(path) as n\n" +
                    "UNWIND rels(path) as r\n" +
                    "WITH n, r\n" +
                    "MATCH (n)-[HAS_SOURCE]-(d:Datasource)\n" +
                    "RETURN {nodes: collect( distinct {id: n.curie, group : d.prefix}), links: collect (distinct {source: startNode(r).curie, target: endNode(r).curie, scope: r.scope}  )} as result";
    @Override
    public Object getMappingSummaryGraph(String curie, int distance) {
        HashMap params = new HashMap();
        params.put("curie", curie) ;
        String query = String.format(SUMMARY_GRAPH_QUERY, distance);
        Result results = neo4jOperations.query(query,params );
        for (Map<String, Object> row : results)  {
            return row.get("result");
        }
        return "";
    }

    private static String MAPPED_TERM_IDS = "MATCH (fd:Datasource)<-[:HAS_SOURCE]-(ft:Term)-[m:MAPPING*1..%s]-(tt:Term)-[:HAS_SOURCE]->(td:Datasource) \n" +
            " WHERE fd.prefix = '%s' AND td.prefix = '%s'  AND fd <> td return distinct(fd.curie) as curie";
    @Override
    public List<String> getMappedTermCuries(String fromDatasource, String targetDatasource, int distance) {
        String query = String.format(MAPPED_TERM_IDS, distance, fromDatasource, targetDatasource);
        Result results = neo4jOperations.query(query, Collections.emptyMap() );
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        List<String> set = new ArrayList<>();
        for (Map<String, Object> row : results) {
            set.add((String)row.get("curie"));
        }
        return set;
    }

    private static String MAPPED_TARGET_COUNTS_QUERY = "MATCH (fd:Datasource)<-[:HAS_SOURCE]-(ft:Term)-[m:MAPPING*1..%s]-(tt:Term)-[:HAS_SOURCE]->(td:Datasource) \n"+
            " WHERE fd.prefix = '%s' AND fd <> td return td.prefix as datasource, count(distinct(tt.curie)) as count ORDER BY count DESC";
    @Override
    public LinkedHashMap<String, Integer> getMappedTargetCounts(String fromDatasource, int distance) {
        String query = String.format(MAPPED_TARGET_COUNTS_QUERY, distance, fromDatasource);
        Result results = neo4jOperations.query(query, Collections.emptyMap() );
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> row : results) {
            map.put((String)row.get("datasource"),(Integer)row.get("count"));
        }
        return map;
    }


    // todo remove fixing to lower case
    private static Function<String,String> sourcePrefixWrap = new Function<String,String>() {
        @Override public String apply(String s) {
            return new StringBuilder().append("'").append(s.toLowerCase()).append("'").append(" in source").toString();
        }
    };

    private static Function<String,String> targetPrefixWrap = new Function<String,String>() {
        @Override public String apply(String s) {
            return new StringBuilder().append("'").append(s).append("'").append(" in td.alternatePrefix").toString();
        }
    };



    /**
     * Query builder for getting mappings by id
     */

    private static String MATCH_MAPPING =
            " MATCH path= allShortestPaths( (ft:Term)-[m:MAPPING*1..%s]-(tt:Term))\n";

    private static String WHERE_MAPPING =
            " WHERE ft.curie = '%s' " +
                    "WITH tt,path, extract ( r in m |  r.sourcePrefix) as source\n" +
                    "MATCH (tt)-[HAS_SOURCE]-(td)\n";

    private static String RETURN  =
            " UNWIND source as source1\n" +
                    "RETURN ft.curie as fromCurie, ft.label as fromLabel, tt.curie as curie, tt.label as label, collect (distinct td.prefix) as datasources, collect (distinct source1) as mappingSources, length(path) as dist\n" +
                    "ORDER BY dist";

    @Deprecated
    private String getMappingQuery (String id, String fromDatasource, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix) {
        String query = "";

        if (distance < 0) {
            query = MATCH_MAPPING.replace("%s", Integer.toString(MappingDistance.DEFAULT_MAPPING_DISTANCE));
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



}
