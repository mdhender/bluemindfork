/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.docker.imaptest;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

public class DovecotImaptestRunner extends GenericContainer<DovecotImaptestRunner> {

	private static final Logger logger = LoggerFactory.getLogger(DovecotImaptestRunner.class);

	private static class ImapTestOutputConsumer extends BaseConsumer<ImapTestOutputConsumer> {

		ConcurrentLinkedDeque<String> logFrames = new ConcurrentLinkedDeque<>();

		@Override
		public void accept(OutputFrame t) {
			String s = t.getUtf8String();
			logFrames.add(s);
		}

	}

	private final ImapTestOutputConsumer consumer;

	DovecotImaptestRunner(String cmd) {
		super("docker.bluemind.net/bluemind/imaptest:5.0");

		withCommand(cmd);
		this.consumer = new ImapTestOutputConsumer();
		withLogConsumer(consumer);
		setStartupCheckStrategy(new OneShotStartupCheckStrategy());
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && o instanceof DovecotImaptestRunner;
	}

	@Override
	public void start() {
		logger.info("Starting imaptest...");
		super.start();
	}

	public List<String> runPlan() {
		try {
			start();
		} catch (ContainerLaunchException cle) {
			logger.warn("launch error, imap test panic ? ({})", cle.getMessage());// NOSONAR
		}
		return List.copyOf(consumer.logFrames);
	}

}
