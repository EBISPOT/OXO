package uk.ac.ebi.spot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.spot.model.*;
import uk.ac.ebi.spot.repository.CuiRepository;
import uk.ac.ebi.spot.repository.IdentifierRepository;
import uk.ac.ebi.spot.repository.PrefixedCuiRepository;
import uk.ac.ebi.spot.repository.UriNodeRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Service
public class IdentifierService {


    @Autowired
    private PrefixedCuiRepository prefixedCuiRepository;

    @Autowired
    private CuiRepository cuiRepository;

    @Autowired
    private DatasourceService datasourceService;

    @Autowired
    private UriNodeRepository uriNodeRepository;

    @Autowired
    private IdentifierRepository identifierRepository;

    private Map<String, Identifier> identifierCache = new HashMap();;

    @Transactional
    public Identifier getOrCreateIdentifier(Identifier identifier) {
//        Identifier id = identifierRepository.findByIdentifier(identifier.getIdentifier());
//        if (id == null) {
//            return identifierRepository.save(identifier);
//        }
//        return id;
        if (identifierCache.containsKey(identifier.getIdentifier())) {
            return identifierCache.get(identifier.getIdentifier());
        }

        if (identifier instanceof URINode) {
            Datasource datasource = ((URINode) identifier).getDatasource();
            if (datasource != null) {
                if (datasource.getId() == null) {
                    datasource = datasourceService.getOrCreateDatasource(datasource);
                    ((URINode) identifier).setDatasource(datasource);
                }
            }
        }


        try {
            Identifier existingId= null;

            if (identifier instanceof URINode) {
              existingId  = uriNodeRepository.findByIdentifier(identifier.getIdentifier());
            }
            else if (identifier instanceof PrefixedCurie) {
                existingId  = prefixedCuiRepository.findByIdentifier(identifier.getIdentifier());
            }
            else if (identifier instanceof CurieNode) {
                existingId  = cuiRepository.findByIdentifier(identifier.getIdentifier());
            }

            if (existingId == null) {
                existingId = identifierRepository.save(identifier);
                identifierCache.put(existingId.getIdentifier(), existingId);
            } else {
                identifierCache.put(existingId.getIdentifier(), existingId);
            }

        }  catch (Exception e) {
            e.printStackTrace();
        }
        return identifierCache.get(identifier.getIdentifier());


    }

    public void dropIdentifiersBySource(String sourcePrefix) {

    }


}
