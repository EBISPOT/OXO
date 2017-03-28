package uk.ac.ebi.spot.model;

/**
 * @author Simon Jupp
 * @date 09/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class MappingRequest {

    private String fromId;
    private String toId;
    private String datasourcePrefix;
    private SourceType sourceType;
    private Scope scope;

    public MappingRequest(String fromId, String toId, String datasourcePrefix, SourceType sourceType, Scope scope) {
        this.fromId = fromId;
        this.toId = toId;
        this.datasourcePrefix = datasourcePrefix;
        this.sourceType = sourceType;
        this.scope = scope;
    }

    public MappingRequest() {

    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public String getDatasourcePrefix() {
        return datasourcePrefix;
    }

    public void setDatasourcePrefix(String datasourcePrefix) {
        this.datasourcePrefix = datasourcePrefix;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }
}
