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


    public void writeResultsAsCsv(List<SearchResult> searchResultList, char seperator, OutputStream outputStream) {

        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        CSVWriter csvWriter = new CSVWriter(writer, seperator);

        try {
            Iterator r = searchResultList.iterator();
            while (r.hasNext()) {

                SearchResult result = (SearchResult) r.next();


                for (MappingResponse response : result.getMappingResponseList()) {
                    List<String> row = new ArrayList<>();
                    row.add(result.getCurie());

                    row.add(response.getCurie());
                    row.add(StringUtils.join(response.getSourcePrefixes(), ','));
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
