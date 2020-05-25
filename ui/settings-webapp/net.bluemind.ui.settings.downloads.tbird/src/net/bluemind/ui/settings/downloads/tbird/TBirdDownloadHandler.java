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
package net.bluemind.ui.settings.downloads.tbird;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.system.api.SysConfKeys;

public class TBirdDownloadHandler implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(TBirdDownloadHandler.class);

	public static String getExternalUrl() {
		String externalUrl = Optional
				.ofNullable(MQ.<String, String>sharedMap("system.configuration").get(SysConfKeys.external_url.name()))
				.orElse("configure.your.external.url");

		return externalUrl.trim();
	}

	@Override
	public void handle(HttpServerRequest request) {
		String externalUrl = getExternalUrl();

		StringBuilder sb = new StringBuilder(40);
		sb.append("https://");
		sb.append(externalUrl);
		String url = sb.toString();
		logger.info("external-url: " + url);

		try {
			Buffer repacked = repack(externalUrl);
			request.response().setStatusCode(200);

			request.response().headers().add("Content-Disposition",
					"attachment; filename=bm-connector-thunderbird.xpi");
			request.response().headers().add("Content-Type", "application/x-download");
			request.response().end(repacked);
		} catch (IOException e) {
			logger.error("error during generating file", e);
			request.response().setStatusCode(500);
			request.response().end("error during generating file " + e.getMessage());
		}
	}

	private Buffer repack(String serverUrl) throws IOException {
		URL resource = Activator.bundle.getResource("web-resources");

		URL url = org.eclipse.core.runtime.FileLocator.toFileURL(resource);
		File rootPath = new File(url.getFile(), "downloads/tbird.xpi");

		try (ZipFile zip = new ZipFile(rootPath.getAbsolutePath())) {

			Enumeration<? extends ZipEntry> entries = zip.entries();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ZipOutputStream zout = new ZipOutputStream(out);

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					zout.putNextEntry(new ZipEntry(entry.getName()));
				} else {
					zout.putNextEntry(new ZipEntry(entry.getName()));
					if (entry.getName().endsWith("install.rdf")) {
						replaceInstallRdf("https://" + serverUrl + "/settings/settings/download/updateRdf",
								zip.getInputStream(entry), zout);
					} else if (entry.getName().endsWith("manifest.json")) {
						replaceInstallJson("https://" + serverUrl + "/settings/settings/download/updateJson",
								zip.getInputStream(entry), zout);
					} else if (entry.getName().endsWith("defaults/preferences/bm.js")) {
						replacePreferences("https://" + serverUrl, zip.getInputStream(entry), zout);
					} else {
						ByteStreams.copy(zip.getInputStream(entry), zout);

					}

					zout.closeEntry();
				}

			}

			zout.putNextEntry(new ZipEntry("chrome/content/certs/cacert.pem"));
			addCaCert(zout);
			zout.closeEntry();
			zout.close();

			return Buffer.buffer(out.toByteArray());
		}
	}

	private void replacePreferences(String serverUrl, InputStream in, final ZipOutputStream zout) throws IOException {
		String file = new String(ByteStreams.toByteArray(in));
		String[] lines = file.split("\n");
		String prefJs = "";

		for (String line : lines) {
			if (line.contains("pref(\"extensions.bm.server\"")) {
				prefJs += "pref(\"extensions.bm.server\", \"" + serverUrl + "\");\r\n";
			} else {
				prefJs += line + "\r\n";
			}
		}

		ByteStreams.copy(new ByteArrayInputStream(prefJs.getBytes()), zout);
	}

	private void addCaCert(OutputStream out) throws FileNotFoundException, IOException {

		try (InputStream cacert = new FileInputStream(new File("/var/lib/bm-ca/cacert.pem"))) {
			ByteStreams.copy(cacert, out);
		}

	}

	private void replaceInstallRdf(String updatRdfUrl, InputStream in, ZipOutputStream zout) throws IOException {
		String file = new String(ByteStreams.toByteArray(in));
		String[] lines = file.split("\n");
		String installRdfContent = "";

		for (String line : lines) {
			if (line.contains("<em:updateURL>")) {
				installRdfContent += "<em:updateURL>" + updatRdfUrl + "</em:updateURL>\r\n";
			} else {
				installRdfContent += line + "\r\n";
			}

		}

		ByteStreams.copy(new ByteArrayInputStream(installRdfContent.getBytes()), zout);
	}

	private void replaceInstallJson(String updatJsonUrl, InputStream in, ZipOutputStream zout) throws IOException {
		String file = new String(ByteStreams.toByteArray(in));
		String[] lines = file.split("\n");
		String manifestContent = "";

		for (String line : lines) {
			if (line.contains("update_url")) {
				manifestContent += "\"update_url\": \"" + updatJsonUrl + "\"\r\n";
			} else {
				manifestContent += line + "\r\n";
			}

		}

		ByteStreams.copy(new ByteArrayInputStream(manifestContent.getBytes()), zout);
	}
}
