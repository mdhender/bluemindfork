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
package net.bluemind.eas.wbxml;

import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import net.bluemind.eas.wbxml.parsers.WbxmlExtensionHandler;

public class PushDocumentHandler implements ContentHandler, WbxmlExtensionHandler {

	private static final Logger logger = LoggerFactory.getLogger(PushDocumentHandler.class);

	private Document doc;
	private Stack<Element> elems;

	public PushDocumentHandler() {
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = db.newDocument();
			elems = new Stack<Element>();
		} catch (ParserConfigurationException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void characters(char[] data, int off, int count) throws SAXException {
		Element cur = elems.peek();
		cur.setTextContent(new String(data, off, count));
	}

	@Override
	public void endDocument() throws SAXException {
		// do nothing
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		elems.pop();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes arg1) throws SAXException {
		Element newE = doc.createElementNS(uri, qName);

		Element parent = null;
		if (!elems.isEmpty()) {
			parent = elems.peek();
		}

		if (parent != null) {
			parent.appendChild(newE);
		} else {
			doc.appendChild(newE);
		}
		elems.add(newE);

		for (int i = 0; i < arg1.getLength(); i++) {
			String att = arg1.getQName(i);
			String val = arg1.getValue(i);
			newE.setAttribute(att, val);
		}
	}

	public Document getDocument() {
		return doc;
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// TODO Auto-generated method stub

	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		// do nothing
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		// do nothing
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		// do nothing
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		// do nothing
	}

	@Override
	public void startDocument() throws SAXException {
		// do nothing
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		// do nothing
	}

	@Override
	public void ext_i(int id, String par) throws SAXException {
		logger.error("ext_i: Not implemented. {} {}", id, par);
	}

	@Override
	public void ext_t(int id, int par) throws SAXException {
		logger.error("ext_t: Not implemented. {} {}", id, par);
	}

	@Override
	public void ext(int id) throws SAXException {
		logger.error("ext {}: Not implemented.", id);
	}

	@Override
	public void opaque(byte[] data) throws SAXException {
		logger.debug("Handling OPAQUE (len: " + data.length + ")");
		Element parentElem = elems.peek();
		parentElem.setTextContent(java.util.Base64.getEncoder().encodeToString(data));
	}

}
