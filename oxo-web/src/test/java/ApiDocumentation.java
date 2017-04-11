import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.spot.OxoWebApp;

import javax.servlet.RequestDispatcher;

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
 * @date 20/03/2017
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

    @Before
    public void setUp() {
        this.document = document("{method-name}"
                ,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())
        );

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation).uris()
                                .withScheme("http")
                                .withHost("www.ebi.ac.uk/spot/oxo")
                                .withPort(80)
                )
                .alwaysDo(this.document)
                .build();
    }

    @Test
    public void pageExample () throws Exception {

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

        this.mockMvc.perform(get("/api/ontologies?page=1&size=1"))
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

        this.mockMvc
                .perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI,
                                "/api/ontologies/foobar")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE,
                                "Resource not found"))
        ;
    }

    @Test
    public void apiExample () throws Exception {

        this.document.snippets(
                responseFields(
                        fieldWithPath("_links").description("<<resources-ontologies-links,Links>> to other resources")
                ),
                links(halLinks(),
                        linkWithRel("mappings").description("Link to the mapping in OxO"),
                        linkWithRel("terms").description("Link to all the terms in OxO"),
                        linkWithRel("datasources").description("Link to all the datasources for mappings in OXO"),
                        linkWithRel("profile").description("ALPS is not currently supported")
                        )
        );
        this.mockMvc.perform(get("/api").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    @Test
    public void termsListExample () throws Exception {

        this.mockMvc.perform(get("/api/terms").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }
    @Test
    public void mappingsListExample () throws Exception {

        this.mockMvc.perform(get("/api/mappings").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }
    @Test
    public void datasourcesListExample () throws Exception {

        this.mockMvc.perform(get("/api/datasources").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }


    @Test
    public void mappingsExample () throws Exception {

        this.document.snippets(
                pathParameters(
                        parameterWithName("mapping_id").description("The id of the mapping in OxO, note these identifiers are not stable between releases and should not be used to access mapping directly")),

                responseFields(
                        fieldWithPath("_links").description("<<mappings-links,Links>> to other resources"),
                        fieldWithPath("mappingId").description("The short unique id for the mapping"),
                        fieldWithPath("datasource").description("The datasource that provided the mapping"),
                        fieldWithPath("sourcePrefix").description("The prefix for the datasource that provided this mapping"),
                        fieldWithPath("sourceType").description("The type of datasource for the mapping, allowed values are ONTOLOGY, DATABASE or USER "),
                        fieldWithPath("scope").description("The scope of the mapping indicates some basic seamntics for mapping based on BROADER, NARROWER, RELATED or EXACT"),
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

        this.mockMvc.perform(get("/api/mappings/{mapping_id}", "efo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
