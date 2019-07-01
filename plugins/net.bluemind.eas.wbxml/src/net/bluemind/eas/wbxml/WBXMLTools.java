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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.dom.DOMSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import net.bluemind.eas.wbxml.parsers.WbxmlParser;
import net.bluemind.eas.wbxml.writers.WbxmlEncoder;

/**
 * Wbxml conversion tools
 * 
 * 
 */
public class WBXMLTools {

	private static final Logger logger = LoggerFactory.getLogger(WBXMLTools.class);

	public static Document toXml(byte[] wbxml) throws IOException {
		return toXml(new ByteArrayInputStream(wbxml));
	}

	/**
	 * Transforms a wbxml byte array into the corresponding DOM representation
	 * 
	 * @param wbxml
	 * @return
	 * @throws IOException
	 */
	public static Document toXml(InputStream wbxmlStream) throws IOException {
		PushDocumentHandler pdh = new PushDocumentHandler();
		WbxmlParser parser = new WbxmlParser(pdh, pdh);
		try {
			parser.parse(wbxmlStream);
			return pdh.getDocument();
		} catch (SAXException e) {
			logger.error(e.getMessage(), e);
			throw new IOException(e.getMessage());
		}

	}

	public static byte[] toWbxml(String defaultNamespace, Document doc) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			toWbxml(defaultNamespace, doc, WbxmlOutput.of(bout));
			return bout.toByteArray();
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public static void toWbxml(String defaultNamespace, Document doc, WbxmlOutput output) throws IOException {
		WbxmlEncoder encoder = new WbxmlEncoder(defaultNamespace, output);
		try {
			DOMSource domSource = new DOMSource(doc.getDocumentElement());
			encoder.convert(domSource);
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

}
