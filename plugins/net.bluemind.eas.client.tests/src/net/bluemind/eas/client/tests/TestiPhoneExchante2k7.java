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
package net.bluemind.eas.client.tests;

import java.io.InputStream;

import org.w3c.dom.Document;

import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.utils.FileUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class TestiPhoneExchante2k7 extends AbstractPushTest {

	private void decode(String fileName) throws Exception {
		InputStream in = loadDataFile(fileName);
		byte[] data = FileUtils.streamBytes(in, true);
		Document doc = WBXMLTools.toXml(data);
		DOMUtils.logDom(doc);
	}

	public void testProvisionRequest1() throws Exception {
		System.err.println("iphone request 1");
		decode("iphoneProvReq1.wbxml");
	}

	public void testProvisionResponse1() throws Exception {
		System.err.println("exchange response 1");
		decode("ex2k7provResp1.wbxml");
	}

	public void testProvisionRequest2() throws Exception {
		System.err.println("iphone request 2");
		decode("iphoneProvReq2.wbxml");
	}

	public void testProvisionResponse2() throws Exception {
		System.err.println("exchange response 2");
		decode("ex2k7provResp2.wbxml");
	}

	public void testDiffOpushExchange() throws Exception {
		System.err.println("eas response");
		decode("tom_prov_resp.wbxml");
		System.err.println("exchange response");
		decode("ex_prov_resp.wbxml");

	}
}
