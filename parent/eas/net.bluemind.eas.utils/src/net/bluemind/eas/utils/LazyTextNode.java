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

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.xerces.dom.ChildNode;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import net.bluemind.common.io.FileBackedOutputStream;

@SuppressWarnings("serial")
public class LazyTextNode extends ChildNode implements Text {

	private static final Logger logger = LoggerFactory.getLogger(LazyTextNode.class);
	private final FileBackedOutputStream fbos;

	public LazyTextNode(Document owner) {
		super((CoreDocumentImpl) owner);
		fbos = new FileBackedOutputStream(128, "eas-textnode");
	}

	@Override
	public String getData() throws DOMException {
		try {
			String ret = fbos.asByteSource().asCharSource(Charset.defaultCharset()).read();
			return ret;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new DOMException((short) 0, e.getMessage());
		}
	}

	@Override
	public void setData(String data) throws DOMException {
		try {
			fbos.write(data.getBytes());
			fbos.close();
		} catch (IOException e) {
			throw new DOMException((short) 0, e.getMessage());
		}
	}

	@Override
	public String substringData(int offset, int count) throws DOMException {
		throw new DOMException((short) 0, "substring not implemented");
	}

	@Override
	public void appendData(String arg) throws DOMException {
		throw new DOMException((short) 0, "append not implemented");
	}

	@Override
	public void insertData(int offset, String arg) throws DOMException {
		throw new DOMException((short) 0, "insert not implemented");
	}

	@Override
	public void deleteData(int offset, int count) throws DOMException {
		throw new DOMException((short) 0, "deleteData not implemented");
		// TODO Auto-generated method stub

	}

	@Override
	public void replaceData(int offset, int count, String arg) throws DOMException {
		throw new DOMException((short) 0, "replace not implemented");
	}

	@Override
	public Text splitText(int offset) throws DOMException {
		throw new DOMException((short) 0, "splitText not implemented");
	}

	@Override
	public boolean isElementContentWhitespace() {
		return false;
	}

	@Override
	public String getWholeText() {
		return getData();
	}

	@Override
	public Text replaceWholeText(String content) throws DOMException {
		throw new DOMException((short) 0, "replaceWhole not implemented");
	}

	@Override
	public short getNodeType() {
		return Node.TEXT_NODE;
	}

	@Override
	public String getNodeName() {
		return "#text";
	}

	@Override
	public String getNodeValue() throws DOMException {
		return getData();
	}

	@Override
	public void setNodeValue(String x) throws DOMException {
		setData(x);
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		setData(textContent);
	}

	@Override
	protected void finalize() throws Throwable {
		fbos.reset();
	}

}
