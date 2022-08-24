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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.driver.mailapi;

import java.text.Normalizer;
import java.util.Optional;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.MessageBody.Part;

public class BodyStructureRenderer {

	public String from(Part root) {
		StringBuilder sb = new StringBuilder();
		from0(sb, root);
		return sb.toString();
	}

	private void from0(StringBuilder sb, Part root) {
		String[] mimeSplit = root.mime.split("/");
		if (mimeSplit[0].equalsIgnoreCase("multipart")) {
			sb.append("(");
			for (Part c : root.children) {
				from0(sb, c);
			}
			sb.append(" \"").append(mimeSplit[1].toUpperCase());
			sb.append("\" (BOUNDARY \"-=Part").append(root.address).append("=-\") NIL NIL NIL");
			sb.append(")");
		} else {
			sb.append("(\"").append(mimeSplit[0].toUpperCase()).append("\" \"").append(mimeSplit[1].toUpperCase())
					.append("\"");
			// cs
			if (mimeSplit[0].equalsIgnoreCase("text")) {
				sb.append(" (\"CHARSET\" \"" + Optional.ofNullable(root.charset).orElse("us-ascii") + "\")");
			} else {
				sb.append(" NIL");
			}
			// contentId
			if (root.contentId != null) {
				sb.append(" \"").append(root.contentId).append("\"");
			} else {
				sb.append(" NIL");
			}

			// nil
			sb.append(" NIL");

			// encoding
			sb.append(" \"").append("8BIT").append("\"");

			// size
			sb.append(" ").append(root.size);

			if (root.dispositionType == DispositionType.ATTACHMENT && !Strings.isNullOrEmpty(root.fileName)) {
				sb.append(" NIL (\"ATTACHMENT\" (\"FILENAME\" \"" + cleanFileName(root) + "\")) NIL NIL");
			} else if (root.dispositionType == DispositionType.INLINE && root.contentId != null) {
				sb.append(" NIL (\"INLINE\" NIL) NIL NIL");
			} else {
				sb.append(" NIL NIL NIL NIL NIL");
			}

			sb.append(")");
		}
	}

	private String cleanFileName(Part p) {
		String s = Normalizer.normalize(p.fileName, Normalizer.Form.NFD);
		s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
		return CharMatcher.ascii().retainFrom(s);
	}

}
