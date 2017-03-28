import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.spot.OxoWebApp;

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

}
