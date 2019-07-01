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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.bluemind.system.helper.distrib.list.Distribution;
import net.bluemind.system.helper.distrib.list.RedHat;
import net.bluemind.system.helper.distrib.list.RedHat7;
import net.bluemind.system.helper.distrib.list.RedHat8;

public class RedHatOSVersion implements IOsVersionDetection {

	private static final Log logger = LogFactory.getLog(RedHatOSVersion.class);

	public Distribution detect() {
		File distributionFile = new File(new RedHat().getDistributionFile());
		Distribution distrib = null;

		distrib = checkVersion(distributionFile, distrib);
		
		if (distrib == null) {
			logger.info("Unable to determine RedHat version.");
			distrib = new RedHat();
		}
		
		logger.info("Detected distribution is: " + distrib.getName());
		return distrib;
	}

	private Distribution checkVersion(File distributionFile, Distribution distrib) {
		try (BufferedReader br = new BufferedReader(new FileReader(distributionFile));) {
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.contains("release 7.")) {
					distrib = new RedHat7();
				} else if (line.contains("release 8.")) {
					distrib = new RedHat8();
				}
			}
		} catch (IOException e) {
			logger.info("Fail to read file: " + distributionFile.getName());
		}
		return distrib;
	}
}
