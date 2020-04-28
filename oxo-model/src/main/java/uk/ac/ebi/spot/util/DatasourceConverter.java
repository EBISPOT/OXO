package uk.ac.ebi.spot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.ogm.typeconversion.AttributeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.repository.DatasourceRepository;

import java.io.IOException;

/**
 * @author Simon Jupp
 * @since 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class DatasourceConverter implements AttributeConverter<Datasource, String> {

    @Autowired
    DatasourceRepository datasourceRepository;

   @Override
   public String toGraphProperty(Datasource value) {
        return value.getPrefix();
   }

   @Override
   public Datasource toEntityAttribute(String value) {
       return datasourceRepository.findByPrefix(value);

   }
}