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
package net.bluemind.eas.serdes.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.search.Range;
import net.bluemind.eas.dto.search.SearchRequest;
import net.bluemind.eas.dto.search.SearchRequest.Store;
import net.bluemind.eas.dto.search.SearchRequest.Store.Options;
import net.bluemind.eas.dto.search.SearchRequest.Store.Options.Picture;
import net.bluemind.eas.dto.search.SearchRequest.Store.Query;
import net.bluemind.eas.dto.search.SearchRequest.Store.Query.And;
import net.bluemind.eas.dto.search.StoreName;
import net.bluemind.eas.serdes.IEasRequestParser;
import net.bluemind.eas.serdes.base.BodyOptionsParser;

public class SearchRequestParser implements IEasRequestParser<SearchRequest> {

	private static final Logger logger = LoggerFactory.getLogger(SearchRequestParser.class);

	@Override
	public SearchRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		SearchRequest sr = new SearchRequest();

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
			case "Store":
				sr.store = parseStore(child);
				break;
			default:
				logger.warn("Not managed SearchRequest child {}", child);
				break;
			}
		}

		return sr;
	}

	private Store parseStore(Element el) {
		Store store = new Store();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "Name":
				try {
					store.name = StoreName.valueOf(child.getTextContent().toLowerCase());
				} catch (IllegalArgumentException e) {
					logger.error("Unknown StoreName {}", child.getTextContent());
				}
				break;
			case "Query":
				store.query = parseQuery(child);
				break;
			case "Options":
				store.options = parseOptions(child);
			default:
				logger.warn("Not managed SearchRequest.Store child {}", child);
				break;
			}
		}

		return store;
	}

	private Options parseOptions(Element el) {
		Options options = new Options();
		BodyOptionsParser bop = new BodyOptionsParser();
		options.bodyOptions = bop.fromOptionsElement(el);
		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "RebuildResult":
				options.rebuildResults = true;
				break;
			case "DeepTraversal":
				options.deepTraversal = true;
				break;
			case "Range":
				String range[] = child.getTextContent().split("-");
				Range r = new Range();
				r.min = Integer.parseInt(range[0]);
				r.max = Integer.parseInt(range[1]);
				options.range = r;
				break;
			case "Picture":
				options.picture = parsePicture(child);
				break;
			default:
				logger.warn("Not managed SearchRequest.Options child {}", child);
				break;
			}
		}

		return options;
	}

	private Query parseQuery(Element el) {
		Query query = new Query();
		NodeList children = el.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				// GAL
				query.value = n.getNodeValue();
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "And":
				query.and = parseAnd(child);
				break;
			default:
				logger.warn("Not managed SearchRequest.Query child {}", child);
				break;
			}
		}

		return query;
	}

	private And parseAnd(Element el) {
		And and = new And();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "Class":
				and.clazz = child.getTextContent();
				break;
			case "FreeText":
				and.freeText = child.getTextContent();
				break;
			case "CollectionId":
				and.collectionId = child.getTextContent();
				break;
			default:
				logger.warn("Not managed SearchRequest.Query.And child {}", child);
				break;
			}
		}

		return and;
	}

	private Picture parsePicture(Element el) {
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
				logger.warn("Not managed SearchRequest.Options.Picture child {}", child);
				break;
			}
		}

		return p;
	}

}
