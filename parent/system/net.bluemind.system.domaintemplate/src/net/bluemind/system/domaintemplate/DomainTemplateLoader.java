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
package net.bluemind.system.domaintemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.DomainTemplate.Description;
import net.bluemind.system.api.DomainTemplate.Kind;
import net.bluemind.system.api.DomainTemplate.Tag;
import net.bluemind.utils.DOMUtils;

public class DomainTemplateLoader {

	public static DomainTemplate load(InputStream in) throws ServerFault {
		Document doc;
		try {
			doc = DOMUtils.parse(in);
		} catch (Exception t) {
			throw new ServerFault(t);
		}

		Element root = doc.getDocumentElement();
		Element ht = DOMUtils.getUniqueElement(root, "hostTags");
		NodeList kindNodes = ht.getElementsByTagName("kind");
		List<DomainTemplate.Kind> kinds = new ArrayList<>(kindNodes.getLength());
		for (int i = 0; i < kindNodes.getLength(); i++) {
			Element ekind = (Element) kindNodes.item(i);
			String id = ekind.getAttribute("id");
			Kind kind = new DomainTemplate.Kind();
			kind.id = id;
			kind.description = loadDescription(ekind);
			kind.tags = loadTags(ekind);
			kinds.add(kind);
		}

		DomainTemplate ret = new DomainTemplate();
		ret.kinds = kinds;
		return ret;
	}

	private static List<Tag> loadTags(Element ekind) {

		NodeList tagNodes = ekind.getElementsByTagName("tag");

		List<Tag> ret = new ArrayList<>();

		for (int i = 0; i < tagNodes.getLength(); i++) {
			ret.add(loadTag((Element) tagNodes.item(i)));
		}
		return ret;
	}

	private static Tag loadTag(Element tagElement) {
		Tag t = new Tag();
		t.description = loadDescription(tagElement);
		t.value = tagElement.getAttribute("value");
		t.autoAssign = "true".equals(tagElement.getAttribute("autoAssign"));
		t.mandatory = "true".equals(tagElement.getAttribute("mandatory"));
		t.multival = "true".equals(tagElement.getAttribute("multival"));
		return t;
	}

	private static Description loadDescription(Element e) {

		NodeList nodes = e.getElementsByTagName("desc");

		Description ret = new Description();
		ret.i18n = new ArrayList<>(nodes.getLength());

		for (int i = 0; i < nodes.getLength(); i++) {
			Element d = (Element) nodes.item(i);
			Description.I18NDescription i18n = new Description.I18NDescription();
			i18n.lang = d.getAttribute("lang");
			i18n.text = d.getTextContent();
			ret.i18n.add(i18n);
		}
		return ret;
	}
}
