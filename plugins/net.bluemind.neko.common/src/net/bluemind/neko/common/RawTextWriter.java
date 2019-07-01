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
package net.bluemind.neko.common;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.cyberneko.html.filters.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawTextWriter extends Writer {

	private static final Logger logger = LoggerFactory.getLogger(RawTextWriter.class);

	public RawTextWriter(OutputStream out) throws IOException {
		super(out, "UTF-8");
	}

	@Override
	protected void printCharacters(XMLString text, boolean normalize) {
		logger.debug("printChars: {}", text);
		super.printCharacters(text, false);
	}

	@Override
	public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
		fPrinter.print(' ');
	}

	@Override
	public void endElement(QName element, Augmentations augs) throws XNIException {
		fPrinter.print(' ');
	}

}
