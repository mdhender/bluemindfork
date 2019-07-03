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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.bluemind.system.helper.distrib.list.Debian;
import net.bluemind.system.helper.distrib.list.RedHat;
import net.bluemind.system.helper.distrib.list.Ubuntu;

public class OsVersionDetectionFactory {

	private static final Log logger = LogFactory.getLog(OsVersionDetectionFactory.class);
	
	private OsVersionDetectionFactory() {
		// use create method
	}

	public static IOsVersionDetection create() {
		File distributionFile = new File(new RedHat().getDistributionFile());
		if (distributionFile.exists()) {
			return new RedHatOSVersion();
		}

		distributionFile = new File(new Ubuntu().getDistributionFile());
		if (distributionFile.exists()) {
			return new UbuntuOSVersion();
		}

		distributionFile = new File(new Debian().getDistributionFile());
		if (distributionFile.exists()) {
			return new DebianOSVersion();
		}

		logger.info("Unable to determine distribution");
		return null;
	}

}
