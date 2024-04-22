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
package net.bluemind.eas.http.tests.builders;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.w3c.dom.Document;

import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.email.EmailResponse;
import net.bluemind.eas.dto.email.EmailResponse.Flag.Status;
import net.bluemind.eas.dto.email.Importance;
import net.bluemind.eas.serdes.email.EmailResponseFormatter;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.eas.wbxml.WbxmlOutput;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class EmailBuilder {

	public static Document getSimpleMail(ProtocolVersion version) throws Exception {

		EmailResponse email = new EmailResponse();
		email.subject = "subject";
		email.threadTopic = "subject";
		email.importance = Importance.NORMAL;
		email.contentClass = "urn:content-classes:message";
		email.internetCPID = "65001";
		email.from = "sender@bm.loc";
		email.to = "to@bm.loc";
		email.dateReceived = new Date();
		email.read = false;
		email.flag = new EmailResponse.Flag();
		email.flag.status = Status.CLEARED;
		email.isDraft = false;

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		WbxmlOutput output = WbxmlOutput.of(bos);
		double valueOfVersion = Double.parseDouble(version.toString());
		WbxmlResponseBuilder builder = new WbxmlResponseBuilder(valueOfVersion, null, output);
		EmailResponseFormatter cf = new EmailResponseFormatter();
		builder.start(NamespaceMapping.SYNC);
		cf.append(builder, valueOfVersion, email, (a) -> {
		});
		builder.end((a) -> {
		});
		return WBXMLTools.toXml(bos.toByteArray());

	}

}
