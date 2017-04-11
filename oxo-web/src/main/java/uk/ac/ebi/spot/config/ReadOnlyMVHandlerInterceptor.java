package uk.ac.ebi.spot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Simon Jupp
 * @since 21/03/2017
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class ReadOnlyMVHandlerInterceptor extends HandlerInterceptorAdapter {

    public ReadOnlyMVHandlerInterceptor() {
    }

    @Value("${oxo.access.readonly:false}")
   	boolean readonly;


    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response, final Object handler,
                           final ModelAndView modelAndView) throws Exception {

        // check for an API key

        if (modelAndView != null) {
            modelAndView.getModelMap().addAttribute("readonly", readonly);
        }


    }
}

