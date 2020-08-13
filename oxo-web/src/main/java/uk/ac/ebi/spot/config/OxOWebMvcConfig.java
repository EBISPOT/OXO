package uk.ac.ebi.spot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.*;


/**
 * @author Simon Jupp
 * @since 21/03/2017
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

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
//        UrlPathHelper urlPathHelper = new UrlPathHelper();
//               urlPathHelper.setUrlDecode(false);
//               configurer.setUrlPathHelper(urlPathHelper);
        configurer
            .setUseSuffixPatternMatch(false);


    }
    
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/custom/**").addResourceLocations("file:" + System.getProperty("oxo.custom")+"/");
    }
}
