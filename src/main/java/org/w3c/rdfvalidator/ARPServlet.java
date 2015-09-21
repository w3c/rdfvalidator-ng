/***********************************************************************
 *
 * ARPServlet - this servlet implements an RDF Validation service.  As
 *  of this writing, the following RDF validation service used this
 *  servlet:
 *
 *   http://www.w3.org/RDF/Validator/
 *
 ***********************************************************************
 *
 * Copyright (c) World Wide Web Consortium, (Massachusetts Institute of
 * Technology, Institut National de Recherche en Informatique et en
 * Automatique, Keio University).
 *
 * All Rights Reserved.
 *
 * Please see the full Copyright clause at
 * <http://www.w3.org/Consortium/Legal/copyright-software.html>
 *
 ***********************************************************************
 *
 * This servlet is a wrapper for the ARP RDF parser.  See the following
 * for information about the ARP RDF parser:
 *
 *  http://www.hpl.hp.co.uk/people/jjc/arp/
 *
 ***********************************************************************
 *
 * Implementation notes:
 *
 * o This servlet supports the HTTP POST operation; it does not 
 *  support the HTTP GET operation
 *
 * o Depending upon the parameters given to the servlet it may 
 *  invoke a GraphViz suprocess to generate a graph of the RDF.  
 *  See the following for more information about GraphViz:
 *
 *   http://www.research.att.com/sw/tools/graphviz/
 *
 *  The servlet assumes version 1.7.4 of GraphViz.
 *
 * o Depending upon the parameters given to the servlet, the RDF
 *  to be validated may be copied to a file.  The name of the file
 *  is automatically generated via Java's temporary file APIs.  The
 *  location of the directory where the file is stored is configured
 *  via the serverlet's init() method.  See below for more information.
 *
 * o See the section on Server Initialization for more information.
 *
 ***********************************************************************
 *
 * HTTP POST parameters - the servlet expects/assumes the following 
 *  variables are defined via the HTTP POST request:
 *
 * RDF - the RDF (assumed to be in RDF/XML syntax) to be validated
 *
 * SAVE_DOT_FILE - if "on", the GraphViz DOT file is saved and a 
 *   link to the file is returned; otherwise the DOT file is not saved
 *
 * SAVE_RDF - if "on", the RDF will be copied to a file; otherwise
 *   the RDF is not copied to a file
 *
 * EMBEDDED_RDF - if "on", then the RDF is not enclosed in <RDF>...</RDF>
 *   tags; otherwise it assumed that the RDF is enclosed in these tags.
 *
 * URI - the URI of the RDF to validate
 *
 * PARSE - if "Parse RDF", then parse RDF from the textarea;
 *   else download from URI; if not present, prefer URI,
 *   but if URI is empty, parse RDF (old behavior).
 *
 * ORIENTATION - the graph's orientation (left to right or top to
 *   bottom); default is left to right
 *
 * FONT_SIZE - the font size to use (10, 12, 14, 16 and 20 are 
 *   supported); the default is 10
 *
 * NODE_COLOR - the color of nodes; default is black
 *
 * NODE_TEXT_COLOR - the color of the text in nodes; default is blue
 *
 * EDGE_COLOR - the color of edges; default is darkgreen
 *
 * EDGE_TEXT_COLOR - the color of the text on edges; default is red
 *
 * ANON_NODES_EMPTY - if "on", anonymous nodes are not labeled; otherwise
 *   anonymous nodes are labeled;
 *
 * TRIPLES_AND_GRAPH - support values are:
 *
 *     PRINT_BOTH - display triples and a graph (the default)
 *     PRINT_TRIPLES - only display the triples
 *     PRINT_GRAPH - only display the graph
 *
 * FORMAT - the graph's output format.  Supported values are:
 *
 *     GIF_EMBED - embed the graph as a GIF
 *     GIF_LINK - don't embed the GIF but create a link for it
 *     SVG_LINK - create the graph in SVG format and create a link to the file
 *     SVG_EMBED - create the graph in SVG format and embed it in an object tag
 *     ISV_ZVTM - IsaViz/ZVTM (Dynamic View - requires Java Plug-in 1.4)
 *     PNG_EMBED - create the graph in PNG format and embed the graph in the 
 *       document that is returned (the default)
 *     PNG_LINK - create the graph in PNG format and create a link to the file
 *     PS_LINK - create a PostScript image of the file and a link to the file
 *     HP_PCL_LINK - create a HPGL/2 - PCL (Laserwriter) image of the file 
 *       and a link to the file
 *     HP_GL_LINK - create a HPGL - PCL (pen plotter) image of the file and 
 *       a link to the file
 *
 * NTRIPLES if "on" the tabular output will be in the NTriples format;
 *  otherwise a table of Subject, Predicate, Objects will be generated
 *
 ***********************************************************************
 *
 * Server Initialization - this servlet requires the following 
 *  parameters be set in the servlet's init() method - via the
 *  ServletConfig object:
 *
 * BITMAPPED_FONT - the absolute path of the top-level directory containing
 *   GraphViz's binary distribution
 *
 * VECTOR_FONT - absolute or relative (based on GRAPH_VIZ_ROOT) path of
 *   the DOT executable (e.g. dotneato/dot) - the program used to generate
 *   a graph from a DOT file.
 *
 * SERVLET_TMP_DIR - the absolute path of the directory to be used to
 *   store temporary files used by the servlet and GraphViz.  This 
 *   directory must be writable by the servlet.  
 *
 *   NOTE - Some files created by the servlet are not removed by 
 *     servlet (e.g. graph image files).
 *
 * If any of these parameters are not defined, the servlet will NOT 
 * validate the RDF.
 *
 ***********************************************************************
 *
 * Dependencies - this servlet requires the following Java packages
 *   as well as GraphViz (described above):
 *
 * ARP RDF parser: http://www.hpl.hp.co.uk/people/jjc/arp/download.html
 *
 * SAX-based XML parser: e.g. Xerces at http://xml.apache.org/
 *
 * Java servlet package: http://java.sun.com/products/servlet/archive.html
 *
 * Apache Regular Expression: http://jakarta.apache.org/builds/jakarta-regexp/release/v1.2/
 *
 ***********************************************************************
 *
 * Author: Art Barstow <barstow@w3.org>
 * Author (internationalization): Martin J. Duerst <duerst@w3.org>
 * Author (maintenance): Emmanuel Pietriga <emmanuel@w3.org>
 *
 * $Id: ARPServlet.java,v 1.5 2006/09/17 23:43:32 ted Exp $
 *
 ***********************************************************************/

// http://dev.w3.org/cvsweb/java/classes/org/w3c/rdf/examples/
package org.w3c.rdfvalidator; 

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Hashtable;

// http://java.sun.com/products/servlet/2.2/javadoc/javax/servlet/package-summary.html
import javax.servlet.*;	 
import javax.servlet.http.*;
import javax.mail.internet.ContentType;

// http://xml.apache.org/apiDocs/org/xml/sax/package-summary.html
import org.xml.sax.InputSource;	
//import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.helpers.*;

// http://jakarta.apache.org/regexp/apidocs/org/apache/regexp/RE.html
import java.util.regex.*;

// http://www.hpl.hp.co.uk/people/jjc/arp/apidocs/index.html
import com.hp.hpl.jena.rdf.arp.*; 
import com.hp.hpl.jena.rdf.arp.impl.RDFXMLParser;

import com.sun.net.ssl.internal.www.protocol.https.*;
import java.security.Security;

public class ARPServlet extends HttpServlet
{
    final static public String	REVISION = "$Id: ARPServlet.java,v 1.5 2006/09/17 23:43:32 ted Exp $";

    // The email address for bug reports
    private static final String MAIL_TO = "www-rdf-validator@w3.org";

    // Names of the POST parameters (described above) and their
    // defaults (if applicable)
    private static final String TEXT              = "RDF";
    private static final String SAVE_DOT_FILE     = "SAVE_DOT_FILE";
    private static final String SAVE_RDF          = "SAVE_RDF";
    private static final String EMBEDDED_RDF      = "EMBEDDED_RDF";
    private static final String URI               = "URI";
    private static final String PARSE             = "PARSE";
    private static final String NTRIPLES          = "NTRIPLES";
    private static final String ANON_NODES_EMPTY  = "ANON_NODES_EMPTY";
 
    private static final String NODE_COLOR         = "NODE_COLOR";
    private static final String DEFAULT_NODE_COLOR = "black";

    private static final String NODE_TEXT_COLOR         = "NODE_TEXT_COLOR";
    private static final String DEFAULT_NODE_TEXT_COLOR = "blue";

    private static final String EDGE_COLOR         = "EDGE_COLOR";
    private static final String DEFAULT_EDGE_COLOR = "darkgreen";

    private static final String EDGE_TEXT_COLOR         = "EDGE_TEXT_COLOR";
    private static final String DEFAULT_EDGE_TEXT_COLOR = "red";

    private static final String ORIENTATION         = "ORIENTATION";
    private static final String DEFAULT_ORIENTATION = "LR";  // Left to Right

    private static final String FONT_SIZE         = "FONT_SIZE";
    private static final String DEFAULT_FONT_SIZE = "10";

    // Print graph and/or triples
    private static final String TRIPLES_AND_GRAPH = "TRIPLES_AND_GRAPH";
    private static final String PRINT_BOTH        = "PRINT_BOTH";
    private static final String PRINT_TRIPLES     = "PRINT_TRIPLES";
    private static final String PRINT_GRAPH       = "PRINT_GRAPH";

