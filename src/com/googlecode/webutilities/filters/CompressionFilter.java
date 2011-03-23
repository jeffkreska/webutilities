/*
 *
 *  Copyright 2011 Rajendra Patil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package com.googlecode.webutilities.filters;

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.filters.common.AbstractFilter;
import com.googlecode.webutilities.filters.compression.CompressedHttpServletRequestWrapper;
import com.googlecode.webutilities.filters.compression.CompressedHttpServletResponseWrapper;
import com.googlecode.webutilities.filters.compression.EncodedStreamsFactory;
import com.googlecode.webutilities.util.Utils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

import static com.googlecode.webutilities.common.Constants.*;

/**
 * Servlet Filter implementation class CompressionFilter to handle compressed requests
 * and also respond with compressed contents supporting gzip, compress or
 * deflate compression encoding.
 *
 * Visit http://code.google.com/p/webutilities/wiki/CompressionFilter for more details.
 *
 * @author rpatil
 * @since 0.0.4
 */
public class CompressionFilter extends AbstractFilter {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(CompressionFilter.class.getName());

    /**
     * The threshold number of bytes) to compress
     */
    private int compressionThreshold = DEFAULT_COMPRESSION_SIZE_THRESHOLD;

    /**
     * To mark the request that it is processed
     */
    private static final String PROCESSED_ATTR = CompressionFilter.class.getName() + ".PROCESSED";

    /**
     * To mark the request that response compressed
     */
    private static final String COMPRESSED_ATTR = CompressionFilter.class.getName() + ".COMPRESSED";

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        
        int compressionMinSize = Utils.readInt(filterConfig.getInitParameter("compressionThreshold"), this.compressionThreshold);
        
        if (compressionMinSize > 0) { // priority given to configured value
            this.compressionThreshold = compressionMinSize;
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        ServletRequest req = getRequest(request);

        ServletResponse resp = getResponse(request, response);

        request.setAttribute(PROCESSED_ATTR, Boolean.TRUE);

        chain.doFilter(req, resp);

        if (resp instanceof CompressedHttpServletResponseWrapper) {

            CompressedHttpServletResponseWrapper compressedResponseWrapper = (CompressedHttpServletResponseWrapper) resp;

            try {

                compressedResponseWrapper.close();  //so that stream is finished and closed.

            } catch (IOException ex) {

                logger.finest("Response was already closed: " + ex);

            }

            if (compressedResponseWrapper.isCompressed()) {

                req.setAttribute(COMPRESSED_ATTR, Boolean.TRUE);

            }

        }

    }

    private ServletRequest getRequest(ServletRequest request) {

        if (!(request instanceof HttpServletRequest)) {
            logger.finest("No Compression: non http request");
            return request;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String contentEncoding = httpRequest.getHeader(HTTP_CONTENT_ENCODING_HEADER);

        if (contentEncoding == null) {
            logger.finest("No Compression: Request content encoding is: " + contentEncoding);
            return request;
        }

        if (!EncodedStreamsFactory.isRequestContentEncodingSupported(contentEncoding)) {
            logger.finest("No Compression: unsupported request content encoding: " + contentEncoding);
            return request;
        }

        return new CompressedHttpServletRequestWrapper(httpRequest, EncodedStreamsFactory.getFactoryForContentEncoding(contentEncoding));

    }

    private String getAppropriateContentEncoding(String acceptEncoding) {
        if(acceptEncoding == null) return null;

        String contentEncoding = null;
        if (CONTENT_ENCODING_IDENTITY.equals(acceptEncoding.trim())) {
            return contentEncoding; //no encoding to be applied
        }

        String[] clientAccepts = acceptEncoding.split(",");

        //!TODO select best encoding (based on q) when multiple encoding are accepted by client
        //@see http://stackoverflow.com/questions/3225136/http-what-is-the-preferred-accept-encoding-for-gzip-deflate
        for(String accepts: clientAccepts){
            if(CONTENT_ENCODING_IDENTITY.equals(accepts.trim())){
                return contentEncoding;
            }else if(EncodedStreamsFactory.SUPPORTED_ENCODINGS.containsKey(accepts.trim())){
                contentEncoding = accepts; //get first matching encoding
                break;
            }
        }
        return contentEncoding;
    }

    private ServletResponse getResponse(ServletRequest request, ServletResponse response) {
        if (response.isCommitted() || request.getAttribute(PROCESSED_ATTR) != null) {
            logger.finest("No Compression: Response committed or filter has already been applied");
            return response;
        }

        if (!(response instanceof HttpServletResponse) || !(request instanceof HttpServletRequest)) {
            logger.finest("No Compression: non http request/response");
            return response;
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String acceptEncoding = httpRequest.getHeader(HTTP_ACCEPT_ENCODING_HEADER);

        String contentEncoding = getAppropriateContentEncoding(acceptEncoding);

        if (contentEncoding == null) {
            logger.finest("No Compression: Accept encoding is : " + acceptEncoding);
            return response;
        }

        String requestURI = httpRequest.getRequestURI();
        if (!isURLAccepted(requestURI)) {
            logger.finest("No Compression: For path: " + requestURI);
            return response;
        }

        String userAgent = httpRequest.getHeader(Constants.HTTP_USER_AGENT_HEADER);
        if (!isUserAgentAccepted(userAgent)) {
            logger.finest("No Compression: For User-Agent: " + userAgent);
            return response;
        }

        EncodedStreamsFactory encodedStreamsFactory = EncodedStreamsFactory.getFactoryForContentEncoding(contentEncoding);

        logger.finest("Using Compression: For content encoding : " + contentEncoding);

        return new CompressedHttpServletResponseWrapper(httpResponse, encodedStreamsFactory, contentEncoding, compressionThreshold, this);
    }

}
