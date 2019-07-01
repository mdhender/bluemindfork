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
package net.bluemind.imap.command;

import java.util.List;

import net.bluemind.imap.Annotation;
import net.bluemind.imap.AnnotationList;
import net.bluemind.imap.impl.IMAPResponse;

public class GetAnnotationCommand extends SimpleCommand<AnnotationList> {
	private String mailbox;

	public GetAnnotationCommand(String mailbox) {
		this(mailbox, "*");
	}

	public GetAnnotationCommand(String mailbox, String annotation) {
		super("GETANNOTATION " + toUtf7(mailbox) + " \"" + annotation + "\" (\"value.priv\" \"value.shared\")");
		this.mailbox = mailbox;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		IMAPResponse last = rs.get(rs.size() - 1);
		if (last.isBad()) {
			for (IMAPResponse r : rs) {
				logger.error("ANNOT: " + r.getPayload());
			}
			data = new AnnotationList(0);
			return;
		}

		data = new AnnotationList(rs.size() - 1);

		rs.forEach(r -> {
			String a = r.getPayload();

			int nameIndex = a.indexOf(mailbox);
			if (nameIndex == -1) {
				return;
			}

			nameIndex += mailbox.length() + 1;
			if (nameIndex > a.length()) {
				return;
			}

			a = a.substring(nameIndex);

			nameIndex = 0;
			int annotationValueIndex = a.indexOf(" (");
			if (annotationValueIndex == -1) {
				return;
			}

			String annotationName = a.substring(nameIndex, annotationValueIndex);
			annotationName = annotationName.substring(annotationName.startsWith("\"") ? 1 : 0,
					annotationName.endsWith("\"") ? annotationName.length() - 1 : annotationName.length());

			String annotationValues = a.substring(annotationValueIndex + 2, a.length() - 1);

			Annotation annotationSharedValue = null;
			Annotation annotationPrivValue = null;

			int kindBegin = 0;
			int kindEnd = 0;
			while ((kindBegin = annotationValues.indexOf("\"value")) != -1
					&& (kindEnd = annotationValues.indexOf("\"", kindBegin + 1)) != -1) {
				String kind = annotationValues.substring(kindBegin + 1, kindEnd);

				int kindValueBegin = kindEnd + 2;
				int kindValueEnd = kindValueBegin;
				int kindValueEndOffset = 0;

				if (kindValueBegin > annotationValues.length()) {
					break;
				}

				if (annotationValues.charAt(kindValueBegin) == '"') {
					// next value search must begin after ending " character
					kindValueEndOffset = 1;

					kindValueBegin += 1;
					kindValueEnd = annotationValues.indexOf("\"", kindValueBegin);
				} else if (annotationValues.charAt(kindValueBegin) == '{') {
					int kindValueLength = 0;
					try {
						kindValueLength = Integer.parseInt(annotationValues.substring(kindValueBegin + 1,
								annotationValues.indexOf('}', kindValueBegin)));
					} catch (NumberFormatException nfe) {
						break;
					}

					kindValueBegin = annotationValues.indexOf('}', kindValueBegin) + 1;
					kindValueEnd = kindValueBegin + kindValueLength;
				} else {
					int next = annotationValues.indexOf(" ", kindValueBegin);
					if (next != -1) {
						annotationValues = annotationValues.substring(next);
					} else {
						break;
					}

					continue;
				}

				if (kindValueEnd == -1 || kindValueEnd > annotationValues.length()) {
					break;
				}

				String kindValue = annotationValues.substring(kindValueBegin, kindValueEnd);
				if (kindValue.equals("NIL")) {
					kindValue = null;
				}

				if (kind.equals("value.shared")) {
					annotationSharedValue = Annotation.fromSharedValue(kindValue);
				} else if (kind.equals("value.priv")) {
					annotationPrivValue = Annotation.fromPrivValue(kindValue);
				}

				annotationValues = annotationValues.substring(kindValueEnd + kindValueEndOffset);
			}

			data.put(annotationName,
					Annotation.merge(data.get(annotationName), annotationSharedValue, annotationPrivValue));
		});
	}
}
