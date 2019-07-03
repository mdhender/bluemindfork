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
package net.bluemind.addressbook.api;

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.tag.api.TagRef;

/**
 * Light {@link VCard}
 *
 */
@BMApi(version = "3")
public class VCardInfo {

	public VCard.Kind kind;
	public String mail;
	public String tel;
	public String formatedName;
	public List<TagRef> categories = Collections.emptyList();
	public int memberCount;
	public boolean photo;
	public String source;

	public static VCardInfo create(VCard card) {
		VCardInfo info = new VCardInfo();
		info.kind = card.kind;
		info.photo = card.identification.photo;
		info.formatedName = card.identification.formatedName.value;
		if (!card.communications.emails.isEmpty()) {
			info.mail = card.defaultMail();
		}

		if (!card.communications.tels.isEmpty()) {
			info.tel = card.communications.tels.get(0).value;
		}

		info.categories = card.explanatory.categories;

		info.memberCount = card.organizational.member != null ? card.organizational.member.size() : -1;

		info.source = card.source;
		return info;
	}

}