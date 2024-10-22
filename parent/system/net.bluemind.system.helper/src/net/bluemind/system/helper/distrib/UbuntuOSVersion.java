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

import net.bluemind.system.helper.distrib.list.Distribution;
import net.bluemind.system.helper.distrib.list.Ubuntu;
import net.bluemind.system.helper.distrib.list.UbuntuFocal;
import net.bluemind.system.helper.distrib.list.UbuntuJammy;

public class UbuntuOSVersion implements IOsVersionDetection {

	private static final Logger logger = LoggerFactory.getLogger(UbuntuOSVersion.class);

	public Distribution detect() {
		File distributionFile = new File(new Ubuntu().getDistributionFile());
		Distribution distrib = null;

		distrib = checkVersion(distributionFile, distrib);

		if (distrib == null) {
			logger.info("Unable to determine Ubuntu version.");
			distrib = new Ubuntu();
		}

		logger.info("Detected distribution is: " + distrib.getName());
		return distrib;
	}

	private Distribution checkVersion(File distributionFile, Distribution distrib) {
		try (BufferedReader br = new BufferedReader(new FileReader(distributionFile))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("DISTRIB_RELEASE")) {
					if (line.endsWith("20.04")) {
						distrib = new UbuntuFocal();
					} else if (line.endsWith("22.04")) {
						distrib = new UbuntuJammy();
					}
				}
			}
		} catch (IOException e) {
			logger.info("Fail to read file: " + distributionFile.getName());
		}
		return distrib;
	}
}
