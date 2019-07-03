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
package net.bluemind.proxy.http.config;

public class TemplatesConfiguration {

	private String path;
	private String url;
	private String defaultDomain;

	public TemplatesConfiguration(String path, String url) {
		this.path = path;
		this.url = url;
	}

	/**
	 * Returns a default forward that will be used when none of the configured
	 * forward matches user's request.
	 * 
	 * May return null. In this case we will return a 404 error to the browser.
	 * 
	 * @return
	 */
	public ForwardedLocation getDefaultLocation() {
		return null;
	}

	/**
	 * The path to a login.xml file.
	 * 
	 * Path will be resolved using a classloader if the path is relative, and on
	 * filesystem if it starts with a '/'
	 * 
	 * @return
	 */
	public String getLoginFormPath() {
		return path + "/login.ftl";
	}

	public String getUrl() {
		return url;
	}

	public String getPath() {
		return path;
	}

	public String getDefaultDomain() {
		return defaultDomain;
	}

	public void setDefaultDomain(String defaultDomain) {
		this.defaultDomain = defaultDomain;
	}

}
