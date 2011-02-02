/*
 * Copyright 2010 Rajendra Patil
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.googlecode.webutilities.filters;

import static com.googlecode.webutilities.common.Constants.DEFAULT_COMPRESSION_SIZE_THRESHOLD;
import static com.googlecode.webutilities.common.Constants.HTTP_ACCEPT_ENCODING_HEADER_VALUES_PATTERN;
import static com.googlecode.webutilities.common.Constants.HTTP_ACCEPT_ENCODING_HEADER;
import static com.googlecode.webutilities.common.Constants.HTTP_USER_AGENT_HEADER;
import static com.googlecode.webutilities.common.Constants.HTTP_VARY_HEADER;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.webutilities.util.Utils;

/**
 * Servlet Filter implementation class GZIPCompressionFilter
 *
 * @author jitendra.takalkar
 * @version 1.0
 * @since 0.0.4
 */
public class GZIPCompressionFilter implements Filter {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(GZIPCompressionFilter.class.getName());

    /**
     * The filter configuration object we are associated with.  If this value
     * is null, this filter instance is not currently configured.
     */
    private FilterConfig config = null;

    /**
     * The threshold number to compress
     */
    private int compressionThreshold = DEFAULT_COMPRESSION_SIZE_THRESHOLD;

    /**
     * Pattern to ignore to perform GZIP
     */
    private String ignoreUserAgentsPattern;

    /**
     * URL Pattern to ignore
     */
    private String ignoreURLPattern;

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) {
        config = filterConfig;
        this.ignoreUserAgentsPattern = filterConfig.getInitParameter("ignoreUserAgentsPattern");
        int compressionMinSize = Utils.readInt(filterConfig.getInitParameter("compressionThreshold"), this.compressionThreshold);
        this.ignoreURLPattern = filterConfig.getInitParameter("ignoreURLPattern");

        if (compressionMinSize > 0) { // priority given to configure value
            this.compressionThreshold = compressionMinSize;
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        this.config = null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        boolean supportCompression = false;
        if (request instanceof HttpServletRequest) {
            String uri = ((HttpServletRequest) request).getRequestURI();
            logger.log(Level.INFO, "requestURI = " + ((HttpServletRequest) request).getRequestURI());
            String userAgent = ((HttpServletRequest) request).getHeader(HTTP_USER_AGENT_HEADER);
            if (isPathIgnored(uri) || isUserAgentIgnored(userAgent)) {
                supportCompression = false;
            } else {
                //Accept-Encoding is sent only once with comma separated values
                String headerValue = ((HttpServletRequest) request).getHeader(HTTP_ACCEPT_ENCODING_HEADER);
                if (headerValue != null && headerValue.matches(HTTP_ACCEPT_ENCODING_HEADER_VALUES_PATTERN)) {
                    logger.log(Level.INFO, "supports compression");
                    supportCompression = true;
                } else {
                    logger.log(Level.INFO, "no support for compresion");
                }
            }
        }

        if (!supportCompression) {
            logger.log(Level.INFO, "doFilter gets called w/o compression");
            chain.doFilter(request, response);
            return;
        } else if (response instanceof HttpServletResponse) {


            //What response Wrapper to choose should be decided based on the value of accept-encoding and choose the best possible ALGO (gzip, deflate, compress or sdch etc.
            GZIPCompressionServletResponseWrapper wrappedResponse =
                    new GZIPCompressionServletResponseWrapper((HttpServletResponse) response);
            wrappedResponse.setCompressionThreshold(compressionThreshold);
            logger.log(Level.INFO, "doFilter gets called with compression");
            try {
                //wrappedResponse.reset();
                chain.doFilter(request, wrappedResponse);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                wrappedResponse.finishResponse();
            }
            return;
        }
    }

    /**
     * Set FilterConfig
     *
     * @param filterConfig The filter configuration object
     */
    public void setFilterConfig(FilterConfig filterConfig) {
        init(filterConfig);
    }

    /**
     * Return FilterConfig
     *
     * @return FilterConfig
     */
    public FilterConfig getFilterConfig() {
        return config;
    }

    /**
     * @param path
     * @return
     */
    public boolean isPathIgnored(String path) {
        return path != null && ignoreURLPattern != null && path.matches(ignoreURLPattern);
    }

    /**
     * @param userAgent
     * @return
     */
    public boolean isUserAgentIgnored(String userAgent) {
        return userAgent != null && ignoreUserAgentsPattern != null && userAgent.matches(ignoreUserAgentsPattern);
    }

}
