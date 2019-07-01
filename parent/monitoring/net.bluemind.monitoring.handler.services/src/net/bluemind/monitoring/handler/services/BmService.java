/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.handler.services;

/**
 * List of all Bluemind services started when launching the server that can be
 * monitored
 * 
 * @author vincent
 *
 */
public enum BmService {

	CORE("core", "/var/run/bm-core.pid"),

	CYRUS("cyrus", "/var/run/bm-cyrus.pid"),

	EAS("eas", "/var/run/bm-eas.pid"),

	ELASTICSEARCH("elasticsearch", "/var/run/bm-elasticsearch.pid"),

	HPS("hps", "/var/run/bm-hps.pid"),

	IPS("ips", "/var/run/bm-ips.pid"),

	LMTPD("lmtpd", "/var/run/bm-lmtpd.pid"),

	LOCATOR("locator", "/var/run/bm-locator.pid"),

	NODE("node", "/var/run/bm-node.pid"),

	TIKA("tika", "/var/run/bm-tika.pid"),

	WEBSERVER("webserver", "/var/run/bm-webserver.pid"),

	XMPP("xmpp", "/var/run/bm-xmpp.pid"),

	DAV("dav", "/var/run/bm-webserver.pid"),

	POSTFIX("postfix", "/var/spool/postfix/pid/master.pid"),

	YSNP("ysnp", "/var/run/bm-ysnp.pid"),

	PHP("php", "/var/run/bm-php-fpm.pid");

	/**
	 * Name of the service used for the information title
	 */
	private String serviceName;
	/**
	 * Location of the file
	 */
	private String file;

	private BmService(String serviceName, String fileLocation) {
		this.serviceName = serviceName;
		this.file = fileLocation;

	}

	public String getServiceName() {
		return serviceName;
	}

	public String getFile() {
		return file;
	}

	@Override
	public String toString() {
		return this.serviceName;
	}

}
