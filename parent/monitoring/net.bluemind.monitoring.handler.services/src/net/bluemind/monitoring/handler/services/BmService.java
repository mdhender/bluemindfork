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

	CORE("bm-core", "Core"),

	CYRUS("bm-cyrus-imapd", "Cyrus"),

	EAS("bm-eas", "Eas"),

	ELASTICSEARCH("bm-elasticsearch", "Elasticsearch"),

	HPS("bm-hps", "Hps"),

	LMTPD("bm-lmtpd", "Lmtpd"),

	NODE("bm-node", "Node"),

	TIKA("bm-tika", "Tika"),

	WEBSERVER("bm-webserver", "Webserver"),

	XMPP("bm-xmpp", "Xmpp"),

	DAV("bm-webserver", "Dav"),

	YSNP("bm-ysnp", "Ysnp"),

	PHP("bm-php-fpm", "Php");

	/**
	 * Name of the systemd service
	 */
	private String serviceName;
	public String className;

	private BmService(String serviceName, String className) {
		this.serviceName = serviceName;
		this.className = className;
	}

	public String getServiceName() {
		return serviceName;
	}

	@Override
	public String toString() {
		return this.serviceName;
	}

    public static BmService fromString(String serviceName) {
	    for (BmService s : BmService.values()) {
            if (s.serviceName.equalsIgnoreCase(serviceName)) {
                return s;
            }
        }
        return null;
    }

}
