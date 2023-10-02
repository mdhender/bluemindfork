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
package net.bluemind.cli.inject.imap;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.concurrent.Semaphore;

import org.columba.ristretto.message.Address;
import org.columba.ristretto.smtp.SMTPProtocol;
import org.columba.ristretto.smtp.SMTPResponse;

import net.bluemind.cli.inject.common.IMessageProducer;
import net.bluemind.cli.inject.common.MailExchangeInjector;
import net.bluemind.cli.inject.common.TargetMailbox;
import net.bluemind.cli.inject.common.TargetMailbox.Auth;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.network.topology.Topology;

public class SmtpInjector extends MailExchangeInjector {

	public static class SmtpTargetMailbox extends TargetMailbox {
		Semaphore lock;
		private SMTPProtocol prot;

		public SmtpTargetMailbox(TargetMailbox.Auth auth) {
			super(auth);
			this.prot = new SMTPProtocol(Topology.get().any("mail/smtp").value.address(), 587);
			this.lock = new Semaphore(1);
		}

		public boolean prepare() {
			try {
				return true;
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}

		public void exchange(TargetMailbox from, byte[] emlContent, long cycle) {
			try {
				lock.acquire();
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				return;
			}
			try {
				prot.openPort();
				prot.startTLS();
				prot.auth("PLAIN", auth.email(), auth.sid().toCharArray());
				prot.helo(InetAddress.getLocalHost());
				prot.mail(new Address(from.auth.email()));
				prot.rcpt(new Address(auth.email()));
				SMTPResponse sendResp = prot.data(new ByteArrayInputStream(emlContent));
				prot.quit();
				logger.debug("Added {} to {}", sendResp.getMessage(), auth.email());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				lock.release();
			}
		}
	}

	public SmtpInjector(IServiceProvider provider, String domainUid, IMessageProducer prod) {
		super(provider, domainUid, SmtpTargetMailbox::new, prod);

	}

}