    // Graph formats
    private static final String FORMAT              = "FORMAT";
    private static final String FORMAT_GIF_EMBED    = "GIF_EMBED";
    private static final String FORMAT_GIF_LINK     = "GIF_LINK";
    private static final String FORMAT_SVG_LINK     = "SVG_LINK";
    private static final String FORMAT_SVG_EMBED     = "SVG_EMBED";
    private static final String FORMAT_ISV_ZVTM     = "ISV_ZVTM";
    private static final String FORMAT_PNG_EMBED    = "PNG_EMBED";
    private static final String FORMAT_PNG_LINK     = "PNG_LINK";
    private static final String FORMAT_PS_LINK      = "PS_LINK";
    private static final String FORMAT_HP_PCL_LINK  = "HP_PCL_LINK";
    private static final String FORMAT_HP_GL_LINK   = "HP_GL_LINK";
    private static final String DEFAULT_FORMAT      = "PNG_EMBED";

    // Take a guess at what fonts to use of BITMAPPED_FONT and
    // VECTOR_FONT are not set.
    private static String m_BitmappedFont = "cyberbit";
    private static String m_VectorFont = "Courier";

    // Names of the servlet's parameters - for Jigsaw web server
    private static final String SERVLET_TMP_DIR    = "SERVLET_TMP_DIR";

    private static String m_RenderDot    = null;

    // Variables for the servlet's parameters
    private static String m_ServletTmpDir   = null;

    // Names of environment variable needed by GraphVis
    private static String LD_LIBRARY_PATH = "LD_LIBRARY_PATH";

    // Names used for temporary files
    private static final String TMP_FILE_PREFIX = "servlet_";
    private static final String SUFFIX_TMP_DIR  = ".tmp";
    private static final String SUFFIX_DOT      = ".dot";
    private static final String SUFFIX_RDF      = ".rdf";

    // Names used for file suffixes and for GraphViz's command line
    // option
    private static final String NAME_GIF      = "gif";
    private static final String NAME_HPGL     = "hpgl";
    private static final String NAME_PCL      = "pcl";
    private static final String NAME_PNG      = "png";
    private static final String NAME_PS       = "ps";
    private static final String NAME_SVG      = "svg";

    // Default GraphViz parameter names and their default values
    // Servlet name
    private static final String SERVLET_NAME = "ARPServlet";

    // Name for the DOT file title
    private static final String DOT_TITLE = "dotfile";

    // The string to use to prefix anonymous nodes.
    private static final String ANON_NODE = "genid:";

    // The string to use for a namespace name when no
    // namespace is available - e.g. for the RDF that is
    // directly entered into the input form.
    private static final String DEFAULT_NAMESPACE = "http://www.w3.org/RDF/Validator/run/";

    //used to detect whether the provided document contains at least one triple, or if it is not RDF at all
    //necessary because ARP does not report any error when parsing an XML document which does not contain any
    //RDF statement
    static boolean AT_LEAST_ONE_TRIPLE=false;

    // Setting this to {1, true, yes} prints messages to STDERR
    static Pattern LogALotPattern = Pattern.compile("^[ \\t\\n\\r]?(1|t.*|y.*)?");
    static boolean LogALot = false;

    // Regular expression for parsing an XML prolog to look for the charset.
    static Pattern XMLProlog = Pattern.compile("<\\?xml[ \\t\\n\\r]+version[ \\t\\n\\r]?=[ \\t\\n\\r]?(['\"])([a-zA-Z0-9_:]|\\.|-)+\\1[ \\t\\n\\r]+encoding[ \\t\\n\\r]?=[ \\t\\n\\r]?(['\"])([A-Za-z]([A-Za-z0-9._]|-)*)\\3");

    //colors for ISV-plugin texts (resources, properties and literals)
    private static float resTBh=0.33333334f;
    private static float resTBs=0.37142858f;
    private static float resTBv=0.4117647f;
    private static float prpTh=0.6680911f;
    private static float prpTs=0.56796116f;
    private static float prpTv=0.80784315f;
    private static float litTBh=0.12878788f;
    private static float litTBs=0.5f;
    private static float litTBv=0.5176471f;

    // exception used by getRDFfromURI
    private class getRDFException extends Exception {
        public getRDFException (String s) {
	    super (s);
        }
    }

    /*
     * Create a File object from the given directory and file names
     *
     *@param directory the file's directory
     *@param prefix the file's prefix name (not its directory)
     *@param suffix the file's suffix or extension name
     *@return a File object if a temporary file is created; null otherwise
     */
    private File createTempFile (String directory, String prefix, String suffix) 
    {
        File f;
        try {
            File d = new File(directory);
            f = File.createTempFile(prefix, suffix, d);
        } catch (Exception e) {
            return null;
        }
        return f;
    }

    /*
     * Given a URI string, open it, read its contents into a String
     * and return the String
     *
     *@param uri the URI to open
     *@return the content at the URI or null if any error occurs
     */
    private String getRDFfromURI (String uri) throws getRDFException
    {
	/* add something like this code here, to allow reading from a file:
	   (if we really want to allow this!)
	   File ff = new File(uri);
	   in = new FileInputStream(ff);
	*/

        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
	
	URL url = null;
	try {
	    url = new URL(uri);
	} catch (MalformedURLException e) {
	    throw new getRDFException("Malformed URI.");
	}

        URLConnection con = null;
	try {
	    con = url.openConnection();
	    con.setRequestProperty("Accept", "application/rdf+xml");
	    con.connect();
	} catch (Exception e) {
	    throw new getRDFException("Unable to open connection.");
	}
	String contentT = con.getContentType();
	String HTTPcharset = null;
	if (contentT != null) {
	    ContentType contentType = null;
	    try {
		contentType = new ContentType(con.getContentType());
	    } catch (javax.mail.internet.ParseException e) {
		throw new getRDFException("Unparsable content type.");
	    }
	    HTTPcharset = contentType.getParameter("charset");
	}
	
	// need buffer for lookahead for encoding detection
	BufferedInputStream bis = null;
	try {
	    bis = new BufferedInputStream(con.getInputStream());
	} catch (IOException e) {
	    throw new getRDFException("Cannot open stream.");
	}
	bis.mark(200); // mark start so that we can get back to it
	String s = "";
	
	try {  // read start of file as bytes
	    int c;
	    int numRead = 0;
	    while ((c = bis.read()) != -1) {
		s += (char)c;
		if (numRead++ >= 195) break;
	    }
	} catch (IOException e) {
	    throw new getRDFException("IOException while starting reading.");
	}
	
	if (s.equals(""))
	    // Nothing was returned 
	    throw new getRDFException("Empty document, ignored.");
	
	// A server could return content but not the RDF/XML that
	// we need.  Check the beginning of s and if it looks like
	// a generic HTML message, return an error.
	if (s.startsWith("<!DOCTYPE"))
	    throw new getRDFException("Document looks like HTML, ignored.");

	String APPFcharset = null;  // 'charset' according to XML APP. F
	int ignoreBytes = 0;
	if (s.startsWith("\u00FE\u00FF")) {
	    APPFcharset = "UTF-16BE";
	    ignoreBytes = 2;
	}
	else if (s.startsWith("\u00FF\u00FE")) {
	    APPFcharset = "UTF-16LE";
	    ignoreBytes = 2;
	}
	else if (s.startsWith("\u00EF\u00BB\u00BF")) {
	    APPFcharset = "UTF-8";
	    ignoreBytes = 3;
	}
	else if (s.startsWith("\u0000<\u0000?")) {
	    APPFcharset = "UTF-16BE";
	}
	else if (s.startsWith("<\u0000?\u0000")) {
	    APPFcharset = "UTF-16LE";
	}
	else if (s.startsWith("<?xml")) {
	    APPFcharset = "iso-8859-1";  //to not loose any bytes 
	}
	else if (s.startsWith("\u004C\u006F\u00A7\u0094")) {
	    APPFcharset = "CP037";  // EBCDIC
	}
	else {
	    APPFcharset = "iso-8859-1";  //to not loose any bytes
	}
	
	// convert start of xml input according to APPFcharset
	String xmlstart = null;
	try {
// 	    System.err.println("---------------------------");
// 	    System.err.println("ignoreBytes="+ignoreBytes);
// 	    System.err.println("s="+s);
// 	    System.err.println("APPFcharset="+APPFcharset);
// 	    if (APPFcharset!=null){xmlstart = new String(s.substring(ignoreBytes).getBytes("iso-8859-1"), APPFcharset);}
// 	    else {xmlstart=new String(s.substring(ignoreBytes).getBytes("iso-8859-1"));APPFcharset = "UTF-8";}
	    xmlstart = new String(s.substring(ignoreBytes).getBytes("iso-8859-1"), APPFcharset);
	} catch (UnsupportedEncodingException e) {
	    throw new getRDFException("Unsupported encoding '"+APPFcharset+"'.");
	}
	String XMLcharset = null;
	Matcher m = XMLProlog.matcher(xmlstart);
	if (m.find())
	    XMLcharset = m.group(4);
	if (HTTPcharset != null)
	    HTTPcharset = HTTPcharset.toUpperCase(); 
	if (XMLcharset != null)
	    XMLcharset = XMLcharset.toUpperCase(); 

	String finalCharset = null;
	if (HTTPcharset != null) { 
	    if (XMLcharset != null && !HTTPcharset.equals(XMLcharset)) 
		throw new getRDFException("Charset conflict: Content-Type: "
                    + contentT+ ". XML encoding: " +  XMLcharset + ".");
	    finalCharset = HTTPcharset; 
	} 
	else if (XMLcharset != null) 
	    finalCharset = XMLcharset;
	if ((finalCharset != null && finalCharset.equals("UTF-16")) ||
	        (finalCharset == null && APPFcharset.startsWith("UTF-16")))
	    if (ignoreBytes == 2)
		finalCharset = APPFcharset;  // use correct endianness
	    else
		throw new getRDFException("Illegal XML: UTF-16 without BOM.");
	if (finalCharset == null)
	    finalCharset = "UTF-8";

	try {
	    bis.reset();                 // move back to start of stream 
	    bis.skip(ignoreBytes);       // skip BOM 
	} catch (IOException e) {
	    throw new getRDFException("IOException while resetting stream.");
	}

	InputStreamReader isr = null; 
	try {
	    isr = new InputStreamReader(bis, finalCharset); 
	} catch (UnsupportedEncodingException e) {
	    throw new getRDFException("Unsupported encoding '"+finalCharset+"'.");
	}
	StringBuffer sb=new StringBuffer("");
	int bytenum=0;
	try {// read whole file as characters
	    int c;
	    while ((c = isr.read()) != -1) {
		sb.append((char)c);
		bytenum++;
	    }
	} 
	catch (IOException e){
	    throw new getRDFException("Undecodable data when reading URI at byte "+bytenum+" using encoding '"+finalCharset+"'."+" Please check encoding and encoding declaration of your document.");
	}
	// todo: fix encoding parameter in xml pseudo-PI
	return sb.toString();
    }

