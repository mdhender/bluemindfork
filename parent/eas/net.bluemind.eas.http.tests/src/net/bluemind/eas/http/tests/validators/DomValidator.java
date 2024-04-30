/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.eas.http.tests.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.bluemind.utils.DOMUtils;

public class DomValidator<T> {

	private final Document document;

	public DomValidator(Document document) {
		this.document = document;
	}

	@SuppressWarnings("unchecked")
	public T assertNamespace(String element, String namespace) {
		Element uniqueElement = DOMUtils.getUniqueElement(document.getDocumentElement(), element);
		assertEquals(namespace, uniqueElement.getNamespaceURI());
		return (T) this;
	}

	public T assertMissingElement(String element) {
		assertNull(DOMUtils.getUniqueElement(document.getDocumentElement(), element));
		return (T) this;
	}

	public T assertElementText(String value, String... subPath) {
		Element element = DOMUtils.getUniqueElement(document.getDocumentElement(), subPath[0]);
		for (int i = 1; i < subPath.length; i++) {
			element = DOMUtils.getUniqueElement(element, subPath[i]);
		}
		assertEquals(value, DOMUtils.getElementText(element));
		return (T) this;
	}

}
