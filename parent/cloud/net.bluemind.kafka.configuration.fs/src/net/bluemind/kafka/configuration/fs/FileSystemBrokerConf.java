/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.kafka.configuration.fs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import net.bluemind.kafka.configuration.IBrokerFactory;
import net.bluemind.kafka.configuration.IKafkaBroker;

public class FileSystemBrokerConf implements IBrokerFactory {

	private IKafkaBroker brok;

	public FileSystemBrokerConf() {
		File f = new File("/etc/bm/kafka.properties");
		if (!f.exists()) {
			f = new File(System.getProperty("user.home"), "kafka.properties");
		}
		if (!f.exists()) {
			this.brok = null;
		} else {
			try {
				Properties props = new Properties();
				try (InputStream inStream = Files.newInputStream(f.toPath())) {
					props.load(inStream);
				}
				String address = props.getProperty("kafka.address");
				String lis = "plaintext://" + address + ":9093";
				this.brok = new IKafkaBroker() {

					@Override
					public String kafkaListener() {
						return lis;
					}

					@Override
					public String inspectAddress() {
						return address;
					}

				};
			} catch (IOException e) {
				e.printStackTrace();
				this.brok = null;
			}
		}
	}

	@Override
	public IKafkaBroker findAny() {
		return brok;
	}

}
