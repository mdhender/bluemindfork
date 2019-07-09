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
package net.bluemind.webmodule.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class WebModule {

	public String root;
	public String rootFile;
	public String index = "index.html";

	public Handler<HttpServerRequest> defaultHandler;
	public Map<String, Handler<HttpServerRequest>> handlers = new HashMap<>();

	public List<JsEntry> js = new ArrayList<JsEntry>();
	public List<String> css = new ArrayList<String>();
	public List<WebResource> resources = new ArrayList<WebResource>();

}
