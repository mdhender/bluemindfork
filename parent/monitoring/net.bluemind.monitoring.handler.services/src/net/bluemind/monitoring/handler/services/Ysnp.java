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

import java.util.Arrays;

import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.server.api.Server;

public class Ysnp extends AbstractJavaService {

	public Ysnp() {
		super(BmService.YSNP.toString(), Arrays.asList("mail/smtp", "mail/smtp-edge"));
	}

	@Override
	public ServerInformation getSpecificServerInfo(Server server, String method) {
		return null;
	}

}
