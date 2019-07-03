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
package net.bluemind.eas.impl.vertx.compat;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.w3c.dom.Document;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.http.EasHeaders;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.utils.DOMDumper;
import net.bluemind.eas.utils.FileUtils;
import net.bluemind.eas.validation.ValidationException;
import net.bluemind.eas.validation.Validator;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.eas.wbxml.WbxmlOutput;

public final class VertxResponder implements Responder {

	private final HttpServerResponse resp;
	private final VertxOutput output;
	private static final Logger logger = LoggerFactory.getLogger(VertxResponder.class);

	private static final CharSequence HEADER_MS_SERVER = HttpHeaders.createOptimized("14.3");
	private static final CharSequence HEADER_SERVER = HttpHeaders.createOptimized("Microsoft-IIS/7.5");
	private static final CharSequence HEADER_CACHE = HttpHeaders.createOptimized("private");
	private static final CharSequence HEADER_WBXML_CONTENT = HttpHeaders
			.createOptimized("application/vnd.ms-sync.wbxml");
	private Vertx vertx;

	public VertxResponder(HttpServerRequest req, HttpServerResponse resp) {
		this(req, resp, null);
	}

	public VertxResponder(HttpServerRequest req, HttpServerResponse resp, Vertx vertx) {
		this.resp = resp;
		this.output = new VertxOutput(req);
		this.vertx = vertx;
	}

	private void setASHeaders(ConnectionHeader connection) {
		resp.putHeader(EasHeaders.Server.MS_SERVER, HEADER_MS_SERVER);
		resp.putHeader(HttpHeaders.SERVER, HEADER_SERVER);
		resp.putHeader(HttpHeaders.CACHE_CONTROL, HEADER_CACHE);
		resp.putHeader(HttpHeaders.CONNECTION, connection.value);
	}

	public HttpServerResponse response() {
		return resp;
	}

	@Override
	public void sendResponse(NamespaceMapping ns, Document doc, ConnectionHeader con) {
		DOMDumper.dumpXml(logger, "to pda:\n", doc);
		try {
			Validator.get().checkResponse(14.1, doc);
			setASHeaders(con);
			resp.putHeader(HttpHeaders.CONTENT_TYPE, HEADER_WBXML_CONTENT);
			resp.setChunked(true);
			WBXMLTools.toWbxml(ns.namespace(), doc, output);
		} catch (ValidationException ve) {
			logger.error("EAS is trying to send a non-conforming response: " + ve.getMessage(), ve);
			setASHeaders(con);
			resp.setStatusCode(500).setStatusMessage(ve.getMessage() != null ? ve.getMessage() : "null").end();
		} catch (IOException ioe) {
			logger.error("Error generating wbxml for ns " + ns.namespace(), ioe);
			setASHeaders(con);
			resp.setStatusCode(500).setStatusMessage(ioe.getMessage() != null ? ioe.getMessage() : "null").end();
		}
	}

	@Override
	public void sendResponse(NamespaceMapping ns, Document doc) {
		sendResponse(ns, doc, ConnectionHeader.close);
	}

	@Override
	public void sendResponseFile(String contentType, InputStream file) throws IOException {
		byte[] b = FileUtils.streamBytes(file, true);
		resp.headers().add(HttpHeaders.CONTENT_TYPE, contentType);
		setASHeaders(ConnectionHeader.close);
		resp.end(new Buffer(b));
	}

	@Override
	public void sendStatus(int statusCode) {
		logger.info("to pda:\nHTTP {}\n", statusCode);
		setASHeaders(ConnectionHeader.close);
		resp.setStatusCode(statusCode).end();
	}

	@Override
	public WbxmlOutput asOutput(ConnectionHeader connection) {
		setASHeaders(connection);
		resp.putHeader(HttpHeaders.CONTENT_TYPE, HEADER_WBXML_CONTENT);
		resp.setChunked(true);
		return output;
	}

	@Override
	public WbxmlOutput asOutput() {
		return asOutput(ConnectionHeader.keepAlive);
	}

	@Override
	public Vertx vertx() {
		return vertx;
	}

}