    /*
     * Copy the given string of RDF to a file in the given directory.
     * This is only done if the servlet is explictly asked to save
     * the RDF to a file.
     *
     *@param tmpDir the file's directory
     *@param rdf the string of RDF
     */
    private void copyRDFStringToFile(String tmpDir, String rdf) 
    {
	File tmpFile = null;
        try {
            // Generate a unique file name 
            tmpFile = createTempFile(tmpDir, TMP_FILE_PREFIX, SUFFIX_RDF);
            if (tmpFile == null) {
                // Not really a critical error, just return
                return;
            }

            // Create a PrintWriter for the GraphViz consumer
            FileWriter fw = new FileWriter(tmpFile);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(rdf);
            pw.close();
        } catch (Exception e) {
            System.err.println(SERVLET_NAME + ": error occured trying to save RDF to file '" + tmpFile + "'.");
            return;
        }
    }

    /*
     * Given the graph's format option, return either the corresponding
     * command line option for that option or the file name suffix for
     * the graph option.  For example GIF files have ".gif" for its
     * suffix and GraphViz uses "-Tgif" for the command line.
     *
     * NOTE: default is PNG.
     *
     *@param graphFormat the graph's output format
     *@param suffix.  If true, the name returned is for the graph's
     * file name suffix; otherwise, the name returned is for the
     * graph's command line option.
     *@return the suffix to use for the graph's output file
     */
    private String getFormatName(String graphFormat, boolean suffix) {

        String name = (suffix) ? "." : "-T";

        if (graphFormat.equals(FORMAT_PNG_EMBED))   return name + NAME_PNG;
        if (graphFormat.equals(FORMAT_PNG_LINK))    return name + NAME_PNG;
        if (graphFormat.equals(FORMAT_GIF_EMBED))   return name + NAME_GIF;
        if (graphFormat.equals(FORMAT_GIF_LINK))    return name + NAME_GIF;
        if (graphFormat.equals(FORMAT_SVG_LINK))    return name + NAME_SVG;
	if (graphFormat.equals(FORMAT_SVG_EMBED))   return name + NAME_SVG;
        if (graphFormat.equals(FORMAT_ISV_ZVTM))    return name + NAME_SVG;
        if (graphFormat.equals(FORMAT_PS_LINK))     return name + NAME_PS;
        if (graphFormat.equals(FORMAT_HP_GL_LINK))  return name + NAME_HPGL;
        if (graphFormat.equals(FORMAT_HP_PCL_LINK)) return name + NAME_PCL;
        
        return name + NAME_PNG;
    }

    /*
     * Invokes the GraphVis program to create a graph image from the
     * the given DOT data file
     *
     *@param dotFileName the name of the DOT data file
     *@param outputFileName the name of the output data file 
     *@param graphFormat the graph's format
     *@return true if success; false if any failure occurs
     */
    private int generateGraphFile(String dotFileName, 
	String outputFileName, String graphFormat) 
    {
        String environment[] = {};

        String formatOption = getFormatName(graphFormat, false);

        // String cmdArray[] = {m_GraphVizPath, formatOption, "-o", outputFileName, dotFileName};
        String cmdArray[] = {m_RenderDot, formatOption, outputFileName, dotFileName};
	if (LogALot) {
	    // System.err.println(cmdArray);
	    System.err.println(m_RenderDot + " " + formatOption + " " + outputFileName + " " + dotFileName);
	}

        Runtime rt = Runtime.getRuntime();
	Process p;
        try {
            p = rt.exec(cmdArray, environment);
            p.waitFor();

        } catch (Exception e) {
            System.err.println("Error: generating OutputFile.");
            return -1;
        }
	return p.exitValue();
    }

    /*
     * Returns a parameter from a request or the parameter's default
     * value.
     *
     *@param req a Servlet request
     *@param param the name of the parameter
     *@param defString the string returned if the param is not found
     *@return if the request contains the specfied parameter its value
     *  in the request is returned; otherwise its default value is
     *  returned
     */
    private String getParameter(HttpServletRequest req, String param, 
        String defString) 
    {
        String s = req.getParameter(param);
        return (s == null) ? defString : s;
    }

    /*
     * If the request contains any graph-related parameters, pass them
     * to the graph consumer for handling
     *
     *@param req the response
     *@param pw the PrintWriter
     *@param consumer the GraphViz consumer
     *@param bitmap true=generate a bitmap (GIF or PNG), false=generate PostScript, SVG, etc.
     */
    private void processGraphParameters (HttpServletRequest req, PrintWriter pw,boolean bitmap)
    {
	// Print the graph header
        pw.println("digraph " + DOT_TITLE + "{ " );

        // Look for colors
        String nodeColor     = getParameter(req, NODE_COLOR, 
					    DEFAULT_NODE_COLOR);
        String nodeTextColor = getParameter(req, NODE_TEXT_COLOR, 
					    DEFAULT_NODE_TEXT_COLOR);
        String edgeColor     = getParameter(req, EDGE_COLOR, 
					    DEFAULT_EDGE_COLOR);
        String edgeTextColor = getParameter(req, EDGE_TEXT_COLOR, 
					    DEFAULT_EDGE_TEXT_COLOR);
        String fontSize      = getParameter(req, FONT_SIZE, 
					    DEFAULT_FONT_SIZE);

        // Orientation must be either LR or TB
        String orientation = req.getParameter (ORIENTATION);
        if (orientation == null || !orientation.equals("TB"))
            orientation = DEFAULT_ORIENTATION;
	String font = bitmap ? m_BitmappedFont : m_VectorFont;

        // Add an attribute for all of the graph's nodes
        pw.println("node [fontname=\"" + font + 
                   "\",fontsize="  + fontSize +
                   ",color="     + nodeColor +
                   ",fontcolor=" + nodeTextColor + "];");

        // Add an attribute for all of the graph's edges
        pw.println("edge [fontname=\"" + font + 
                   "\",fontsize="  + fontSize +
                   ",color="     + edgeColor +
                   ",fontcolor=" + edgeTextColor + "];");

        // Add an attribute for the orientation
        pw.println("rankdir=" + orientation + ";");
    }

    private static class SaxErrorHandler implements org.xml.sax.ErrorHandler
    { 
        PrintWriter out;
        boolean silent = false;
	String fatalErrors = "";
	String errors = "";
	String warnings = "";
	String datatypeErrors="";

        /*
         * Constructuor for a SaxErrorHandler 
         *
	 *@param out the servlet's PrintWriter
	 *@param silent if false, output is suprressed
         */
        public SaxErrorHandler(PrintWriter out, boolean silent) 
        {
            this.out = out;
            this.silent = silent;
        }

        /*
         * Create a formatted string from the exception's message
         *
	 *@param e the SAX Parse Exception
	 *@return a formatted string
         */
        private static String format(org.xml.sax.SAXParseException e) 
        {
            String msg = e.getMessage();
            if (msg == null)
                msg = e.toString();
	    msg = replaceString(msg,"&","&amp;");
	    msg = replaceString(msg,"<","&lt;");
	    msg = replaceString(msg,">","&gt;");
	    msg = replaceString(msg,"\"","&quot;");
	    msg = replaceString(msg,"'","&apos;");
            return msg + "[Line = " + e.getLineNumber() + ", Column = " + e.getColumnNumber() + "]";
        }

