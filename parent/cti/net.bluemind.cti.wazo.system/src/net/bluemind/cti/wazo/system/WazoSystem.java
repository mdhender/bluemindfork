/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cti.wazo.system;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.cti.wazo.service.WazoBackend;
import net.bluemind.system.api.ConnectionTestStatus;
import net.bluemind.system.service.RegisteredExternalSystem;
import net.bluemind.user.api.UserAccount;

public class WazoSystem extends RegisteredExternalSystem {

	public static final Logger logger = LoggerFactory.getLogger(WazoSystem.class);

	public WazoSystem() {
		super("Wazo", "Wazo, An Open Source project to build your own IP telecom platform",
				AuthKind.SIMPLE_CREDENTIALS);
	}

	@Override
	public byte[] getLogo() {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("/logo/wazo-logo.png")) {
			return ByteStreams.toByteArray(in);
		} catch (Exception e) {
			logger.warn("Cannot load wazo logo", e);
			return new byte[0];
		}
	}

	@Override
	public boolean handles(String userAccountIdentifier) {
		return userAccountIdentifier.startsWith(super.identifier);
	}

	@Override
	public ConnectionTestStatus testConnection(String domain, UserAccount account) {
		boolean testConnection = new WazoBackend().testConnection(domain, account);
		return testConnection ? ConnectionTestStatus.OK : ConnectionTestStatus.NOK;
	}

}
