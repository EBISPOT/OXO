package uk.ac.ebi.spot.service;

import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Simon Jupp
 * @date 05/10/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class SearchResultsCsvBuilder {


    static String [] HEADERS = {"curie_id", "label", "mapped_curie", "mapped_label", "mapping_source_prefix", "mapping_target_prefix", "distance" };
    public void writeResultsAsCsv(List<SearchResult> searchResultList, char seperator, OutputStream outputStream) {

        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        CSVWriter csvWriter = new CSVWriter(writer, seperator);

        try {
            Iterator r = searchResultList.iterator();
            csvWriter.writeNext(
                    HEADERS
            );
            while (r.hasNext()) {

                SearchResult result = (SearchResult) r.next();


                for (MappingResponse response : result.getMappingResponseList()) {
                    List<String> row = new ArrayList<>();
                    row.add(result.getCurie());
                    row.add(result.getLabel());

                    row.add(response.getCurie());
                    row.add(response.getLabel());
                    row.add(StringUtils.join(response.getSourcePrefixes(), ','));
                    row.add(StringUtils.join(response.getTargetPrefixes(), ','));
                    row.add(String.valueOf(response.getDistance()));

                    csvWriter.writeNext(row.toArray(new String [row.size()]));
                }
                csvWriter.flush();

            }
            csvWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
