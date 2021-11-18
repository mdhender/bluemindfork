/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.cli.inject.common;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.field.LenientFieldParser;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.field.address.ParseException;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javafaker.Faker;
import com.github.javafaker.GameOfThrones;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import net.bluemind.mime4j.common.Mime4JHelper;

public class Mime4jMessageProducer implements IMessageProducer {

	private static final GameOfThrones gotFaker = Faker.instance().gameOfThrones();
	private static final Logger logger = LoggerFactory.getLogger(Mime4jMessageProducer.class);

	private final byte[] data800k;
	private final byte[] data5m;
	private final byte[] sapin;

	public Mime4jMessageProducer() {
		try {
			this.sapin = ByteStreams.toByteArray(data("sapin.png"));
			this.data800k = ByteStreams.toByteArray(new GZIPInputStream(data("800k-of-zeros.bin.gz")));
			this.data5m = ByteStreams.toByteArray(new GZIPInputStream(data("5meg-of-zeros.bin.gz")));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private InputStream data(String name) {
		return Mime4jMessageProducer.class.getClassLoader().getResourceAsStream("data/" + name);
	}

	@Override
	public byte[] createEml(TargetMailbox from, TargetMailbox to) {
		MessageImpl msg = new MessageImpl();

		try {
			msg.setFrom(AddressBuilder.DEFAULT.parseMailbox(from.email));
			msg.setTo(AddressBuilder.DEFAULT.parseMailbox(to.email));
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		}
		msg.setSubject("[" + gotFaker.city() + "] News from " + gotFaker.character() + " with " + gotFaker.dragon());

		MultipartImpl mixed = new MultipartImpl("mixed");

		MultipartImpl related = new MultipartImpl("related");
		BasicBodyFactory bbf = new BasicBodyFactory();
		String cid = "sapin-" + UUID.randomUUID().toString() + "@bluemind.net";
		TextBody body = bbf.textBody(
				"<html><body>" + "<div>Des infos de <em>" + gotFaker.character() + "</em></div>\r\n" + "<p>--</p>\r\n"
						+ "<div><img src=\"cid:" + cid + "\" alt=\"sapin\"/></div>" + "</body></html>",
				StandardCharsets.UTF_8);
		BodyPart bp = new BodyPart();
		bp.setBody(body, "text/html", ImmutableMap.of("charset", "utf-8"));
		bp.setContentTransferEncoding("quoted-printable");
		related.addBodyPart(bp);

		// add the inline sapin
		try {
			BinaryBody inlineImg = bbf.binaryBody(sapin);
			BodyPart inlineBp = new BodyPart();
			inlineBp.setBody(inlineImg, "image/png");
			inlineBp.setContentDisposition(ContentDispositionField.DISPOSITION_TYPE_INLINE);
			inlineBp.setContentTransferEncoding("base64");
			inlineBp.getHeader().addField(LenientFieldParser.parse("Content-ID: <" + cid + ">"));
			related.addBodyPart(inlineBp);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		BodyPart relBp = new BodyPart();
		relBp.setMultipart(related);
		mixed.addBodyPart(relBp);

		try {
			int selected = ThreadLocalRandom.current().nextInt(5);
			byte[] attach = null;
			switch (selected) {
			case 1:
				attach = data5m;
				break;
			case 2:
				attach = data800k;
				break;
			default:
				break;
			}
			if (attach != null) {
				BinaryBody attachBody = bbf.binaryBody(attach);
				BodyPart bin = new BodyPart();
				bin.setBody(attachBody, "application/octet-stream");
				bin.setContentDisposition(ContentDispositionField.DISPOSITION_TYPE_ATTACHMENT,
						"attach-" + UUID.randomUUID().toString() + ".bin");
				bin.setContentTransferEncoding("base64");
				mixed.addBodyPart(bin);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		msg.setMultipart(mixed);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Mime4JHelper.serialize(msg, out);
		return out.toByteArray();
	}

}
