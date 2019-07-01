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
package net.bluemind.ysnp.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.unixsocket.UnixDomainSocketChannel;
import net.bluemind.utils.IniFile;

/**
 * format is : \0 len login \0 len pass \0 len srv \0 len realm
 * 
 * 
 */
public class SaslauthdProtocol {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private final static Registry registry = MetricsRegistry.get();
	private final static IdFactory idFactory = new IdFactory(MetricsRegistry.get(), SaslauthdProtocol.class);

	private ValidationPolicy vp;

	private static final byte[] SASL_OK = new byte[] { 0, 2, (byte) 'O', (byte) 'K' };

	private static final byte[] SASL_FAILED = new byte[] { 0, 2, (byte) 'N', (byte) 'O' };

	private String defaultDomain;

	public SaslauthdProtocol(ValidationPolicy vp) {
		this.vp = vp;

		IniFile ini = new IniFile("/etc/bm/bm.ini") {
			@Override
			public String getCategory() {
				return null;
			}
		};
		defaultDomain = ini.getData().get("default-domain");

	}

	public void execute(UnixDomainSocketChannel channel) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(new byte[2028]); // max is 255*4+8
		channel.read(buf);
		buf.flip();
		byte[] v = new byte[buf.getShort()];
		buf.get(v);
		String login = new String(v);

		v = new byte[buf.getShort()];
		buf.get(v);
		String password = new String(v);

		v = new byte[buf.getShort()];
		buf.get(v);
		String service = new String(v);

		v = new byte[buf.getShort()];
		buf.get(v);
		String realm = new String(v);

		if (!"admin0".equals(login) && realm.isEmpty() && defaultDomain != null) {
			realm = defaultDomain;
		}

		logger.info("Attempting login: " + login + " service: " + service + " realm: " + realm);

		boolean valid = false;

		try {
			valid = check(login, password, service, realm);
		} catch (Exception t) {
			logger.error(t.getMessage(), t);
		}

		ByteBuffer response;
		if (valid) {
			registry.counter(idFactory.name("authCount", "status", "ok", "service", service)).increment();
			response = ByteBuffer.wrap(SASL_OK);
		} else {
			registry.counter(idFactory.name("authCount", "status", "failed", "service", service)).increment();
			response = ByteBuffer.wrap(SASL_FAILED);
		}
		channel.write(response);
	}

	private boolean check(String login, String password, String service, String realm) {
		return vp.validate(login, password, service, realm);
	}

}
