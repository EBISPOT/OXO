package uk.ac.ebi.spot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.repository.DatasourceRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Simon Jupp
 * @date 12/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Service
public class DatasourceService {

    @Autowired
    private DatasourceRepository datasourceRepository;

    private Map<String, Datasource> datasourceCache = new HashMap();

    public Datasource getOrCreateDatasource (Datasource datasource) {

        if (datasourceCache.containsKey(datasource.getPrefix())) {
            return datasourceCache.get(datasource.getPrefix());
        }


        try {
            Datasource existingSource = datasourceRepository.findByPrefix(datasource.getPrefix());

            if (existingSource == null) {
                existingSource = datasourceRepository.save(datasource);
                datasourceCache.put(existingSource.getPrefix(), existingSource);
            } else {
                datasourceCache.put(existingSource.getPrefix(), existingSource);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return datasourceCache.get(datasource.getPrefix());

    }
}