        /*
         * Handle a parse error
         *
	 *@param e the SAX Parse Exception
	 */
        public void error(org.xml.sax.SAXParseException e) 
            throws org.xml.sax.SAXException 
        {
            if (this.silent) return;

	    if (e instanceof com.hp.hpl.jena.rdf.arp.ParseException){
		com.hp.hpl.jena.rdf.arp.ParseException pe=(com.hp.hpl.jena.rdf.arp.ParseException)e;
// 		if (pe.getErrorNumber()==com.hp.hpl.jena.rdf.arp.ARP.WARN_NOT_SUPPORTED && pe.getMessage().indexOf("datatyping")!=-1){
// 		    datatypeErrors+=String.valueOf(pe.getLineNumber())+", ";
// 		}
// 		else {
		    this.errors += "Error: " + format(e) + "<br />";
// 		}
	    }
	    else {this.errors += "Error: " + format(e) + "<br />";}
        }
    
        /*
         * Handle a fatal parse error
         *
	 *@param e the SAX Parse Exception
	 */
        public void fatalError(org.xml.sax.SAXParseException e) 
            throws org.xml.sax.SAXException 
        {
            if (this.silent) return;

	    this.fatalErrors += "FatalError: " + format(e) + "<br />";
        }
    
        /*
         * Handle a parse warning
         *
	 *@param e the SAX Parse Exception
	 */
        public void warning(org.xml.sax.SAXParseException e) 
            throws org.xml.sax.SAXException 
        {
            if (this.silent) return;

	    if (e instanceof com.hp.hpl.jena.rdf.arp.ParseException){
// 		com.hp.hpl.jena.rdf.arp.ParseException pe=(com.hp.hpl.jena.rdf.arp.ParseException)e;
// 		if (pe.getErrorNumber()==com.hp.hpl.jena.rdf.arp.ARP.WARN_NOT_SUPPORTED && pe.getMessage().indexOf("datatyping")!=-1){
// 		    datatypeErrors+=String.valueOf(pe.getLineNumber())+", ";
// 		}
// 		else {
		    this.warnings += "Warning: " + format(e) + "<br />";
// 		}
	    }
	    else {this.errors += "Warning: " + format(e) + "<br />";}
        }

        /*
         * Return the error messages
         *
	 *@return the error messages or an empty string if there are
	 * no messages
	 */
	public String getErrors()
	{
	   return this.errors;
	}

	public String getDatatypeErrors()
	{
	   return this.datatypeErrors;
	}

        /*
         * Return the fatal error messages
         *
	 *@return the fatal error messages or an empty string if there are
	 * no messages
	 */
	public String getFatalErrors()
	{
	   return this.fatalErrors;
	}

        /*
         * Return the warning messages
         *
	 *@return the warning messages or an empty string if there are
	 * no messages
	 */
	public String getWarnings()
	{
	   return this.warnings;
	}
    } 

    /*
     * Generate a graph of the RDF data model
     *
     *@param out the servlet's output Writer
     *@param pw the graph file's PrintWriter
     *@param dotFile the File handle for the graph file
     *@param rdf the RDF text
     *@param req a Servlet request
     *@param graphFormat the graph's format
     *@param saveRDF the RDF can be cached [saved to the file system]
     *@param saveDOTFile the DOT file should be cached
     */
    private void generateGraph(PrintWriter out, PrintWriter pw,
	File dotFile, String rdf, HttpServletRequest req, String graphFormat, 
        boolean saveRDF, boolean saveDOTFile) 
    {
        try {
            out.println("<hr title='visualisation' />");
            out.println("<h3><a name='graph' id='graph'>" +
                        "Graph of the data model</a></h3>");

            // The temporary directory
            String tmpDir = m_ServletTmpDir;

            // Add the graph footer
            pw.println( " }");

            // Close the DOT input file so the GraphViz can
            // open and read it
            pw.close();

            // Generate a unique file name for the output file
            // that will be created
            String suffix = getFormatName(graphFormat, true);
            File outputFile = createTempFile(tmpDir, TMP_FILE_PREFIX, suffix);
            if (outputFile == null) {
                out.println("Failed to create a temporary file for the graph. A graph cannot be generated.");
                dotFile.delete();
                return;
            }

            // Pass the DOT data file to the GraphViz dot program
            // so it can create a graph image of the data model
            String dotFileName = dotFile.getAbsolutePath();
            String outputFileName = outputFile.getAbsolutePath();
	    int grapvizExitValue=generateGraphFile(dotFileName, outputFileName, graphFormat);
	    if (grapvizExitValue != 0) {		
		//from sysexits.h, sent by renderDot http://dev.w3.org/cvsweb/~checkout~/2006/RDFValidator/renderDot
		//#define EX_TEMPFAIL75/* temp failure; user is invited to retry */
		if (grapvizExitValue == 75) {
		    out.println("We're sorry but the server load of the RDF validator is too high at the moment, and generation of the graph image files has been canceled.  Please try again later.");
		}
		else if (grapvizExitValue > 150) {
		    out.println("The resources necessary to create this graph exceed size, cpu or memory restraints imposed for graphics generation on this server.");
		}
		else {
		    out.println("An attempt to create a graph failed.");
		}
                dotFile.delete();
                outputFile.delete();
                return;
            }
            // Handle the DOT file
            if (saveDOTFile) {
                // Make the DOT file link'able if so requested
                String dotPath = SERVLET_NAME + SUFFIX_TMP_DIR + 
                                 File.separator + dotFile.getName();
                out.println("<a href=\"" + dotPath + "\">Download the DOT file.</a><br /><br />");
            }
            else {
                // Delete it ...
                dotFile.delete();
            }

            // NOTE: Cannot delete the output file here because its
            // pathname is returned to the client
            String imagePath = SERVLET_NAME + SUFFIX_TMP_DIR + File.separator + 
                               outputFile.getName();

            // Handle the embedded image formats first
            if (graphFormat.equals(FORMAT_GIF_EMBED) ||
                graphFormat.equals(FORMAT_PNG_EMBED)) {
                if (outputFile.length() > 0)
                    out.println("<img alt='graph representation of RDF data' " +
                                "src='" + imagePath + "'/>");
                else
                    out.println("The graph image file is empty.");
            } 
	    else if (graphFormat.equals(FORMAT_SVG_EMBED)){
		if (outputFile.length() > 0){
		    out.println("<object type=\"image/svg+xml\" name=\"rdfsvg\" data=\"http://www.w3.org/RDF/Validator/"+imagePath+"\" width=\"640\" height=\"480\">Your browser does not support the &lt;object&gt; tag. The SVG representation of the model cannot be embedded in this page. You can use <b>SVG - link</b> or update your browser to a version supporting the &lt;object&gt; tag.</object>");
		}
                else
                    out.println("The graph image file is empty.");
	    }
	    else if (graphFormat.equals(FORMAT_ISV_ZVTM)){
		if (outputFile.length() > 0){
		    out.println("<applet code=\"org.w3c.IsaViz.applet.IsvBrowser.class\"");
		    out.println("archive=\"lib/zvtm.jar,lib/isvapp.jar,lib/xercesImpl.jar,lib/xmlParserAPIs.jar\"");
		    out.println("width=\"640\" height=\"480\">");
		    out.println("<param name=\"type\" value=\"application/x-java-applet;version=1.4\" />");
		    out.println("<param name=\"scriptable\" value=\"false\" />");
		    out.println("<param name=\"width\" value=\"640\" />");
		    out.println("<param name=\"height\" value=\"480\" />");
		    out.println("<param name=\"svgFile\" value=\"http://www.w3.org/RDF/Validator/"+imagePath+"\" />");
		    out.println("</applet>");
		}
                else
                    out.println("The graph image file is empty.");
	    }
	    else {
                if (outputFile.length() > 0)
                    out.println("<a href=\"" + imagePath + "\">Get/view the graph's image file (" + suffix + ").</a><br /><br />");
                else
                    out.println("The graph image file is empty.");
            }

            // One last thing to do before exiting - copy the RDF to a file
            if (saveRDF)
                copyRDFStringToFile(tmpDir, rdf);

        } catch (Exception e) {
            System.err.println("Exception generating graph: " + e.getMessage());
        }
    }

    /*
     * Search the given string for substring "key"
     * and if it is found, replace it with string "replacement"
     *
     *@param input the input string
     *@param key the string to search for
     *@param replacement the string to replace all occurences of "key"
     *@return if no substitutions are done, input is returned; otherwise
     * a new string is returned.
     */
    public static String replaceString(String input, String key, 
        String replacement) 
    {
	return input.replaceAll(key, replacement);
    }

