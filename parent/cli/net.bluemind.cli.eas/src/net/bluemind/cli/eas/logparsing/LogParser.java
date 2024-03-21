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
package net.bluemind.cli.eas.logparsing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.bluemind.cli.cmd.api.CliContext;

public class LogParser {

	private static final String SEPARATOR = "__";
	private final String content;
	private final ILogHandler logHandler;
	private final CliContext ctx;

	private final Map<String, RidProcessing> rids;
	private String currentRid;
	private String currentDate;
	private PARSER_STATE state;

	public LogParser(String content, ILogHandler logHandler, CliContext ctx) {
		this.content = content;
		this.logHandler = logHandler;
		this.ctx = ctx;
		rids = new HashMap<>();
	}

	public void parse() throws Exception {
		this.state = PARSER_STATE.UNKNOWN;
		String[] lines = content.split("\\r?\\n");
		for (String line : lines) {
			handleLine(line);
		}
	}

	private void handleLine(String line) throws Exception {
		if (line.contains("document from device:")) {
			currentRid = extractRid(line);
			currentDate = extractDate(line);
			state = PARSER_STATE.CONTENT;
			rids.put(currentRid, new RidProcessing(TYPE.REQUEST, new StringBuilder()));
			return;
		}

		if (line.contains("wbxml sent to device:")) {
			currentRid = extractRid(line);
			state = PARSER_STATE.CONTENT;
			rids.put(currentRid, new RidProcessing(TYPE.RESPONSE, new StringBuilder()));
			return;
		}

		if (line.contains("WrappedResponse") && line.contains("cmd: Sync")) {
			String rid = extractRid(line);
			String code = extractHttpCode(line);
			logHandler.syncResponseProcessed(rid, code);
			state = PARSER_STATE.UNKNOWN;
			return;
		}

		if (state == PARSER_STATE.CONTENT) {
			if (line.isBlank()) {
				RidProcessing ridProcessing = rids.get(currentRid);
				ISyncInfo info = parseData(ridProcessing.data());
				if (info.isSync()) {
					SyncInfo sync = (SyncInfo) info;
					if (ridProcessing.state == TYPE.REQUEST) {
						logHandler.syncRequest(currentRid, currentDate, sync);
					} else {
						logHandler.syncResponse(currentRid, sync);
					}
				} else {
					state = PARSER_STATE.UNKNOWN;
				}
			} else {
				rids.get(currentRid).data().append(line + "\r\n");
			}
		}
	}

	private ISyncInfo parseData(StringBuilder data) throws SAXException, IOException, ParserConfigurationException {
		String xml = data.toString();
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml.getBytes()));

		Node root = document.getChildNodes().item(0);
		if (!root.getNodeName().equals("Sync")) {
			return new NoSyncInfo();
		}

		String syncKey = findByName(root, "SyncKey");
		String collectionId = findByName(root, "CollectionId");

		if (syncKey == null || collectionId == null) {
			ctx.info("Cannot find syncKey or collectionId in data: {}", xml);
			return new NoSyncInfo();
		}

		if (collectionId.contains(SEPARATOR)) {
			collectionId = collectionId.split(SEPARATOR)[1];
		}
		return new SyncInfo(syncKey, collectionId, xml);
	}

	private String findByName(Node element, String name) {
		if (element.getNodeName().equals(name)) {
			return element.getFirstChild().getNodeValue();
		}
		NodeList children = element.getChildNodes();
		if (children == null || children.getLength() == 0) {
			return null;
		}
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String byName = findByName(child, name);
			if (byName != null) {
				return byName;
			}
		}
		return null;
	}

	private String extractHttpCode(String line) {
		int beginIndex = line.indexOf("http.out: ") + 10;
		int endIndex = line.indexOf("]", beginIndex);
		return line.substring(beginIndex, endIndex);
	}

	private String extractDate(String line) {
		return line.substring(0, line.indexOf(","));
	}

	private String extractRid(String line) {
		int beginIndex = line.indexOf("rid: ") + 5;
		int endIndex = line.indexOf(",", beginIndex);
		return line.substring(beginIndex, endIndex);
	}

	record RidProcessing(TYPE state, StringBuilder data) {

	}

	record SyncInfo(String syncKey, String collectionId, String xml) implements ISyncInfo {

		@Override
		public boolean isSync() {
			return true;
		}

	}

	record NoSyncInfo() implements ISyncInfo {

		@Override
		public boolean isSync() {
			return false;
		}
	}

	interface ISyncInfo {
		public boolean isSync();
	}

	enum TYPE {
		REQUEST, RESPONSE
	}

	enum PARSER_STATE {
		UNKNOWN, CONTENT
	}
}
