/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.imap.driver.mailapi;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.field.Fields;

import com.google.common.base.Strings;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.WithId;

/*
 * Enveloppe structure: The fields of the envelope structure are in the
 * following order: date, subject, from, sender, reply-to, to, cc, bcc,
 * in-reply-to, and message-id. The date, subject, in-reply-to, and message-id
 * fields are strings. The from, sender, reply-to, to, cc, and bcc fields are
 * parenthesized lists of address structures.
 */
public class EnvelopeRenderer {
	private EnvelopeRenderer() {
	}

	public static ByteBuf render(Supplier<MessageBody> body, WithId<MailboxRecord> rec) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		// LC: TODO MimeUtils.fold() ?

		// Date
		if (rec.value.internalDate != null) {
			DateTimeField dateField = Fields.date("Date", rec.value.internalDate);
			sb.append(quoted(dateField.getBody()));
		} else {
			sb.append("NIL");
		}
		sb.append(" ");

		// Subject
		var subject = Fields.subject(body.get().subject).getBody();
		if (!Strings.isNullOrEmpty(subject)) {
			sb.append("{" + subject.length() + "}\r\n").append(subject);
		} else {
			sb.append("NIL");
		}
		sb.append(" ");

		Consumer<List<Recipient>> toMail = rcpts -> {
			sb.append("(");
			int idx = 0;
			for (Recipient rcpt : rcpts) {
				if (idx++ > 0) {
					sb.append(" ");
				}
				var loginatdom = rcpt.address.split("@");
				sb.append("(");
				sb.append(rcpt.dn != null ? (quoted(rcpt.dn)) : "NIL");
				sb.append(" ");

				sb.append("NIL "); // Don't understand what it should be
				sb.append(quoted(loginatdom[0]));
				sb.append(" ");

				sb.append(quoted(loginatdom[1]));
				sb.append(")");
			}
			sb.append(")");
		};
		Consumer<Mailbox> mailboxToMail = mbox -> {
			sb.append("((");
			sb.append(mbox.getName() != null ? (quoted(mbox.getName())) : "NIL");
			sb.append(" ");

			sb.append("NIL "); // Don't understand what it should be
			sb.append(quoted(mbox.getLocalPart()));
			sb.append(" ");

			sb.append(quoted(mbox.getDomain()));
			sb.append("))");
		};

		// From
		body.get().recipients.stream().filter(r -> r.kind == RecipientKind.Originator).findFirst()
				.ifPresentOrElse(r -> {
					mailboxToMail.accept(fromRecipient(r));
					sb.append(" ");
				}, () -> sb.append("NIL"));
		sb.append(" ");

		// Sender
		body.get().recipients.stream().filter(r -> r.kind == RecipientKind.Sender).findFirst()
				.ifPresentOrElse(r -> mailboxToMail.accept(fromRecipient(r)), () -> sb.append("NIL"));
		sb.append(" ");

		// Reply-To
		sb.append("NIL ");

		// To
		var to = body.get().recipients.stream().filter(r -> r.kind == RecipientKind.Primary).toList();
		if (to.isEmpty()) {
			sb.append("NIL");
		} else {
			toMail.accept(to);
		}
		sb.append(" ");

		// Cc
		var cc = body.get().recipients.stream().filter(r -> r.kind == RecipientKind.CarbonCopy).toList();
		if (cc.isEmpty()) {
			sb.append("NIL");
		} else {
			toMail.accept(cc);
		}
		sb.append(" ");

		// Bcc
		var bcc = body.get().recipients.stream().filter(r -> r.kind == RecipientKind.BlindCarbonCopy).toList();
		if (bcc.isEmpty()) {
			sb.append("NIL");
		} else {
			toMail.accept(bcc);
		}
		sb.append(" ");

		// In-Reply-To
		sb.append("NIL ");

		// Message-Id
		sb.append("\"").append(body.get().messageId).append("\"");
		sb.append(")");
		return Unpooled.wrappedBuffer(sb.toString().getBytes());
	}

	private static String quoted(String s) {
		return '"' + s + '"';
	}

	private static Mailbox fromRecipient(Recipient r) {
		int idx = r.address.indexOf('@');
		if (idx > 0) {
			return new Mailbox(r.dn, r.address.substring(0, idx), r.address.substring(idx + 1));
		} else {
			return new Mailbox(r.dn, r.address, "invalid.email.domain");
		}
	}
}
