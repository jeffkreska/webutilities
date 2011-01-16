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
package com.googlecode.webutilities.yuimin;

import java.io.StringReader;
import java.util.logging.Logger;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.googlecode.webutilities.common.Constants;
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * The <code>YuiMinTag</code> is the JSP custom tag to expose the YUICompressor functionality in the JSP.
 * <p>
 * Using the <code>YuiMinTag</code> inline css, js, json can be compressed.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Put the <b>webutilities-x.y.z.jar</b> and <b>yuicompressor-x.y.z.jar</b> (See dependency mentioned below) in your classpath (WEB-INF/lib folder of your webapp).
 * </p>
 * <p>
 * Delcare the taglib and prefix in your JSP
 * </p>
 * <pre>
 * <b>&lt;%@ taglib uri="http://webutilities.googlecode.com/taglib/yuimin" prefix="ymin" %&gt;</b>
 * </pre>
 * Use the tag to minify your inline JavaScript
 * <pre>
 * <b>&lt;ymin:minify type="js"></b>
 *  	...
 *  	// your inline uncompressed JavaScript
 *  	...
 * <b>&lt;/ymin:minify&gt;</b>
 * </pre>
 * You can also similarly use the tag to minify your inline style
 * <pre>
 * <b>&lt;ymin:minify type="css"></b>
 *  	...
 *  	// your inline uncompressed CSS
 *  	...
 * <b>&lt;/ymin:minify&gt;</b>
 * </pre>
 * <h3>Attributes</h3>
 * <p>
 * The <code>YuiMinTag</code> also supports the different attributes similar to YUICompressor's command line options. Below are the attributes and their explanations.
 * all attrinutes are optional except <b>type</b>.
 * </p>
 * <pre>
 *  <b>type</b> - possible values are js or css. The only required attribute. Usage as shown above.
 *  <b>lineBreak</b> - equivalent to YUICompressor --line-break. Insert a line break after the specified column number
 *  <b>noMunge</b> - equivalent to YUICompressor --nomunge. Minify only, do not obfuscate.
 *  <b>preserveSemi</b> - equivalent to YUICompressor --preserve-semi. Preserve all semicolons.
 *  <b>disableOptimizations</b> - equivalent to YUICompressor --disable-optimizations. Disable all micro optimizations.
 * </pre>
 * <p/>
 * <h3>Dependency</h3>
 * <p>The <code>YuiMinTag</code> depends on jsp-api and YUICompressor jar to be in the classpath.</p>
 * <p><b>jsp-api.jar</b> - Must be already present in your webapp classpath</p>
 * <p><b>yuicompressor-x.y.z.jar</b> - Download and put appropriate version of this jar in your classpath (in WEB-INF/lib)</p>
 * <p/>
 * <h3>Limitations</h3>
 * <p>Current version of <code>YuiMinTag</code> <b>does not support charset</b> option. </p>
 *
 * @author rpatil
 * @version 1.0
 */
public class YuiMinTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;

    //private String charset = "UTF-8";

    private int lineBreak = -1;

    private boolean noMunge = false;

    private boolean preserveSemi = false;

    private boolean disableOptimizations = false;

    private String type;

    private static final Logger logger = Logger.getLogger(YuiMinTag.class.getName());

    public void setType(String type) {
        this.type = type;
    }

//	public void setCharset(String charset) {
//		this.charset = charset;
//	}

    public void setLineBreak(int lineBreak) {
        this.lineBreak = lineBreak;
    }

    public void setNoMunge(boolean noMunge) {
        this.noMunge = noMunge;
    }

    public void setPreserveSemi(boolean preserveSemi) {
        this.preserveSemi = preserveSemi;
    }

    public void setDisableOptimizations(boolean disableOptimizations) {
        this.disableOptimizations = disableOptimizations;
    }

    @Override
    public int doAfterBody() throws JspException {
        BodyContent content = getBodyContent();
        StringReader stringReader = new StringReader(content.getString());
        JspWriter jspWriter = content.getEnclosingWriter();
        try {
            if (stringReader != null) {
                if (Constants.TYPE_JS.equals(type.toLowerCase())) {
                    JavaScriptCompressor compressor = new JavaScriptCompressor(stringReader, null);
                    logger.info("Compressing " + Constants.TYPE_JS);
                    compressor.compress(jspWriter, this.lineBreak, !this.noMunge, false, this.preserveSemi, this.disableOptimizations);
                } else if (Constants.TYPE_CSS.equals(type.toLowerCase())) {
                    CssCompressor compressor = new CssCompressor(stringReader);
                    logger.info("Compressing " + Constants.TYPE_CSS);
                    compressor.compress(jspWriter, this.lineBreak);
                }
            }
        } catch (Exception e) {
            logger.severe("Exception in YUIMinTag: " + e);
            return EVAL_BODY_INCLUDE;
        }
        return SKIP_BODY;
    }
}
