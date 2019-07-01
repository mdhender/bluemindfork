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
package net.bluemind.ysnp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YSNPConfiguration {

	public static final String CFG = "/etc/ysnp/ysnp.conf";
	private Properties conf;
	private static final Logger logger = LoggerFactory.getLogger(YSNPConfiguration.class);

	public YSNPConfiguration() throws FileNotFoundException, IOException, InterruptedException {
		initSaslauthdSocketDir();
		initPtsockSocketDir();

		conf = new Properties();
		conf.put("daemon.socket.path", "/var/run/saslauthd/mux");
		conf.put("bmpt-daemon.socket.path", "/var/run/cyrus/socket/bm-ptsock");
		conf.put("daemon.threads", 2 + Runtime.getRuntime().availableProcessors() + "");
		try {
			FileInputStream in = new FileInputStream(CFG);
			conf.load(in);
			in.close();
		} catch (Exception e) {
			logger.warn(CFG + " file not found. using default settings");
		}
	}

	private void initPtsockSocketDir() throws IOException, InterruptedException {
		File cyrusDir = new File("/var/run/cyrus");
		File socketDir = new File("/var/run/cyrus/socket");

		if (!cyrusDir.isDirectory()) {
			Process p = Runtime.getRuntime().exec("/bin/rm -rf " + cyrusDir.getPath());
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				logger.warn("Unable to remove: " + cyrusDir.getPath());
				throw e;
			}
		}

		if (!socketDir.isDirectory()) {
			Process p = Runtime.getRuntime().exec("/bin/rm -rf " + socketDir.getPath());
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				logger.warn("Unable to remove: " + socketDir.getPath());
				throw e;
			}

			socketDir.mkdirs();
		}

		try {
			Process p = Runtime.getRuntime().exec("chown cyrus:mail " + cyrusDir.getPath());
			p.waitFor();

			p = Runtime.getRuntime().exec("chown cyrus:mail " + socketDir.getPath());
			p.waitFor();
		} catch (InterruptedException e) {
			logger.warn("Unable to set permissions on: " + socketDir.getPath());
			throw e;
		}
	}

	private void initSaslauthdSocketDir() throws IOException, InterruptedException {
		File runDir = new File("/var/run/saslauthd");
		if (runDir.isDirectory()) {
			Process p = Runtime.getRuntime().exec("/bin/rm -rf " + runDir.getPath());
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				logger.warn("Unable to remove: " + runDir.getPath());
				throw e;
			}
		}

		File postfixChrootDir = new File("/var/spool/postfix/var/run/saslauthd");
		try {
			Process p = Runtime.getRuntime().exec("/bin/rm -rf " + postfixChrootDir.getPath());
			p.waitFor();

			postfixChrootDir.mkdirs();

			// no sasl group on redhat...
			p = Runtime.getRuntime().exec("chown root:sasl " + postfixChrootDir.getPath());
			p.waitFor();

			p = Runtime.getRuntime().exec("chmod u+r+w+x,g-r-w+x,o-r-w-x " + postfixChrootDir.getPath());
			p.waitFor();

			// should fix redhat cyrus login
			p = Runtime.getRuntime().exec("chmod a+x " + postfixChrootDir.getPath());
			p.waitFor();
		} catch (InterruptedException e) {
			logger.warn("Unable to configure: " + postfixChrootDir.getPath());
			throw e;
		}

		try {
			Process p = Runtime.getRuntime().exec("/bin/ln -s " + postfixChrootDir.getPath() + " " + runDir.getPath());
			p.waitFor();
		} catch (InterruptedException e) {
			logger.warn("Unable to create: " + runDir.getPath() + " link to directory: " + postfixChrootDir.getPath());
			throw e;
		}
	}

	public String getPtSocketPath() {
		return getString("bmpt-daemon.socket.path");
	}

	public String getSocketPath() {
		return getString("daemon.socket.path");
	}

	public int getExecutorsCount() {
		return Integer.parseInt(getString("daemon.threads"));
	}

	public String getString(String confKey) {
		return conf.getProperty(confKey);
	}
}
