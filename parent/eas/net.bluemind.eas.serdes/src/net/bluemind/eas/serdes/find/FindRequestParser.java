/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.eas.serdes.find;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Range;
import net.bluemind.eas.dto.find.FindRequest;
import net.bluemind.eas.dto.find.FindRequest.ExecuteSearch;
import net.bluemind.eas.dto.find.FindRequest.ExecuteSearch.GALSearchCriterion;
import net.bluemind.eas.dto.find.FindRequest.ExecuteSearch.MailBoxSearchCriterion;
import net.bluemind.eas.dto.find.FindRequest.Options;
import net.bluemind.eas.dto.find.FindRequest.Options.Picture;
import net.bluemind.eas.dto.find.FindRequest.Query;
import net.bluemind.eas.serdes.IEasRequestParser;
import net.bluemind.eas.utils.EasLogUser;

public class FindRequestParser implements IEasRequestParser<FindRequest> {

	private static final Logger logger = LoggerFactory.getLogger(FindRequestParser.class);

	@Override
	public FindRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past, String user) {
		FindRequest request = new FindRequest();

		Element elements = doc.getDocumentElement();
		NodeList children = elements.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "SearchId":
				request.searchId = child.getTextContent();
				break;
			case "ExecuteSearch":
				request.executeSearch = parseExecuteSearch(child, user);
				break;
			default:
				EasLogUser.logWarnAsUser(user, logger, "Not managed FindRequest child {}", child);
				break;
			}
		}

		return request;
	}

	private ExecuteSearch parseExecuteSearch(Element element, String user) {
		ExecuteSearch executeSearch = new ExecuteSearch();

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "MailBoxSearchCriterion":
				executeSearch.mailBoxSearchCriterion = parseMailBoxSearchCriterion(child, user);
				break;
			case "GALSearchCriterion":
				executeSearch.galSearchCriterion = parseGALSearchCriterion(child, user);
				break;
			default:
				EasLogUser.logWarnAsUser(user, logger, "Not managed FindRequest child {}", child);
			}

		}

		return executeSearch;
	}

	private MailBoxSearchCriterion parseMailBoxSearchCriterion(Element element, String user) {
		MailBoxSearchCriterion mailBoxSearchCriterion = new MailBoxSearchCriterion();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "Query":
				mailBoxSearchCriterion.query = parseQuery(child, user);
				break;
			case "Options":
				mailBoxSearchCriterion.options = parseOptions(child, user);
				break;
			default:
				EasLogUser.logWarnAsUser(user, logger, "Not managed MailBoxSearchCriterion child {}", child);
				break;
			}
		}

		return mailBoxSearchCriterion;
	}

	private GALSearchCriterion parseGALSearchCriterion(Element element, String user) {
		GALSearchCriterion galSearchCriterion = new GALSearchCriterion();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "Query":
				galSearchCriterion.query = parseQuery(child, user);
				break;
			case "Options":
				galSearchCriterion.options = parseOptions(child, user);
				break;
			default:
				EasLogUser.logWarnAsUser(user, logger, "Not managed GALSearchCriterion child {}", child);
				break;
			}
		}

		return galSearchCriterion;
	}

	private Query parseQuery(Element element, String user) {
		Query query = new Query();

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "FreeText":
				query.freeText = child.getTextContent();
				break;
			case "Class":
				/**
				 * MS-ASCMD 2.2.3.27.1 Class (Find) In Find command requests, the only supported
				 * value for the airsync:Class element is "Email".
				 */
				// query.clazz = child.getTextContent();
				break;
			case "CollectionId":
				query.collectionId = child.getTextContent();
				break;
			default:
				EasLogUser.logWarnAsUser(user, logger, "Not managed Query child {}", child);
				break;
			}
		}

		return query;
	}

	private Options parseOptions(Element element, String user) {
		Options options = new Options();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "DeepTraversal":
				options.deepTraversal = true;
				break;
			case "Range":
				String[] range = child.getTextContent().split("-");
				Range r = new Range();
				r.min = Integer.parseInt(range[0]);
				r.max = Integer.parseInt(range[1]);
				options.range = r;
				break;
			case "Picture":
				options.picture = parsePicture(child, user);
				break;
			default:
				EasLogUser.logWarnAsUser(user, logger, "Not managed Options child: '{}'", child);
				break;
			}
		}

		return options;
	}

	private Picture parsePicture(Element el, String user) {
		Picture p = new Picture();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "MaxPictures":
				p.maxPictures = Integer.parseInt(child.getTextContent());
				break;
			case "MaxSize":
				p.maxSize = Integer.parseInt(child.getTextContent());
				break;
			default:
				EasLogUser.logWarnAsUser(user, logger, "Not managed Picture child {}", child);
				break;
			}
		}

		return p;
	}

}
