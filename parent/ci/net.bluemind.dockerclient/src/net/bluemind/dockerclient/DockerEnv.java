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
package net.bluemind.dockerclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DockerEnv {

	private static final Logger logger = LoggerFactory.getLogger(DockerEnv.class);

	private static final ObjectMapper mapper = new ObjectMapper();
	private static List<Image> images;
	private static Map<String, String> imageIp = new HashMap<String, String>();

	private static AsyncHttpClient httpClient;

	private static class HttpHost {
		String host;
		int port;

		public HttpHost(String h, int p) {
			this.host = h;
			this.port = p;
		}

		public String path(String p) {
			return "http://" + host + ":" + port + p;
		}
	}

	private static HttpHost dockerHost;

	static {
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public String getImageIp(String imageName) {
		return null;
	}

	public static String getIp(String imageName) {
		return imageIp.get(imageName);
	}

	private static void init() throws Exception {
		String home = System.getProperty("user.home");
		File f = new File(home + "/.docker.io.properties");

		httpClient = new DefaultAsyncHttpClient();
		URL dockerUrl = new URL("http://localhost:4243");
		if (f.exists()) {
			logger.info("load docker conf from ~/.docker.io.properties");
			Properties p = new Properties();
			try (InputStream pfile = new FileInputStream(f)) {
				p.load(pfile);
			}
			String urlString = p.getProperty("docker.io.url");
			if (urlString != null) {
				dockerUrl = new URL(urlString);
			}
			// builder.uri(p.getProperty("docker.io.url"));
		} else {
			logger.info("load docker conf from env");
			// builder = DefaultDockerClient.fromEnv();
		}

		dockerHost = new HttpHost(dockerUrl.getHost(), dockerUrl.getPort() != -1 ? dockerUrl.getPort() : 80);

		images = loadImages(new File(""));

		for (Image i : images) {
			String ip = retrieveIp(i);
			logger.info("container [{}] ip: {}", i.getName(), ip);
			imageIp.put(i.getActualName(), ip);
		}
	}

	public static Map<String, String> getImagesMap() {
		return imageIp;
	}

	private static String retrieveIp(Image image) throws Exception {
		String imageName = image.getActualName();
		String name = imageName;
		name = imageName.replaceAll("\\:", "_").replace("/", "_");
		name = name + "-junit";

		try {
			Response resp = httpClient.prepareGet(dockerHost.path("/containers/" + name + "/json")).execute().get();
			logger.debug("containers/{}/json", name);

			// NetworkSettings
			// IPAddress
			JsonNode c = mapper.readTree(resp.getResponseBody());
			logger.debug("{}", c);
			if (c.get("NetworkSettings") == null) {
				return null;
			}
			return c.get("NetworkSettings").get("IPAddress").asText();

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	public List<Image> getImages() {
		return images;
	}

	public static List<Image> loadImages(File root) {
		File ciJson = new File(root.getAbsoluteFile(), "services.json");
		if (!ciJson.exists()) {
			return Collections.emptyList();
		}
		try {
			List<Image> ret = mapper.readValue(ciJson,

					new TypeReference<List<Image>>() {
					});
			return ret;
		} catch (Exception e) {
			e.printStackTrace();

			return Collections.emptyList();
		}
	}

}
