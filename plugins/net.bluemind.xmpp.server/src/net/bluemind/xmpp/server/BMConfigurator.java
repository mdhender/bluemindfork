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
package net.bluemind.xmpp.server;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tigase.conf.ConfigurationException;
import tigase.conf.Configurator;
import tigase.conf.ConfiguratorAbstract;
import tigase.db.TigaseDBException;

public class BMConfigurator extends Configurator {

	private static Logger logger = LoggerFactory.getLogger(BMConfigurator.class);

	@Override
	public void init(String[] args) throws ConfigurationException, TigaseDBException {
		logger.info("gogo bm config {}", Arrays.asList(args));
		super.init(new String[] { ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY,
				"/usr/share/bm-xmpp/etc/init.properties" });
	}

}