    /*
     * Print the document's header info
     *
     *@param out the servlet's output Writer
     */
    private void printDocumentHeader (PrintWriter out) 
    {
        try {

            out.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"+
            "<html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'>"+
            "<head>"+
            "<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />"+
            "<title>W3C RDF Validation Results</title>"+
            "<link rel='stylesheet' href='http://validator.w3.org/base.css' type='text/css' />"+
            "<style type='text/css'> #menu li { color: white;}"+
                           "  td {" +
               "    background:#EEEEEE;" +
               "    font-family:'courier new',courier,serif;" +
                "    border-width: 1px;" +
                "    border-color: black;" +
		"  }" +
		"</style>\n" +
            "</head><body><div id='banner'><h1 id='title'>"+
            "<a href='http://www.w3.org/'><img height='48' alt='W3C' id='logo' src='http://www.w3.org/Icons/WWW/w3c_home_nb' /></a>"+
            "<a href='http://www.w3.org/RDF/' title='RDF (Resource Description Framework)'><img src='http://www.w3.org/RDF/icons/rdf_powered_button.48' alt='RDF' /></a>"+
            "Validation Service</h1> </div>"+
            "<ul class='navbar' id='menu'>"+
            "<li><span class='hideme'><a href='#skip' accesskey='2' title='Skip past navigation to main part of page'>Skip Navigation</a> |</span>"+
            "<strong><a href='/RDF/Validator/'>Home</a></strong></li>"+
            "<li><a href='documentation' accesskey='3' title='Documentation for this Service'>Documentation</a></li>"+
            "<li><a href='documentation#feedback' accesskey='4' title='How to provide feedback on this service'>Feedback</a></li>"+
            "</ul><div id='main'>"+
            "<div id='jumpbar'>Jump To:"+
            "<ul>"+
       		"<li><a href='#source'>Source</a></li>" +
            "<li><a href='#triples'>Triples</a></li>" +
            "<li><a href='#messages'>Messages</a></li>"+ 
            "<li><a href='#graph'>Graph</a></li>" +
            "<li><a href='#feedback'>Feedback</a></li>" +
            "<li><a href='http://www.w3.org/RDF/Validator/'>Back to Validator Input</a></li>"+
            "</ul></div><!-- jumpbar -->");


        } catch (Exception e) {
            System.err.println("Exception (printDocumentHeader): " + e.getMessage());
        }
    }

    /*
     * Print the rdf listing
     *
     *@param out the servlet's output Writer
     *@param rdf the RDF code
     *@param needCR if true, add a CarriageReturn to the output; if false,
     * do not add it
     */
    private void printListing (PrintWriter out, String rdf, 
        boolean needCR) 
    {
        try {
            out.println("<hr title='original source' />" +
                        "<h3><a name='source' id='source'>" +
			"The original RDF/XML document</a></h3>" +
                        "<pre>");

            String s = replaceString(rdf, "&", "&amp;");
            s = replaceString(s, "<", "&lt;");
            
            // Now output the RDF one line at a time with line numbers
            int lineNum = 1;
            int nl = 0;
            String terminator = needCR?"\n":"";
            do {
                String tok;
                nl = s.indexOf('\n');
                if ( nl == -1 ) {
                    tok = s;
                } else {
                    tok = s.substring(0,nl);
                    s = s.substring(nl+1);
                }
                out.print("<a name=\"" + lineNum + "\">" + lineNum +
                          "</a>: " + tok + terminator);
                lineNum++;
            } while ( nl != -1 );

            out.println("</pre>");
        } catch (Exception e) {
            System.err.println("Exception (printListing): " + e.getMessage());
        }
    }

    /*
     * Print the header for the triple listing
     *
     *@param out the servlet's output Writer
     *@param nTriples if true, output is N-Triples syntax
     */
    private void printTripleTableHeader (PrintWriter out, boolean nTriples) 
    {
        try {
            if (nTriples) {
                out.println("<h3><a name='triples' id='triples'>" +
                   "Triples of the Data Model in " +
                   "<a href=\"http://www.w3.org/2001/sw/RDFCore/ntriples/\">" +
                   "N-Triples</a> Format (Sub, Pred, Obj)</a></h3>" +
                   "<pre>");
            } else {
                out.println("<hr title='triples' />");
                out.println("<h3><a name='triples' id='triples'>" +
                            "Triples of the Data Model</a></h3>");
                out.println("<table frame='border' rules='all'><tr>" +
			    "<td><b>Number</b></td>" +
			    "<td><b>Subject</b></td>" +
			    "<td><b>Predicate</b></td>" +
			    "<td><b>Object</b></td>" +
			    "</tr>");
            }
        } catch (Exception e) {
            System.err.println("Exception (printTripleTableHeader): " + e.getMessage());
        }
    }

    /*
     * Print the footer info for the triple listing
     *
     *@param out the servlet's output Writer
     *@param nTriples if true, output is N-Triples syntax
     */
    private void printTripleTableFooter (PrintWriter out, 
        boolean nTriples) 
    {
        try {
            if (nTriples)
                out.println("</pre>");
            else
                out.println("</table>");
        } catch (Exception e) {
            System.err.println("Exception (printTripleTableFooter): " + e.getMessage());
        }
    }
    
    /*
     * Print the document's footer info
     *
     *@param out the servlet's output Writer
     *@param rdf the RDF code
     */
    private void printDocumentFooter (PrintWriter out, String rdf) 
    {
        try {

            String s;

            s = "<hr title='Problem reporting' />" +
                "<h3><a name='feedback' id='feedback'>Feedback</a></h3>" +
                "<p>If you suspect the parser is in error, please enter an explanation below and then press the <b>Submit problem report</b> button, to mail the report (and listing) to <i>" + MAIL_TO + "</i></p>" +
                "<form enctype='text/plain' method='post' action='mailto:" + MAIL_TO + "'>" +
                "<textarea cols='60' rows='4' name='report'></textarea>";
            out.println(s);

	    out.println("<input type='hidden' name='RDF' value=\"&lt;?xml version=&quot;1.0&quot;&gt;");

            // The listing is being passed as a parameter so the '<' 
            // and '"' characters must be replaced with &lt; and &quot, 
            // respectively
            if (rdf != null) {
                String s1;
		s1 = replaceString(rdf, "&",  "&amp;");
                s1 = replaceString(s1,  "<",  "&lt;");
                s1 = replaceString(s1,  ">",  "&gt;");
                s1 = replaceString(s1,  "\"", "&quot;");
                out.println(s1);
            }
            out.println("\" />");

            out.println("<input type='submit' value='Submit problem report' />" +
                        "</form>");

	    out.println("</div><!-- main --></body></html>");

        } catch (Exception e) {
            System.err.println("Exception (printDocumentFooter): " + e.getMessage());
        }
    }

    /*
     * Create a formatted string from the exception's message
     *
     *@param e any exception other than a SAXParseException
     *@return a formatted string
     */
    private static String formatOtherThanSAXParseEx(Exception e)
    {
	String msg = e.getMessage();
	if (msg == null)
	    msg = e.toString();
	msg = replaceString(msg,"&","&amp;");
	msg = replaceString(msg,"<","&lt;");
	msg = replaceString(msg,">","&gt;");
	msg = replaceString(msg,"\"","&quot;");
	msg = replaceString(msg,"'","&apos;");
	return msg;
    }

    /*
     * Servlet's get info method
     */
    public String getServletInfo () {
	return "Servlet wrapper for the ARP RDF parser. This is revision " + REVISION;
    }

    /*
     * Servlet's init method
     *
     *@param config the servlet's configuration object
     *@throws ServletException
     */
    public void init(ServletConfig config) throws ServletException 
    {
	super.init (config);

	try {
	    String t = config.getInitParameter("LOG_A_LOT");
	    Matcher m = LogALotPattern.matcher(t);
	    if (m.find() && m.group(1) != null) {
		System.err.println("considering LOG_A_LOT value \"" + t + "\" to be true base on substring \"" + m.group(1) + "\"");
		LogALot = true;
	    } else {
		System.err.println("considering LOG_A_LOT value \"" + t + "\" to be false.");
	    }
	} catch (Throwable t) {
	}

        // Cache the required parameters ...
        m_ServletTmpDir = config.getInitParameter(SERVLET_TMP_DIR);
        m_RenderDot = config.getInitParameter("RENDER_DOT");

	// ... and the optional parameters.
	try { m_BitmappedFont = config.getInitParameter("BITMAPPED_FONT"); } catch (Throwable t) {}
	try { m_VectorFont = config.getInitParameter("VECTOR_FONT"); } catch (Throwable t) {}

        if (m_ServletTmpDir == null) {
	    System.err.println (
                "<html>" +
                "<h1>Servlet Initialization Error</h1>" +
                "<h2>One or more of the following parameters has not been initialized: " + 
                SERVLET_TMP_DIR + "</h2></html>");
        }
    }

    /*
     * Servlet's destroy info method
     */
    public void destroy () {
	super.destroy ();
    }

    /*
     * Servlet's doGet info method - supported for testing
     *
     *@param req the request
     *@param res the response
     *@throws ServletException, IOException
     */
    public void doGet (HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException 
    {
	req.setCharacterEncoding("UTF-8");
        String sRDF = req.getParameter(TEXT);
	String sURI = req.getParameter(URI);

        sRDF = (sRDF == null) ? "" : sRDF;
        sURI = (sURI == null) ? "" : sURI;

	//	try {
        //    sRDF = java.net.URLDecoder.decode(sRDF, "UTF-8");
        //    sURI = java.net.URLDecoder.decode(sURI, "UTF-8");
	//} catch (Exception e) {
        //    System.err.println("Exception: URLDecoder.decode()");
	//	}

        process(req, res, sRDF, sURI);
    }

    /*
     * Servlet's doPost method
     *
     *@param req the request
     *@param res the response
     *@throws ServletException, IOException, java.io.UnsupportedEncodingException
     */
    public void doPost (HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException 
    {
	// String encoding = req.getCharacterEncoding();
        // if (encoding == null) {
	   req.setCharacterEncoding("UTF-8");
	// }
	req.setCharacterEncoding("UTF-8");
	String sRDF = req.getParameter(TEXT);
	String sURI = req.getParameter(URI);

        sRDF = (sRDF == null) ? "" : sRDF;
        sURI = (sURI == null) ? "" : sURI;

	try {
	    process(req,res,sRDF, sURI);
	} catch (Throwable t) {
	    t.printStackTrace(System.err);
	}
    }

    /*
     * Output a Resource in NTriples syntax
     *
     *@param out the servlet's output Writer
     *@param r the Resource to output
     */
    static private void printResource(PrintWriter out, AResource r)
    {
        if (r.isAnonymous() )
            out.print("_:j" + r.getAnonymousID() + " ");
        else
           out.print("&lt;" + r.getURI() + "&gt; ");
    }

    /*
     * Convert to Hex and padd left with zeroes
     *
     *@param in the integer to convert and padd
     *@param in the length of the result
     *@return the padded string
     */
     // MJD: is there an easier way to do this?
    static private String hexPadd (int number, int length)
    {
	String t = Integer.toHexString(number).toUpperCase();
	int hexlength = t.length();

        if ( hexlength > length ) {    // too long, truncate
	    hexlength = length;
	}

	int zerolength = length - hexlength;
	String r = "";

	for (int i=0; i < zerolength; i++) {
	    r += "0";
	}
	for (int i=0; i < hexlength; i++) {
	    r += t.charAt(i);
	}
	return r;
    }

    /*
     * Output a Literal in NTriples syntax
     *
     *@param out the servlet's output Writer
     *@param l the Literal to output
     */
    static private void printNTripleLiteral(PrintWriter out, ALiteral l) 
    {
        out.print("\"");
        char ar[] = l.toString().toCharArray();

        for (int i=0;i<ar.length;i++) {
            switch (ar[i]) {
                case '\\':
                    out.print("\\\\");
                    break;
                case '"':
                    out.print("\\\"");
                    break;
                case '\n':
                    out.print("\\n");
                    break;
                case '\r':
                    out.print("\\r");
                    break;
                case '\t':
                    out.print("\\t");
                    break;
                default:
                    if ( ar[i] >= 32 && ar[i] <= 127 )
                        out.print(ar[i]);
                    else if ( ar[i] < 0xD800 || ar[i] >= 0xE000)
			out.print("\\u" + hexPadd(ar[i], 4) );
	            else  {  // deal with surrogates
			     // check for correct surrogate pair
                             // this code should probably move somewhere else:
                             // check when we get the input
                        if ( ar[i] >= 0xDC00 ) {
			    out.print("{{{error: lone low surrogate}}}");
			}
		        else if ( ++i >= ar.length ) {
                            out.print("{{{error: lone surrogate at end of string}}}");
			}
			else if ( ar[i] < 0xDC00 || ar[i] >= 0xE000 ) {
			    out.print("{{{error: high surrogate not followed by low surrogate}}}");
			}
			// no errors, actually print
			else {
			    int scalarvalue = 0x10000 + (ar[i-1] * 1024) + ar[i];
			    out.print("\\U" + hexPadd(scalarvalue, 8) );
			}
		    }
            }
        }
        out.print("\" ");
	String s1="";
	if (l.getLang()!=null && l.getLang().length()>0){//add language info if it exists
	    s1+="@"+l.getLang();
	}
	if (l.getDatatypeURI()!=null){//add datatype info if it exists
	    String s2=l.getDatatypeURI();
	    s2 = replaceString(s2, "<", "&lt;");
	    s2 = replaceString(s2, ">", "&gt;");
	    s2 = replaceString(s2, "&", "&amp;");
	    if (s2.length()>0){
		s1+="^^"+s2;
	    }
	}
	if (s1.length()>0){out.print(s1);}
    }

    /*
     * Control point for outputing an triple in NTriple syntax
     *
     *@param out the servlet's output Writer
     *@param subj the subject
     *@param pred the predicate
     *@param objRes the object as a Resource (may be null)
     *@param objLit the object as a Literal (may be null)
     */
    static private void printNTriple(PrintWriter out, AResource subj, 
        AResource pred, AResource objRes, ALiteral objLit) 
    {
        printResource(out, subj);
        printResource(out, pred);
        if (objRes != null)
            printResource(out, objRes);
        else
            printNTripleLiteral(out, objLit);
        out.println(".");
    }

    /*
     * Create a HTML anchor from the URI or anonNode of the
     * given Resource
     *
     *@param r the Resource
     *@return the string as an HTML anchor
     */
    static private String addAnchor(AResource r) 
    {
        if (r.isAnonymous())
            return ANON_NODE + r.getAnonymousID();
        else
            return "<a href='" + r.getURI().replaceAll("\"", "&quot;").replaceAll("'", "&apos;") + "'>" + r.getURI().replaceAll("<", "&lt;") + "</a>";
    }

    /*
     * Output a triple as a row in HTML
     *
     *@param out the servlet's output Writer
     *@param subj the subject
     *@param pred the predicate
     *@param objRes the object as a Resource (may be null)
     *@param objLit the object as a Literal (may be null)
     *@param num the statement number
     */
    static private void printTableRow(PrintWriter out, AResource subj, 
        AResource pred, AResource objRes, ALiteral objLit, int num) 
    {
        out.println("<tr><td>" + num + "</td>");
        out.println("<td>" + addAnchor(subj) + "</td>");
        out.println("<td>" + addAnchor(pred) + "</td>");
        if (objRes != null)
            out.println("<td>" + addAnchor(objRes) + "</td>");
        else {
            out.println("<td>");
            String s1 = objLit.toString().trim();
            s1 = replaceString(s1, "<", "&lt;");
            s1 = replaceString(s1, ">", "&gt;");
            s1 = replaceString(s1, "&", "&amp;");
	    s1 = "&quot;"+s1+"&quot;";
	    if (objLit.getLang()!=null && objLit.getLang().length()>0){//add language info if it exists
		s1+="@"+objLit.getLang();
	    }
	    if (objLit.getDatatypeURI()!=null){//add datatype info if it exists
		String s3=objLit.getDatatypeURI();
		s3 = replaceString(s3, "<", "&lt;");
		s3 = replaceString(s3, ">", "&gt;");
		s3 = replaceString(s3, "&", "&amp;");
		if (s3.length()>0){
		    s1+="^^"+s3;
		}
	    }
            out.println(s1);
            out.println("</td>");
        }
        out.println("</tr>");
    }

    private static class SH implements StatementHandler 
    {
        PrintWriter out;
        PrintWriter pw;
        boolean isNTriples;
        boolean printTriples;
        boolean printGraph;
        boolean anonNodesEmpty;
        int numStatements;
        int numLiterals;
	Hashtable subjects;
	int numSubjects;
	String gFormat;
  
        /*
         * Constructuor for the StatementHandler.  The primary
	 * responsiblitly is to cache init variables
         *
	 *@param out the servlet's output Writer
	 *@param pw the Dot file's PrintWriter
	 *  syntax; otherwise use HTML syntax
	 *@param isNTriples if true, output using the NTriples
	 *@param printTriples if true, print the triples
	 *@param printGraph if true, create the graph file
	 *@param printGraph if true, anonomyous nodes should be empty
	 */
        public SH(PrintWriter out, PrintWriter pw, boolean isNTriples, 
            boolean printTriples, boolean printGraph, boolean anonNodesEmpty,String graphFormat)
        {
            this.out = out;
            this.pw = pw;
            this.isNTriples = isNTriples;
            this.printTriples = printTriples;
            this.printGraph = printGraph;
            this.anonNodesEmpty = anonNodesEmpty;

            this.numStatements = 0;
            this.numLiterals = 0;

	    this.subjects = new Hashtable();
	    this.numSubjects = 0;
	    this.gFormat=graphFormat;
        }

        /*
         * Generic handler for a Resource/Resource/Resource triple (S/P/O).
	 * Dispatches to the methods that do the real work.
         *
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Resource)
	 */
        public void statement(AResource subj, AResource pred, AResource obj) 
        {
	    if (!ARPServlet.AT_LEAST_ONE_TRIPLE){ARPServlet.AT_LEAST_ONE_TRIPLE=true;}
	    if (printTriples)
                statementResource(subj, pred, obj);
	    if (printGraph)
	        statementDotResource(subj, pred, obj);
        }

        /*
         * Generic handler for a Resource/Resource/Resource triple (S/P/O).
	 * Dispatches to the methods that do the real work.
         *
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Literal)
	 */
        public void statement(AResource subj, AResource pred, ALiteral lit) 
        {
	    if (!ARPServlet.AT_LEAST_ONE_TRIPLE){ARPServlet.AT_LEAST_ONE_TRIPLE=true;}
            numLiterals++;
	    if (printTriples)
                statementLiteral(subj, pred, lit);
	    if (printGraph)
                statementDotLiteral(subj, pred, lit);
        }

        /*
         * Handler for a Resource/Resource/Resource triple (S/P/O)
	 * Outputs the given triple using NTriples or HTML syntax.
         *
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Resource)
	 */
        public void statementResource(AResource subj, AResource pred, AResource obj) 
        {
            numStatements++;

            if (this.isNTriples)
                printNTriple(out, subj, pred, obj, null);
            else
                printTableRow(out, subj, pred, obj, null, this.numStatements);
        }

        /*
         * Handler for a Resource/Resource/Literal triple (S/P/O)
	 * Outputs the given triple using NTriples or HTML syntax.
         *
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Literal)
	 */
        public void statementLiteral(AResource subj, AResource pred, ALiteral lit) 
        {
            numStatements++;

            if (this.isNTriples)
                printNTriple(out, subj, pred, null, lit);
            else
                printTableRow(out, subj, pred, null, lit, this.numStatements);
        }

	/* 
	 * Print the first part of a triple's Dot file.  See below for
	 * more info.  This is the same regardless if the triple's
	 * object is a Resource or a Literal
	 *
	 *@param subj the subject
	 */
        public void printFirstPart(AResource subj) 
	{
            if (subj.isAnonymous()) {
		if (this.anonNodesEmpty) {
		    Integer n = (Integer) subjects.get(subj.getAnonymousID());
		    if (n == null) {
			this.numSubjects++;
			subjects.put(subj.getAnonymousID(), new Integer(this.numSubjects));
                        this.pw.println("\"" + ANON_NODE + subj.getAnonymousID() + "\" [label=\"   \"];");
		    }
		}
                this.pw.print("\"" + ANON_NODE + subj.getAnonymousID());
            } else {
		if (gFormat!=null && gFormat.equals(FORMAT_ISV_ZVTM)){
		    this.pw.println("\"" + subj.getURI() + "\" [fontcolor=\""+Float.toString(ARPServlet.resTBh)+","+Float.toString(ARPServlet.resTBs)+","+Float.toString(ARPServlet.resTBv)+"\",URL=\"" +subj.getURI() + "\"];");
		}
		else {
		    this.pw.println("\"" + subj.getURI() + "\" [URL=\"" +subj.getURI() + "\"];");
		}
                this.pw.print("\"" + subj.getURI());
            }
	}

        /*
         * Handler for a Resource/Resource/Resource triple (S/P/O).
	 * Outputs the given triple using Dot syntax.
	 *
	 * Each triple will be output in three lines of DOT code as
	 * follows (not including the complication of anon nodes 
	 * and the possiblity that the anon nodes may be named 
	 * with an empty string):
	 *
	 *   1. "<subject>" [URL="<subject">];
	 *   2. "<subject>" -> "<object>" [label="<predicate>",URL="<predicate>"];
	 *   3. "<object>"  [URL="<object>"];
         *
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Resource)
	 */
        public void statementDotResource(AResource subj, AResource pred, AResource obj) 
        {
	    if (this.pw == null) return;

	    printFirstPart(subj);
           
            this.pw.print("\" -> ");

            if (obj.isAnonymous()) {
		if (this.anonNodesEmpty) {
		    if (gFormat!=null && gFormat.equals(FORMAT_ISV_ZVTM)){
			this.pw.println("\"" + ANON_NODE +obj.getAnonymousID() + "\" [fontcolor=\""+Float.toString(ARPServlet.prpTh)+","+Float.toString(ARPServlet.prpTs)+","+Float.toString(ARPServlet.prpTv)+"\",label=\"" + pred.getURI() + "\",URL=\"" + pred.getURI() + "\"];");
		    }
		    else {
			this.pw.println("\"" + ANON_NODE +obj.getAnonymousID() + "\" [label=\"" + pred.getURI() + "\",URL=\"" + pred.getURI() + "\"];");
		    }
		} else {
		    if (gFormat!=null && gFormat.equals(FORMAT_ISV_ZVTM)){
			this.pw.println("\"" + ANON_NODE + obj.getAnonymousID() + "\" [fontcolor=\""+Float.toString(ARPServlet.prpTh)+","+Float.toString(ARPServlet.prpTs)+","+Float.toString(ARPServlet.prpTv)+"\",label=\"" + pred.getURI() + "\",URL=\"" +pred.getURI() + "\"];");
		    }
		    else {
			this.pw.println("\"" + ANON_NODE + obj.getAnonymousID() + "\" [label=\"" + pred.getURI() + "\",URL=\"" +pred.getURI() + "\"];");
		    }
		}
            } else {
		if (gFormat!=null && gFormat.equals(FORMAT_ISV_ZVTM)){
		    this.pw.println("\"" + obj.getURI() + "\" [fontcolor=\""+Float.toString(ARPServlet.prpTh)+","+Float.toString(ARPServlet.prpTs)+","+Float.toString(ARPServlet.prpTv)+"\",label=\"" +pred.getURI() + "\",URL=\"" + pred.getURI() + "\"];");
		    this.pw.println("\"" + obj.getURI() + "\" [fontcolor=\""+Float.toString(resTBh)+","+Float.toString(resTBs)+","+Float.toString(resTBv)+"\",URL=\"" +obj.getURI() + "\"];");
		}
		else {
		    this.pw.println("\"" + obj.getURI() + "\" [label=\"" +pred.getURI() + "\",URL=\"" + pred.getURI() + "\"];");
		    this.pw.println("\"" + obj.getURI() + "\" [URL=\"" +obj.getURI() + "\"];");
		}
	    }
        }

        /*
         * Handler for a Resource/Resource/Literal triple (S/P/O).
	 * Outputs the given triple using Dot syntax.
	 *
	 * Each triple will be output in three lines of DOT code as
	 * follows (not including the complication of anon nodes 
	 * and the possiblity that the anon nodes may be named 
	 * with an empty string):
         *
	 *   1. "<subject>" [URL="<subject">];
	 *   2. "<subject>" -> "<literal>" [label="<predicate>",URL="<predicate>"];
	 *   3. "<literal>" [shape="box"];
         *
	 *@param subj the subject
	 *@param pred the predicate
	 *@param obj the object (as a Literal)
	 */
        public void statementDotLiteral(AResource subj, AResource pred, ALiteral lit) 
        {
	    if (this.pw == null) return;

	    printFirstPart(subj);  // Same as Res/Res/Res

            /*
             * Before outputing the object (Literal) do the following:
             *
             * o GraphViz/DOT cannot handle embedded line terminators characters
             *   so they must be replaced with spaces
             * o Limit the number of chars to make the graph legible
             * o Escape double quotes
             */
            String s1 = new String(lit.toString());
            s1 = s1.replace('\n', ' ');
            s1 = s1.replace('\f', ' ');
            s1 = s1.replace('\r', ' ');
            if (s1.indexOf('"') != -1)
                s1 = replaceString(s1, "\"", "\\\"");

            // Anything beyond 80 chars makes the graph too large
            String tmpObject;
            if (s1.length() >= 80)
                tmpObject = s1.substring(0, 80) + " ...";
            else
                tmpObject = s1.substring(0, s1.length());

	    // Create a temporary label for the literal so that if
	    // it is duplicated it will be unique in the graph and
	    // thus have its own node.
	    String tmpName = "Literal_" + Integer.toString(this.numLiterals);
            this.pw.print("\" -> \"" + tmpName);

	    if (gFormat!=null && gFormat.equals(FORMAT_ISV_ZVTM)){
		this.pw.println("\" [fontcolor=\""+Float.toString(ARPServlet.prpTh)+","+Float.toString(ARPServlet.prpTs)+","+Float.toString(ARPServlet.prpTv)+"\",label=\"" + pred.getURI() + "\",URL=\""    + pred.getURI() + "\"];");
		this.pw.println("\"" + tmpName + "\" [fontcolor=\""+Float.toString(litTBh)+","+Float.toString(litTBs)+","+Float.toString(litTBv)+"\",shape=box,label=\"" + tmpObject + "\"];");
	    }
	    else {
		this.pw.println("\" [label=\"" + pred.getURI() + "\",URL=\""    + pred.getURI() + "\"];");
		this.pw.println("\"" + tmpName + "\" [shape=box,label=\"" + tmpObject + "\"];");
	    }
        }
    }

    private void printErrorMessages(PrintWriter out, SaxErrorHandler eh)
    {
        try {
            String s;
            boolean c = true;

	    out.println("<h2><a name='messages' id='messages'>" +
			"Validation Results</a></h2>");

            s = eh.getFatalErrors();
            if (s != null && s.length() >= 1) {
                out.println("<h3>Fatal Error Messages</h3>" + s);
		c = false;
	    }

            s = eh.getErrors();
            if (s != null && s.length() >= 1) {
                out.println("<h3>Error Messages</h3>" + s);
		c = false;
	    }

            s = eh.getWarnings();
            if (s != null && s.length() >= 1) {
                out.println("<h3>Warning Messages</h3>" + s);
		c = false;
	    }

            if (c){
		if (AT_LEAST_ONE_TRIPLE){
		    out.println("<p>Your RDF document validated successfully.</p>");
		}
		else {
		    out.println("<p>Error: Your document does not contain any RDF statement.</p>");
		}
		AT_LEAST_ONE_TRIPLE=false;
	    }

	    /*the following should not happen anymore, as we use ARP2 which supports datatypes, but leave it there for now, just in case*/
	    s = eh.getDatatypeErrors();
	    if (s != null && s.length() >= 2){//2 to prevent an arrayindexoutofbounds exception
	        out.println("<h3>Note about datatypes</h3>");
		out.println("<p>Datatypes are used on lines "+s.substring(0,s.length()-2)+". This RDF feature is not yet supported by the RDF Validator. Literals are treated as untyped.</p>");
	    }

	} catch (Exception e) {
	    System.err.println(SERVLET_NAME + ": Error printing error messages.");
	}
    }

    /*
     * Initialize the graph output file.  If an error occurs, this
     * function will print an error message.
     *
     *@param out the servlet's output Writer
     *@req the servlet request object
     *@return the File object for the graph file; null if an error occurs
     */
    private File initGraphFile(PrintWriter out, 
	HttpServletRequest req)
    {
        try {
            // Stop if any of the parameters are missing
            if (m_ServletTmpDir == null)
	    {
                // Put the paths in a comment in the returned content
                out.println("<!-- SERVLET_TMP_DIR = " + m_ServletTmpDir + " -->");

                out.println("<h1>Servlet initialization failed</h1>");
		out.println("Please send a message to <a href='mailto:" + MAIL_TO +  "'>" + MAIL_TO + "</a> and mention this problem.");
                return null;
            } 
	} catch (Exception e) {
	    System.err.println("Unable to create a temporary graph file. A graph cannot be generated.");
	    return null;
	}

        File dotFile = null;

        // Must generate a unique file name that the DOT handler will use 
        dotFile = createTempFile(m_ServletTmpDir, TMP_FILE_PREFIX, SUFFIX_DOT);
        if (dotFile == null) {
            out.println("<h1>Failed to create a temporary graph file. A graph cannot be generated.</h1>");
            System.err.println("Failed to create temporary graph file \""+TMP_FILE_PREFIX+" "+SUFFIX_DOT+"\" in \""+m_ServletTmpDir+"\" . A graph cannot be generated.");
            return null;
        }

	return dotFile;
    }

    /*
     * Check if the given URI is supported or not
     *
     *@param out the servlet's output Writer
     *@param uri the URI to check
     *@return true if the URI is supported; false otherwise
     */
    private boolean isURISupported(PrintWriter out, String uri)
    {
	try {
	    if (uri.length() >= 4 && uri.substring(0,4).equalsIgnoreCase("file")) {
	        out.println("<h1>file URI Schemes are NOT Supported</h1>");
	        out.println("URIs from the 'file' URI scheme are not supported by this servlet.");
	        return false;
	    }
        } catch (Exception e) {
	    System.err.println("Exception in isURISupported.");
	    return false;
	}

	return true;
    }
    
    /*
     * Handle the servlets doGet or doPut request
     *
     *@param req the servlet's request
     *@param res the servlet's response
     *@throws SevletException, IOException
     */
    private void process(HttpServletRequest req, HttpServletResponse res, 
        String sRDF, String sURI) throws ServletException, IOException 
    {
	String sSaveRDF         = req.getParameter (SAVE_RDF);
	String sSaveDOTFile     = req.getParameter (SAVE_DOT_FILE);
	String sNTriples        = req.getParameter (NTRIPLES);
	String sEmbedded        = req.getParameter (EMBEDDED_RDF);
	String sTriplesAndGraph = req.getParameter (TRIPLES_AND_GRAPH);
	String sAnonNodesEmpty  = req.getParameter (ANON_NODES_EMPTY);
	String sParse           = req.getParameter (PARSE);
	String sFormat          = req.getParameter (FORMAT);
	if (sFormat==null)
	    sFormat = DEFAULT_FORMAT;

        // Set the print flags
        boolean printTriples = false;
	boolean printGraph = false;
	if (sTriplesAndGraph != null) {
	    if (sTriplesAndGraph.equals(PRINT_TRIPLES)) {
		printTriples = true;
	    }
	    if (sTriplesAndGraph.equals(PRINT_GRAPH)) {
		printGraph = true;
	    }
	    if (sTriplesAndGraph.equals(PRINT_BOTH)) {
		printTriples = true;
		printGraph = true;
	    }
	} 

	// Determine if printing the triples and/or graph
        boolean anonNodesEmpty = (sAnonNodesEmpty != null) ? true : false;
        boolean nTriples = (sNTriples != null) ? true : false;

        // ARP parser has embedded = true by default so if user
        // wants embedding, must set it to false
        boolean embedded = (sEmbedded != null) ? false : true;

        res.setContentType ("text/html;charset=utf-8");
        PrintWriter out = res.getWriter ();

	// Temporary buffer to rearrange output (placing validation
	// success or failure at top
	StringWriter sw = new StringWriter();
	PrintWriter outtmp = new PrintWriter(sw);

	printDocumentHeader (out);

	boolean parseRDF = true;
	if (sParse != null)
            parseRDF = sParse.equals("Parse RDF");
        else if (!sURI.equals(""))  // continue even if PARSE is not present
            parseRDF = false;            

	if (parseRDF)  sURI = "";
	else           sRDF = "";

        // getting encoding right: bad hack, but it works :-(
        // sRDF = new String(sRDF.getBytes("iso-8859-1"), "utf-8");
	// sURI = new String(sURI.getBytes("iso-8859-1"), "utf-8");

        if ((!parseRDF && sURI.equals("")) || (parseRDF && sRDF.equals(""))) {
            out.println("<h1>" + (parseRDF ? "RDF" : "URI")
                          + " was not specified.</h1>");
	    printDocumentFooter(out, null);
            return;
        }

        String xmlBase = null;

        if (!sURI.equals("")) {

	    // First check for unsupported URIs
	    if (!isURISupported(out, sURI)) {
                printDocumentFooter(out, null);
                return;
	    }

            xmlBase = sURI;
	    try {
		sRDF = getRDFfromURI(sURI);
		if (sRDF == null)
		    throw new getRDFException("The URI may not exist or the server is down.@@");
	    } catch (getRDFException e) {
		out.println("<h1>RDF Load Error</h1>");
		out.println("An attempt to load the RDF from URI '" + sURI.replace("<", "&lt;") +
			    "' failed.  (" + e.getMessage() + ")");
		printDocumentFooter(out, null);
		return;
	    }
        } else {
	    java.util.Date d = new java.util.Date();
	    xmlBase = DEFAULT_NAMESPACE + d.getTime() + '#';
	}
 
	PrintWriter pw = null; // The writer for the graph file
	OutputStreamWriter osw = null;
	File dotFile = null;   // The graph file
        if (printGraph) {
	    dotFile = initGraphFile(out, req);
	    if (dotFile == null)
		// Assume error has been reported
		return;
            // Create a PrintWriter for the DOT handler
	    FileOutputStream fos = new FileOutputStream(dotFile);
	    if (fos != null)
		osw = new OutputStreamWriter(fos, "utf-8");
	    if (osw != null)
                pw = new PrintWriter(osw);
	    if (pw != null)
                // Add the graph header
                processGraphParameters (req, pw, (sFormat.equals(FORMAT_PNG_EMBED)) || (sFormat.equals(FORMAT_PNG_LINK)) || (sFormat.equals(FORMAT_GIF_EMBED)) || (sFormat.equals(FORMAT_GIF_LINK)));
	}

	// Create the StatementHandler - it will handle triples for
	// the table/ntriples and the graph file
        SH sh = new SH(outtmp, pw, nTriples, printTriples, printGraph, anonNodesEmpty,sFormat);

        // Create the ErrorHandler 
        SaxErrorHandler errorHandler = new SaxErrorHandler(out, false);

        // Create and initialize the parser

// 	System.err.println("-----------------------------------------------");
// 	System.err.println("Determining class for ARP");
// 	String arp="com.hp.hpl.jena.rdf.arp.ARP";
// 	try {
// 	    Class c = Class.forName (arp);
// 	    String classRes = "/"+arp.replace ('.', '/')+
// 		".class";
// 	    System.out.println ("Class "+arp+
// 				" URL: "+c.getResource (classRes));
// 	}
// 	catch (Throwable t)
//             {
//                 System.out.println ("Unable to locate class "+arp);
//             }

	ARP parser = new com.hp.hpl.jena.rdf.arp.ARP();
        parser.getHandlers().setErrorHandler(errorHandler);
        parser.getHandlers().setStatementHandler(sh);
        parser.getOptions().setEmbedding(embedded);
	parser.getOptions().setStrictErrorMode();

        try {
            java.lang.reflect.Field arpfField = parser.getClass().getDeclaredField("arpf");
            arpfField.setAccessible(true);
            RDFXMLParser arpf = (RDFXMLParser) arpfField.get(parser);
            java.lang.reflect.Field saxParserField = arpf.getClass().getDeclaredField("saxParser");
            saxParserField.setAccessible(true);
            org.apache.xerces.parsers.SAXParser saxParser = (org.apache.xerces.parsers.SAXParser) saxParserField.get(arpf);
            saxParser.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (NoSuchFieldException|IllegalAccessException|SAXException e) {
            throw new RuntimeException("should not be here");
        }

        if (printTriples)
            printTripleTableHeader (outtmp, nTriples);

        try {
	    StringReader  sr = new StringReader (sRDF);
	    parser.load(sr, xmlBase);
        } catch (Exception ex) {
	    out.println ("<h1><a name='messages' id='messages'>Parser " +
                         "Loading Error</a></h1>");
	    out.println ("Exception parsing: " +formatOtherThanSAXParseEx(ex));
            printDocumentFooter(out, null);
            return;
        }

        if (printTriples)
            printTripleTableFooter(outtmp, nTriples);

	printErrorMessages(out, errorHandler);
	out.print(sw.toString());

        printListing (out, sRDF, !parseRDF);

        res.flushBuffer();
        if (sFormat != null && printGraph) {
            generateGraph(out, pw, dotFile, sRDF, req, sFormat,
                          (sSaveRDF != null) ? true : false,
                          (sSaveDOTFile != null && sSaveDOTFile.equals ("on") ? true : false));
        }


        if (!parseRDF)
            printDocumentFooter(out, null);
        else
            printDocumentFooter(out, sRDF);
    }
}
