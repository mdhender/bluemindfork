/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.system.helper.distrib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.system.helper.distrib.list.Debian;
import net.bluemind.system.helper.distrib.list.DebianBuster;
import net.bluemind.system.helper.distrib.list.DebianJessie;
import net.bluemind.system.helper.distrib.list.DebianStretch;
import net.bluemind.system.helper.distrib.list.Distribution;

public class DebianOSVersion implements IOsVersionDetection {

	private static final Logger logger = LoggerFactory.getLogger(DebianOSVersion.class);

	// TODO wrong detection in debian_version file (example : stretch/sid)
	public Distribution detect() {
		File distributionFile = new File(new Debian().getDistributionFile());
		Distribution distrib = null;

		distrib = checkVersion(distributionFile, distrib);

		if (distrib == null) {
			logger.info("Unable to determine Debian version.");
			distrib = new Debian();
		}

		logger.info("Detected distribution is: " + distrib.getName());
		return distrib;
	}

	private Distribution checkVersion(File distributionFile, Distribution distrib) {
		try (BufferedReader br = new BufferedReader(new FileReader(distributionFile));) {

			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("8.")) {
					distrib = new DebianJessie();
				} else if (line.startsWith("9.")) {
					distrib = new DebianStretch();
				} else if (line.startsWith("10.")) {
					distrib = new DebianBuster();
				}
			}
		} catch (IOException e) {
			logger.info("Fail to read file: " + distributionFile.getName());
		}
		return distrib;
	}
}
