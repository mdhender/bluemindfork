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
package net.bluemind.eas.serdes.resolverecipients;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsRequest;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsRequest.Options;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsRequest.Options.Availability;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsRequest.Options.CertificateRetrieval;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsRequest.Options.Picture;
import net.bluemind.eas.serdes.IEasRequestParser;

public class ResolveRecipientsRequestParser implements IEasRequestParser<ResolveRecipientsRequest> {

	private static final Logger logger = LoggerFactory.getLogger(ResolveRecipientsRequestParser.class);

	@Override
	public ResolveRecipientsRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		ResolveRecipientsRequest rrr = new ResolveRecipientsRequest();

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
			case "To":
				rrr.to.add(child.getTextContent());
				break;
			case "Options":
				rrr.options = parseOptions(child);
				break;
			default:
				logger.warn("Not managed ResolveRecipients child {}", child);
				break;
			}
		}

		return rrr;
	}

	private Options parseOptions(Element el) {
		Options options = new Options();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "CertificateRetrieval":
				options.certificateRetrieval = CertificateRetrieval.get(child.getTextContent());
				break;
			case "MaxCertificates":
				options.maxCertificates = Integer.parseInt(child.getTextContent());
				break;
			case "MaxAmbiguousRecipients":
				options.maxAmbiguousRecipients = Integer.parseInt(child.getTextContent());
				break;
			case "Availability":
				options.availability = parseAvailability(child);
				break;
			case "Picture":
				options.picture = parsePicture(child);
				break;
			default:
				logger.warn("Not managed ResolveRecipients.Options child {}", child);
				break;
			}
		}

		return options;
	}

	private Availability parseAvailability(Element el) {
		Availability availability = new Availability();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		// date.setTimeZone(TimeZone.getTimeZone("GMT"));

		NodeList children = el.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "StartTime":
				try {
					availability.startTime = sdf.parse(child.getTextContent());
				} catch (ParseException e) {
					logger.error(e.getMessage(), e);
				}
				break;
			case "EndTime":
				try {
					availability.endTime = sdf.parse(child.getTextContent());
				} catch (ParseException e) {
					logger.error(e.getMessage(), e);
				}
				break;
			default:
				logger.warn("Not managed ResolveRecipients.Options.Availability child {}", child);
				break;
			}
		}

		return availability;
	}

	private Picture parsePicture(Element el) {
		Picture picture = new Picture();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "MaxSize":
				picture.maxSize = Integer.parseInt(child.getTextContent());
				break;
			case "MaxPictures":
				picture.maxPictures = Integer.parseInt(child.getTextContent());

				break;
			default:
				logger.warn("Not managed ResolveRecipients.Options.Picture child {}", child);
				break;
			}
		}

		return picture;
	}

}
