package uk.ac.ebi.spot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Simon Jupp
 * @date 21/03/2017
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Configuration
public class OxOWebMvcConfig extends WebMvcConfigurerAdapter {
    @Bean
    public ReadOnlyMVHandlerInterceptor getReadOnlyHandlerInterceptor() {
        return new ReadOnlyMVHandlerInterceptor();
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getReadOnlyHandlerInterceptor());

    }
}
