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

import java.util.Collection;

/**
 * Global proxy configuration object. Defines which locations are forwarded to
 * another site, and which templates are shown when a login is required or a
 * location is not handled.
 * 
 * 
 */
public class HPSConfiguration {

	private Collection<ForwardedLocation> forwardedLocations;

	private TemplatesConfiguration templatesConfiguration;

	private CookiePreferences cookiePreferences;

	private int port;

	public HPSConfiguration() {

	}

	public Collection<ForwardedLocation> getForwardedLocations() {
		return forwardedLocations;
	}

	public void setForwardedLocations(Collection<ForwardedLocation> forwardedLocations) {
		this.forwardedLocations = forwardedLocations;
	}

	public TemplatesConfiguration getTemplatesConfiguration() {
		return templatesConfiguration;
	}

	public void setTemplatesConfiguration(TemplatesConfiguration templatesConfiguration) {
		this.templatesConfiguration = templatesConfiguration;
	}

	public CookiePreferences getCookiePreferences() {
		return cookiePreferences;
	}

	public void setCookiePreferences(CookiePreferences cookiePreferences) {
		this.cookiePreferences = cookiePreferences;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
