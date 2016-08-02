package uk.ac.ebi.spot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.ac.ebi.spot.loader.Loader;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.MappingSource;
import uk.ac.ebi.spot.service.MappingService;

import java.util.Collection;

@SpringBootApplication
public class OxoApplication implements CommandLineRunner {

	@Autowired
	Collection<Loader> loaders;

	@Autowired
	MappingService mappingService;

//	@Autowired
//	IndexCreator indexCreator;

	public OxoApplication () {


	}

	public static void main(String[] args) {
		SpringApplication.run(OxoApplication.class, args);
	}

	@Override
	public void run(String... strings) throws Exception {

		System.out.println("Loading loaders");
		for (Loader loader : loaders) {

			Collection<MappingSource> mappingSources = loader.load();
			for (MappingSource mappingSource : mappingSources) {
				mappingService.dropMappingsBySource(mappingSource.getDatasource().getPrefix());
			}
			mappingService.getOrCreateMappings(mappingSources);

		}

	}
}
