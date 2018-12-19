import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.response.model.QueryResultModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.spot.OxoWebApp;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.SourceType;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.service.DatasourceService;
import uk.ac.ebi.spot.service.MappingService;
import uk.ac.ebi.spot.service.TermService;

import javax.servlet.RequestDispatcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Simon Jupp
 * @since 20/03/2017
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OxoWebApp.class)
@WebAppConfiguration
@Ignore
public class ApiDocumentation {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("src/main/asciidoc/generated-snippets");

    private RestDocumentationResultHandler document;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private DatasourceService datasourceService;

    @MockBean
    private TermService termService;

    @MockBean
    private MappingService mappingService;

    @MockBean
    Neo4jOperations neo4jTemplate;

    @Before
    public void setUp() {

        Mockito.when(neo4jTemplate.query(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean())).thenReturn(new QueryResultModel(null, null));

        this.document = document("{method-name}"
                ,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())
        );

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation).uris()
                                .withScheme("https")
                                .withHost("www.ebi.ac.uk")
                                .withPort(443)
                )
                .alwaysDo(this.document)
                .build();
    }

//    @Test
//    public void datasourcesListExample () throws Exception {
//
//        Datasource datasource = new Datasource("test", "TEST", "test", Collections.emptySet(), "test ids", "database of test identifiers", SourceType.ONTOLOGY);
//
//        Page<Datasource> page = new PageImpl<Datasource>(Collections.singletonList(datasource), new PageRequest(1, 20), 60);
//
//        this.document.snippets(
//                responseFields(
//                        fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
//                        fieldWithPath("_embedded").description("The list of datasources"),
//                        fieldWithPath("page.size").description("The number of resources in this page"),
//                        fieldWithPath("page.totalElements").description("The total number of resources"),
//                        fieldWithPath("page.totalPages").description("The total number of pages"),
//                        fieldWithPath("page.number").description("The page number")
//                ),
//                links(halLinks(),
//                        linkWithRel("self").description("This resource list"),
//                        linkWithRel("first").description("The first page in the resource list"),
//                        linkWithRel("next").description("The next page in the resource list"),
//                        linkWithRel("prev").description("The previous page in the resource list"),
//                        linkWithRel("last").description("The last page in the resource list")
//                )
//
//        );
//
//        Mockito.when(datasourceService.getDatasources(new PageRequest(0, 20))).thenReturn(page);
//        RequestBuilder requestBuilder = MockMvcRequestBuilders
//                .get("/spot/oxo/api/datasources").contextPath("/spot/oxo").accept(MediaType.APPLICATION_JSON);
//
//        this.mockMvc.perform(requestBuilder)
//                .andExpect(status().isOk());
//    }

    @Test
    public void pageExample () throws Exception {

        Datasource datasource = new Datasource("test", "TEST", "test", Collections.emptySet(), "test ids", "database of test identifiers", SourceType.ONTOLOGY);

        Page<Datasource> page = new PageImpl<Datasource>(Collections.singletonList(datasource), new PageRequest(1, 20), 60);

        this.document.snippets(
                responseFields(
                        fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                        fieldWithPath("_embedded").description("The list of resources"),
                        fieldWithPath("page.size").description("The number of resources in this page"),
                        fieldWithPath("page.totalElements").description("The total number of resources"),
                        fieldWithPath("page.totalPages").description("The total number of pages"),
                        fieldWithPath("page.number").description("The page number")
                ),
                links(halLinks(),
                        linkWithRel("self").description("This resource list"),
                        linkWithRel("first").description("The first page in the resource list"),
                        linkWithRel("next").description("The next page in the resource list"),
                        linkWithRel("prev").description("The previous page in the resource list"),
                        linkWithRel("last").description("The last page in the resource list")
                )

        );

        Mockito.when(datasourceService.getDatasources(new PageRequest(0, 20))).thenReturn(page);
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/spot/oxo/api/datasources").contextPath("/spot/oxo").accept(MediaType.APPLICATION_JSON);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk());

    }


    @Test
    public void errorExample() throws Exception {
        this.document.snippets(
                responseFields(
                        fieldWithPath("error").description("The HTTP error that occurred, e.g. `Bad Request`").optional(),
//                fieldWithPath("exception").description("A description of the cause of the error").optional(),
                        fieldWithPath("message").description("A description of the cause of the error").optional(),
                        fieldWithPath("path").description("The path to which the request was made").optional(),
                        fieldWithPath("status").description("The HTTP status code, e.g. `400`").optional(),
                        fieldWithPath("timestamp").description("The time, in milliseconds, at which the error occurred").optional()));

        Mockito.when(datasourceService.getDatasource(Mockito.anyString())).thenReturn(null);
        
        this.mockMvc
                .perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI,
                                "/spot/oxo/api/datasources/foobar")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE,
                                "Datasource not found"))
        ;
    }

    @Test
    public void apiExample () throws Exception {

        this.document.snippets(
                responseFields(
                        fieldWithPath("_links").description("<<resources-mappings-links,Links>> to other resources")
                ),
                links(halLinks(),
                        linkWithRel("mappings").description("Link to the mapping in OxO"),
                        linkWithRel("terms").description("Link to all the terms in OxO"),
                        linkWithRel("datasources").description("Link to all the datasources for mappings in OxO"),
                        linkWithRel("profile").description("ALPS is not currently supported")
                        )
        );
        this.mockMvc.perform(get("/spot/oxo/api").contextPath("/spot/oxo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    @Test
    public void termsListExample () throws Exception {

        Datasource datasource = new Datasource("test", "TEST", "test", Collections.emptySet(), "test ids", "database of test identifiers", SourceType.ONTOLOGY);
        Term term = new Term("FOO:001", "001", "http://www.example.com/FOO/001", "Foo 1", datasource);

        Page<Term> page = new PageImpl<Term>(Collections.singletonList(term), new PageRequest(1, 20), 60);

        Mockito.when(termService.getTerms(new PageRequest(0, 20))).thenReturn(page);

        this.mockMvc.perform(get("/spot/oxo/api/terms").contextPath("/spot/oxo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }
    @Test
    public void mappingsListExample () throws Exception {

        Datasource datasource = new Datasource("test", "TEST", "test", Collections.emptySet(), "test ids", "database of test identifiers", SourceType.ONTOLOGY);
        Term term1 = new Term("FOO:001", "001", "http://www.example.com/FOO/001", "Foo 1", datasource);
        Term term2 = new Term("BAR:001", "001", "http://www.example.com/BAR/001", "Bar 1", datasource);
        Mapping mapping = new Mapping();
        mapping.setMappingId(1L);
        mapping.setDatasource(datasource);
        mapping.setFromTerm(term1);
        mapping.setToTerm(term2);


        Page<Mapping> page = new PageImpl<Mapping>(Collections.singletonList(mapping), new PageRequest(1, 20), 60);

        Mockito.when(mappingService.getMappings(new PageRequest(0, 20))).thenReturn(page);

        this.mockMvc.perform(get("/spot/oxo/api/mappings").contextPath("/spot/oxo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }



    @Test
    public void mappingsExample () throws Exception {

        Datasource diseaseOntology = new Datasource("doid", "DOID", "doid", Collections.emptySet(), "Human Disease Ontology", "Human Disease Ontology", SourceType.ONTOLOGY);
        Datasource mondo = new Datasource("mondo", "MONDO", "mondo", Collections.emptySet(), "MONDO: Monarch Disease Ontology", "MONDO: Monarch Disease Ontology", SourceType.ONTOLOGY);
        Term term1 = new Term("DOID:162", "162", "http://purl.obolibrary.org/obo/DOID_162", "cancer", diseaseOntology);
        Term term2 = new Term("MONDO:0004992", "0004992", "http://purl.obolibrary.org/obo/MONDO_0004992", "cancer", mondo);
        Mapping mapping = new Mapping();
        mapping.setMappingId(1L);
        mapping.setDatasource(mondo);
        mapping.setFromTerm(term1);
        mapping.setToTerm(term2);


        this.document.snippets(
                pathParameters(
                        parameterWithName("mapping_id").description("The id of the mapping in OxO, note these identifiers are not stable between releases and should not be used to access mapping directly")),

                responseFields(
                        fieldWithPath("_links").description("<<mappings-links,Links>> to other resources"),
                        fieldWithPath("mappingId").description("The short unique id for the mapping"),
                        fieldWithPath("datasource").description("The datasource that provided the mapping"),
                        fieldWithPath("sourcePrefix").description("The prefix for the datasource that provided this mapping"),
                        fieldWithPath("sourceType").description("The type of datasource for the mapping, allowed values are ONTOLOGY, DATABASE or USER "),
                        fieldWithPath("scope").description("The scope of the mapping indicates some basic semantics for mapping based on BROADER, NARROWER, RELATED or EXACT"),
                        fieldWithPath("predicate").description("If an ONTOLOGY sourcetype this value will contain the predicate used to extract the mapping e.g. obo:dbXref"),
                        fieldWithPath("fromTerm").description("The from term in a mapping, each mapping has a from term and a to term"),
                        fieldWithPath("toTerm").description("The to term in a mapping, each mapping has a from term and a to term"),
                        fieldWithPath("date").description("The date the mapping was extracted from the source")
                ),
                links(halLinks(),
                        linkWithRel("self").description("This mapping"),
                        linkWithRel("fromTerm").description("The from <<resources-term,term>> in the mapping"),
                        linkWithRel("toTerm").description("The to <<resources-term,term>> in the mapping")
                )

        );

        Mockito.when(mappingService.getMapping("1")).thenReturn(mapping);


        this.mockMvc.perform(get("/spot/oxo/api/mappings/{mapping_id}", "1").contextPath("/spot/oxo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
