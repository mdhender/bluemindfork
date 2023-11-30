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
package net.bluemind.eas.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

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

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Utility methods to extract data from a DOM.
 * 
 * 
 */
public final class DOMUtils {
	private static final Logger logger = LoggerFactory.getLogger(DOMUtils.class);

	private static TransformerFactory fac;
	private static DocumentBuilderFactory dbf;
	private static DocumentBuilder builder;
	private static Semaphore builderLock;

	static {
		fac = TransformerFactory.newInstance();
		dbf = new DocumentBuilderFactoryImpl();
		dbf.setNamespaceAware(true);
		dbf.setValidating(false);
		try {
			builder = dbf.newDocumentBuilder();
			builderLock = new Semaphore(1);
		} catch (ParserConfigurationException e) {
		}
	}

	private static final void lock() {
		try {
			builderLock.acquire();
		} catch (InterruptedException e) {
		}
	}

	private static final void unlock() {
		builderLock.release();
	}

	public static String getElementText(Element root, String elementName) {
		NodeList list = root.getElementsByTagName(elementName);
		if (list.getLength() == 0) {
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

	public static Element getDirectChildElement(Element root, String elementName) {
		NodeList nodes = root.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			if (n.getNodeName().equals(elementName)) {
				return (Element) n;
			}

		}
		return null;
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
	public static final String stripNonValidXMLCharacters(String in) {
		char[] current = in.toCharArray();
		StringBuilder out = new StringBuilder(current.length);

		for (int i = 0; i < current.length; i++) {
			char c = current[i];
			if (validXmlChar(c)) {
				out.append(c);
			}
		}
		return out.toString();
	}

	// Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] |
	// [#x10000-#x10FFFF]
	private static final boolean validXmlChar(int c) {
		return c == 0x9 || c == 0xA || c == 0xD || (c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD)
				|| (c >= 0x10000 && c <= 0x10FFFF);
	}

	public static Element createElementAndText(Element parent, String elementName, String text) {
		if (text == null) {
			throw new NullPointerException("null text");
		}
		String ns = parent.getNamespaceURI();
		String name = elementName;
		int idx = elementName.indexOf(':');
		if (idx > 0) {
			ns = elementName.substring(0, idx);
			name = elementName.substring(idx + 1);
		}

		Element el = parent.getOwnerDocument().createElementNS(ns, name);
		parent.appendChild(el);
		LazyTextNode ltn = new LazyTextNode(parent.getOwnerDocument());
		ltn.setNodeValue(stripNonValidXMLCharacters(text));
		el.appendChild(ltn);
		return el;
	}

	public static Element createElement(Element parent, String elementName) {
		String ns = parent.getNamespaceURI();
		String name = elementName;
		int idx = elementName.indexOf(':');
		if (idx > 0) {
			ns = elementName.substring(0, idx);
			name = elementName.substring(idx + 1);
		}
		Element el = parent.getOwnerDocument().createElementNS(ns, name);
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
		serialise(doc, out, false);
	}

	public static void logDom(Document doc) throws TransformerException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		serialise(doc, out, true);
		System.out.println(out.toString());
	}

	public static Document parse(InputStream is)
			throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
		lock();
		Document ret = null;
		try {
			ret = builder.parse(is);
		} finally {
			unlock();
		}
		return ret;
	}

	public static Document createDoc(String namespace, String rootElement) {
		if (namespace == null) {
			throw new RuntimeException("EAS documents must be namespaced correctly, namespace can't be null");
		}
		lock();
		DOMImplementation di = builder.getDOMImplementation();
		Document ret = null;
		try {
			ret = di.createDocument(namespace, rootElement, null);
		} catch (Exception e) {
			logger.error("document creation failed", e);
		} finally {
			unlock();
		}
		return ret;
	}

	public static Document cloneDOM(Document doc) throws TransformerException {
		return (Document) doc.cloneNode(true);
	}
}
