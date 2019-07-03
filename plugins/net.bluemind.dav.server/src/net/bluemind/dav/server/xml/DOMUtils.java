/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.dav.server.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.bluemind.core.api.fault.ServerFault;

/**
 * Utility methods to extract data from a DOM.
 * 
 * 
 */
public final class DOMUtils {

	private static TransformerFactory fac;
	private static DocumentBuilderFactory dbf;

	private static ThreadLocal<DocumentBuilder> builder = new ThreadLocal<DocumentBuilder>();
	private static ErrorHandler errorHandler;

	static {
		fac = TransformerFactory.newInstance();
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(false);
		try {
			// From xerces performance faq:
			// Deferred DOM -- by default, the DOM feature defer-node-expansion
			// is true, causing DOM nodes to be expanded as the tree is
			// traversed. The performance tests produced by Denis Sosnoski
			// showed that Xerces DOM with deferred node expansion offers poor
			// performance and large memory size for small documents (0K-10K).
			// Thus, for best performance when using Xerces DOM with smaller
			// documents you should disable the deferred node expansion feature.
			// For larger documents (~100K and higher) the deferred DOM offers
			// better performance than non-deferred DOM but uses a large memory
			// size.
			dbf.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
		} catch (ParserConfigurationException e) {
		}

		errorHandler = new ErrorHandler() {

			private Logger logger = LoggerFactory.getLogger(getClass());

			@Override
			public void warning(SAXParseException arg0) throws SAXException {
				logger.warn(arg0.getMessage());
			}

			@Override
			public void fatalError(SAXParseException arg0) throws SAXException {
				logger.error(arg0.getMessage(), arg0);
			}

			@Override
			public void error(SAXParseException arg0) throws SAXException {
				logger.error(arg0.getMessage(), arg0);
			}
		};

	}

	private static DocumentBuilder builder() {
		DocumentBuilder ret = builder.get();
		if (ret == null) {
			try {
				ret = dbf.newDocumentBuilder();
				ret.setErrorHandler(errorHandler);
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			builder.set(ret);
		}
		return ret;
	}

	private static final Logger logger = LoggerFactory.getLogger(DOMUtils.class);

	public static String getElementTextInChildren(Element root, String elementName) {
		NodeList list = root.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.TEXT_NODE) {
				continue;
			}
			Element e = (Element) list.item(i);
			if (e.getTagName().equals(elementName)) {
				return getElementText((Element) list.item(i));
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("No element named '" + elementName + "' under '" //$NON-NLS-1$ //$NON-NLS-2$
					+ root.getNodeName() + "'"); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Returns the first element with the given name in the children of the
	 * provided element
	 * 
	 * @param root
	 *            child nodes of this element are searched
	 * @param elementName
	 * @return null if no element matches elementName
	 */
	public static Element getUniqueElementInChildren(Element root, String elementName) {

		NodeList list = root.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.TEXT_NODE) {
				continue;
			}
			Element e = (Element) list.item(i);
			if (e.getTagName().equals(elementName)) {
				return (Element) list.item(i);
			}
		}
		return null;
	}

