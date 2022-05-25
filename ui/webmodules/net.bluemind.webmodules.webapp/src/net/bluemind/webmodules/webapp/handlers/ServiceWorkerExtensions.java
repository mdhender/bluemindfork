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
package net.bluemind.webmodules.webapp.handlers;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import com.google.common.net.HttpHeaders;

import io.netty.util.AsciiString;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.webmodule.server.PreEncodedObject;

public class ServiceWorkerExtensions implements Handler<HttpServerRequest> {
	private static final PreEncodedObject extensions;

	static {
		extensions = new PreEncodedObject(loadExtensions());
	}
	/**
	 * {@code "text/javascript"}
	 */
	public static final AsciiString TEXT_JAVASCRIPT = AsciiString.cached("text/javascript");

	@Override
	public void handle(final HttpServerRequest request) {
		StringBuffer output = new StringBuffer();
		output.append("self.WebApp.$extensions.load(");
		output.append(extensions.json());
		output.append(");");
		request.response().putHeader(HttpHeaders.CONTENT_TYPE, TEXT_JAVASCRIPT).setStatusCode(200)
				.end(output.toString());
	}

	private static JsonObject loadExtensions() {
		IExtensionPoint swExtensions = Platform.getExtensionRegistry().getExtensionPoint("serviceworker.extension");
		if (swExtensions == null) {
			return new JsonObject();
		}
		return Arrays.asList(swExtensions.getExtensions()).stream()
				.flatMap(swExtension -> Stream.of(swExtension.getConfigurationElements()))
				.map(registerPoint -> Platform.getExtensionRegistry()
						.getExtensionPoint(registerPoint.getAttribute("id")))
				.filter(Objects::nonNull).reduce(new JsonObject(),
						(object, point) -> object.put(point.getUniqueIdentifier(), toJson(point)),
						(object, point) -> new JsonObject());

	}

	private static JsonArray toJson(IExtensionPoint point) {
		return Arrays.asList(point.getExtensions()).stream().map(ServiceWorkerExtensions::toJson).reduce(new JsonArray(),
				(array, extension) -> array.add(extension), (a, b) -> new JsonArray());
	}

	private static JsonObject toJson(IExtension extension) {
		return toJson(extension.getConfigurationElements()).put("bundle", extension.getContributor().getName());

	}

	private static JsonObject toJson(IConfigurationElement[] elements) {
		JsonObject root = new JsonObject();
		for (IConfigurationElement element : elements) {
			root.put(element.getName(), toJson(element));
		}
		return root;
	}

	private static JsonObject toJson(IConfigurationElement element) {
		JsonObject data = new JsonObject();
		for (String atttribute : element.getAttributeNames()) {
			data.put(atttribute, element.getAttribute(atttribute));
		}
		if (element.getValue() != null) {
			data.put("body", element.getValue());
		}
		if (element.getChildren() != null) {
			data.put("children", toJson(element.getChildren()));
		}
		return data;
	}

}
