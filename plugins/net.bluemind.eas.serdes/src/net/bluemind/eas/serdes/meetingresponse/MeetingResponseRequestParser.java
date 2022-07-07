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
package net.bluemind.eas.serdes.meetingresponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.meetingresponse.MeetingResponseRequest;
import net.bluemind.eas.dto.meetingresponse.MeetingResponseRequest.Request;
import net.bluemind.eas.serdes.IEasRequestParser;

public class MeetingResponseRequestParser implements IEasRequestParser<MeetingResponseRequest> {

	private static final Logger logger = LoggerFactory.getLogger(MeetingResponseRequestParser.class);

	@Override
	public MeetingResponseRequest parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past) {
		MeetingResponseRequest req = new MeetingResponseRequest();

		Element elements = doc.getDocumentElement();
		NodeList children = elements.getChildNodes();

		req.requests = new ArrayList<MeetingResponseRequest.Request>(children.getLength());

		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element child = (Element) n;
			String childName = child.getNodeName();
			switch (childName) {
			case "Request":
				req.requests.add(parseRequest(child));
				break;
			default:
				logger.warn("Not managed MeetingResponse child {}", child);
				break;
			}
		}

		return req;
	}

	private Request parseRequest(Element el) {
		MeetingResponseRequest.Request req = new Request();

		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element child = (Element) n;
			String childName = child.getNodeName();

			switch (childName) {
			case "UserResponse":
				req.userResponse = Request.UserResponse.get(child.getTextContent());
				break;
			case "CollectionId":
				req.collectionId = child.getTextContent();
				break;
			case "RequestId":
				req.requestId = sanitizeRequestId(child.getTextContent());
				break;
			case "LongId":
				req.LongId = Integer.parseInt(child.getTextContent());
				break;
			case "InstanceId":
				req.instanceId = parseDate(child.getTextContent());
				break;
			default:
				logger.warn("Not managed MeetingResponse.Request child {}", child);
				break;
			}
		}

		return req;
	}

	private String sanitizeRequestId(String requestId) {
		// iOS sometimes sends
		// collectionId:itemId&lt;!ExceptionDate!&gt;20010101T000000Z as requestId
		// collectionId:itemId is expected
		if (requestId.contains("&lt;!ExceptionDate!&gt;")) {
			requestId = requestId.substring(0, requestId.indexOf("&lt;!ExceptionDate!&gt;"));
		}
		return requestId;

	}

	private Date parseDate(String str) {
		SimpleDateFormat date;
		try {
			if (str.matches("^....-..-..T..:..:..\\....Z$")) {
				date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				date.setTimeZone(TimeZone.getTimeZone("GMT"));
				return date.parse(str);
			} else if (str.matches("^........T......Z$")) {
				date = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
				date.setTimeZone(TimeZone.getTimeZone("GMT"));
				return date.parse(str);
			}
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		}

		return null;
	}

}