	public static String getElementText(Element root, String elementName) {
		NodeList list = root.getElementsByTagName(elementName);
		if (list.getLength() == 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("No element named '" + elementName + "' under '" //$NON-NLS-1$ //$NON-NLS-2$
						+ root.getNodeName() + "'"); //$NON-NLS-1$
			}
			return null;
		}
		return getElementText((Element) list.item(0));
	}

	public static String getElementText(Element node) {
		Text txtElem = (Text) node.getFirstChild();
		if (txtElem == null) {
			return null;
		}
		return txtElem.getData();
	}

	public static String[] getTexts(Element root, String elementName) {
		NodeList list = root.getElementsByTagName(elementName);
		String[] ret = new String[list.getLength()];
		for (int i = 0; i < list.getLength(); i++) {
			Text txt = (Text) list.item(i).getFirstChild();
			if (txt != null) {
				ret[i] = txt.getData();
			} else {
				ret[i] = ""; //$NON-NLS-1$
			}
		}
		return ret;
	}

	/**
	 * Renvoie sous la forme d'un tableau la valeur des attributs donnés pour
	 * toutes les occurences d'un élément donnée dans le dom
	 * 
	 * <code>
	 *  <toto>
	 *   <titi id="a" val="ba"/>
	 *   <titi id="b" val="bb"/>
	 *  </toto>
	 * </code>
	 * 
	 * et getAttributes(&lt;toto&gt;, "titi", { "id", "val" }) renvoie { { "a",
	 * "ba" } { "b", "bb" } }
	 * 
	 * @param root
	 * @param elementName
	 * @param wantedAttributes
	 * @return
	 */
	public static String[][] getAttributes(Element root, String elementName, String[] wantedAttributes) {
		NodeList list = root.getElementsByTagName(elementName);
		String[][] ret = new String[list.getLength()][wantedAttributes.length];
		for (int i = 0; i < list.getLength(); i++) {
			Element elem = (Element) list.item(i);
			for (int j = 0; j < wantedAttributes.length; j++) {
				ret[i][j] = elem.getAttribute(wantedAttributes[j]);
			}
		}
		return ret;
	}

	/**
	 * Renvoie la valeur de l'attribut donné, d'un élément donné qui doit être
	 * unique sous l'élément racine
	 * 
	 * @param root
	 * @param elementName
	 * @param attribute
	 * @return
	 */
	public static String getElementAttribute(Element root, String elementName, String attribute) {
		NodeList list = root.getElementsByTagName(elementName);
		if (list.getLength() == 0) {
			return null;
		}
		return ((Element) list.item(0)).getAttribute(attribute);
	}

	/**
	 * Renvoie une élément qui doit être unique dans le document.
	 * 
	 * @param root
	 * @param elementName
	 * @return
	 */
	public static Element getUniqueElement(Element root, String elementName) {
		NodeList list = root.getElementsByTagName(elementName);
		return (Element) list.item(0);
	}

	public static Element findElementWithUniqueAttribute(Element root, String elementName, String attribute,
			String attributeValue) {
		NodeList list = root.getElementsByTagName(elementName);
		for (int i = 0; i < list.getLength(); i++) {
			Element tmp = (Element) list.item(i);
			if (tmp.getAttribute(attribute).equals(attributeValue)) {
				return tmp;
			}
		}
		return null;
	}

	/**
	 * This method ensures that the output String has only valid XML unicode
	 * characters as specified by the XML 1.0 standard. For reference, please
	 * see <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
	 * standard</a>. This method will return an empty String if the input is
	 * null or empty.
	 * 
	 * @param in
	 *            The String whose non-valid characters we want to remove.
	 * @return The in String, stripped of non-valid characters.
	 */
	private static String stripNonValidXMLCharacters(String in) {
		StringBuffer out = new StringBuffer(in.length()); // Used to hold the
		// output.
		char current;

		for (int i = 0; i < in.length(); i++) {
			current = in.charAt(i);
			if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF))
					|| ((current >= 0xE000) && (current <= 0xFFFD))
					|| ((current >= 0x10000) && (current <= 0x10FFFF))) {
				out.append(current);
			}
		}
		return out.toString();
	}

	public static Element createElementAndText(Element parent, String elementName, String text) {
		if (text == null) {
			throw new NullPointerException("null text");
		}
		Element el = parent.getOwnerDocument().createElement(elementName);
		parent.appendChild(el);
		Text txt = el.getOwnerDocument().createTextNode(stripNonValidXMLCharacters(text));
		el.appendChild(txt);
		return el;
	}

	public static Element appendText(Element el, String text) {
		if (text == null) {
			throw new NullPointerException("null text");
		}
		Text txt = el.getOwnerDocument().createTextNode(stripNonValidXMLCharacters(text));
		el.appendChild(txt);
		return el;
	}

	public static Element createElement(Element parent, String elementName) {
		Element el = parent.getOwnerDocument().createElement(elementName);
		parent.appendChild(el);
		return el;
	}

	public static void serialise(Document doc, OutputStream out, boolean pretty) throws TransformerException {
		Transformer tf = fac.newTransformer();
		if (pretty) {
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
		} else {
			tf.setOutputProperty(OutputKeys.INDENT, "no");
		}
		Source input = new DOMSource(doc.getDocumentElement());
		Result output = new StreamResult(out);
		tf.transform(input, output);
	}

	public static void serialise(Document doc, OutputStream out) throws TransformerException {
		if (logger.isDebugEnabled()) {
			logDom(doc);
		}
		serialise(doc, out, false);
	}

	public static void logDom(Document doc) throws TransformerException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		serialise(doc, out, true);
		System.out.println(out.toString());
	}

	public static Document parse(InputStream is)
			throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
		DocumentBuilder b = builder();
		Document ret = b.parse(is);
		b.reset();
		return ret;
	}

	public static Document createDoc(String namespace, String rootElement)
			throws ParserConfigurationException, FactoryConfigurationError {
		DOMImplementation di = builder().getDOMImplementation();
		Document ret = null;
		ret = di.createDocument(namespace, rootElement, null);
		return ret;
	}

	public static Document createDocNS(String namespace, String rootElement)
			throws ParserConfigurationException, FactoryConfigurationError {
		Document ret = builder().newDocument();
		Element root = ret.createElementNS(namespace, rootElement);
		ret.appendChild(root);
		return ret;
	}

	public static String asString(Document doc) throws ServerFault {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			DOMUtils.serialise(doc, out, false);
			return out.toString();
		} catch (Exception e) {
			throw new ServerFault(e);
		}

	}

	public static String asPrettyString(Document doc) throws ServerFault {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			DOMUtils.serialise(doc, out, true);
			return out.toString();
		} catch (Exception e) {
			throw new ServerFault(e);
		}

	}

	public static final Document parse(String p) throws ServerFault {
		ByteArrayInputStream bi = new ByteArrayInputStream(p.getBytes());
		try {
			Document doc = parse(bi);
			return doc;
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}
}
