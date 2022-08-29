/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.addressbook.service.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;

public class VCardGroupSanitizer {

	private final BmContext context;

	public VCardGroupSanitizer(BmContext context) {
		this.context = context;
	}

	public boolean sanitize(VCard group) {
		if (group.kind != VCard.Kind.group) {
			return false;
		}

		if (group.organizational.member == null || group.organizational.member.isEmpty()) {
			return false;
		}

		return group.organizational.member.stream().map(member -> member.containerUid).filter(c -> c != null).distinct()
				.map(uid -> cleanupMembersOf(group, uid)).filter(m -> m).count() > 0;
	}

	private boolean cleanupMembersOf(VCard group, String uid) {
		IAddressBook ab = null;
		try {
			ab = context.provider().instance(IAddressBook.class, uid);
		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.NOT_FOUND) {
				group.organizational.member.stream().filter(member -> uid.equals(member.containerUid))
						.forEach(member -> {
							member.containerUid = null;
							member.itemUid = null;
						});
				return true;
			}

			if (e.getCode() != ErrorCode.PERMISSION_DENIED) {
				throw e;
			} else {
				return false;
			}
		}
		try {
			return sanitizeFor(ab, group, uid);
		} catch (ServerFault e) {
			if (e.getCode() != ErrorCode.PERMISSION_DENIED) {
				throw e;
			}
			return false;
		}
	}

	public boolean sanitizeFor(IAddressBook ab, VCard group, String abUid) {
		Map<String, Member> localCards = group.organizational.member.stream().filter(
				member -> (abUid.equals(member.containerUid) || member.containerUid == null) && member.itemUid != null)
				.collect(Collectors.toMap(m -> m.itemUid, m -> m, (member1, member2) -> member1));

		List<ItemValue<VCard>> cards = ab.multipleGet(ImmutableList.copyOf(localCards.keySet()));

		boolean modifiedFlag = cards.stream() //
				.filter(card -> !isVCardMemberValid(localCards.get(card.uid), card.value)) //
				.map(card -> {
					boolean modified = false;
					Member member = localCards.get(card.uid);
					modified |= !card.displayName.equals(member.commonName);
					member.commonName = card.displayName;

					modified |= !StringUtils.equals(card.value.defaultMail(), member.mailto);
					member.mailto = card.value.defaultMail();
					return modified;
				}).filter(m -> m).count() > 0;

		Set<String> foundMemberCardUid = cards.stream().map(card -> card.uid).collect(Collectors.toSet());
		List<Member> notFoundMembers = localCards.entrySet().stream() //
				.filter(entry -> !foundMemberCardUid.contains(entry.getKey())) //
				.map(entry -> entry.getValue()) //
				.collect(Collectors.toList());
		modifiedFlag |= !notFoundMembers.isEmpty();
		notFoundMembers.forEach(notFoundMember -> notFoundMember.containerUid = null);

		return modifiedFlag;
	}

	private static boolean isVCardMemberValid(Member member, VCard card) {
		return member.commonName != null && isVCardMemberEmailValid(member, card);
	}

	public static boolean isVCardMemberEmailValid(Member member, VCard card) {
		return card.communications.emails.stream() //
				.anyMatch(email -> email.value != null && email.value.equals(member.mailto));
	}
}
