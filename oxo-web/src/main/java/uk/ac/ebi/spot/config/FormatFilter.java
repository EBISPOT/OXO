package uk.ac.ebi.spot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static com.sun.javafx.tools.resource.DeployResource.Type.data;

/**
 * @author Simon Jupp
 * @date 06/10/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 *
 * filter that will detect url params format=tsv or format-csv and set the appropriate request header for content negotiation
 *
 */
@Component
public class FormatFilter implements Filter {
    private Logger log = LoggerFactory.getLogger(getClass());

    protected Logger getLog() {
        return log;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(req);
        if (req.getParameter("format") != null) {
            String format = req.getParameter("format");
            if (format.equals("csv")) {
                requestWrapper.addHeader("Accept", "text/csv");;
            }else if (format.equals("tsv")) {
                requestWrapper.addHeader("Accept", "text/tsv");;
            }

        }
        chain.doFilter(requestWrapper, response); // Goes to default servlet.
    }


    @Override
    public void destroy() {
    }

    public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        /**
         * construct a wrapper for this request
         *
         * @param request
         */
        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        private Map<String, String> headerMap = new HashMap<String, String>();

        /**
         * add a header with given name and value
         *
         * @param name
         * @param value
         */
        public void addHeader(String name, String value) {
            headerMap.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = super.getHeader(name);
            if (headerMap.containsKey(name)) {
                headerValue = headerMap.get(name);
            }
            return headerValue;
        }

        /**
         * get the Header names
         */
        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            for (String name : headerMap.keySet()) {
                names.add(name);
            }
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> values = Collections.list(super.getHeaders(name));
            if (headerMap.containsKey(name)) {
                values.add(headerMap.get(name));
            }
            return Collections.enumeration(values);
        }

    }

}