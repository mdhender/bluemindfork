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
package net.bluemind.xivo.tests;

import junit.framework.TestCase;
import net.bluemind.xivo.client.XivoClient;
import net.bluemind.xivo.client.XivoFault;
import net.bluemind.xivo.common.Hosts;

public class XivoClientTests extends TestCase {

	public void testDnd() throws XivoFault {
		XivoClient xc = new XivoClient(Hosts.xivo());
		for (int i = 0; i < 10; i++) {
			xc.setDND("jack", "avencall.com", true);
			sleep();
			xc.setDND("jack", "avencall.com", false);
			sleep();
		}
	}

	public void testHandshake() throws XivoFault {
		XivoClient xc = new XivoClient(Hosts.xivo());
		xc.handshake("blue-mind.net");
	}

	public void testInitUserLinks() throws XivoFault {
		XivoClient xc = new XivoClient(Hosts.xivo());
		xc.setDND("bruce", "avencall.com", true);
		sleep();
		xc.setDND("bruce", "avencall.com", false);
		xc.setDND("jack", "avencall.com", true);
		sleep();
		xc.setDND("jack", "avencall.com", false);
		xc.setDND("irene", "avencall.com", true);
		sleep();
		xc.setDND("irene", "avencall.com", false);
	}

	public void testDial() throws XivoFault {
		XivoClient xc = new XivoClient(Hosts.xivo());
		xc.dial("jack", "avencall.com", "1102");
	}

	public void testForward() throws XivoFault {
		XivoClient xc = new XivoClient(Hosts.xivo());
		xc.forward("bruce", "avencall.com", "2000");
		xc.dial("jack", "avencall.com", "1000");
		sleep();
		sleep();
		sleep();
		sleep();
		xc.forward("bruce", "avencall.com", "");
	}

	private void sleep() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
	}
}
