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
package net.bluemind.addressbook.service.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.field.address.ParseException;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.fault.ValidationException;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;

public class VCardValidator implements IValidator<VCard> {

	public static final class Factory implements IValidatorFactory<VCard> {

		@Override
		public Class<VCard> support() {
			return VCard.class;
		}

		@Override
		public IValidator<VCard> create(BmContext context) {
			return new VCardValidator(context);
		}

	}

	private final BmContext context;

	public VCardValidator(BmContext context) {
		this.context = context;
	}

	/**
	 * local & domain part
	 */
	private static final Pattern EMAIL = Pattern
			.compile("^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@([a-z0-9-]+\\.)+[a-z]{2,}$");

	public void validate(VCard card, Optional<String> containerUid) throws ServerFault {
		if (card == null) {
			throw new ServerFault("card should not be empty");
		}

		if (card.kind == null) {
			throw new ServerFault("kind must be assigned", ErrorCode.INVALID_PARAMETER);
		}

		if (card.identification.formatedName == null || Strings.isNullOrEmpty(card.identification.formatedName.value)) {
			throw new ServerFault("identification.formatedName should not be empty", ErrorCode.INVALID_PARAMETER);
		}

		Iterator<Email> it = card.communications.emails.iterator();
		while (it.hasNext()) {
			Email v = it.next();
			String mail = v.value;
			if (!EMAIL.matcher(mail).matches()) {
				// try another email validator
				try {
					Mailbox address = AddressBuilder.DEFAULT.parseMailbox(mail);
					mail = address.getAddress();
				} catch (ParseException e1) {
					throw new ValidationException("Email invalid: '" + mail + "'", ErrorCode.INVALID_EMAIL);
				}
				if (!EMAIL.matcher(mail).matches()) {
					throw new ValidationException("Email invalid: '" + mail + "'", ErrorCode.INVALID_EMAIL);
				}
			}
		}

		containerUid.ifPresent(container -> validateMembers(card, container));

		// Sanitize collected contact: fix firstname/lastname"
		// FIXME : sanitze contact should be in plugin that "collect" contacts
	}

	private void validateMembers(VCard card, String containerUid) {
		for (Member member : card.organizational.member) {
			if (!containerUid.equals(member.containerUid)) {
				ItemValue<VCard> memberCard = null;
				try {
					memberCard = getMemberVCard(member.containerUid, member.itemUid);
				} catch (Exception e) {
					// ignore
				}
				if (memberCard != null && memberCard.value.kind == Kind.group) {
					throw new ValidationException(
							"Forbidden group member " + memberCard.displayName + " from foreign address book",
							ErrorCode.INVALID_GROUP_MEMBER);
				}
			}
		}
	}

	public ItemValue<VCard> getMemberVCard(String containerUid, String itemUid) {
		IAddressBook ab = context.provider().instance(IAddressBook.class, containerUid);
		ItemValue<VCard> memberCard = ab.getComplete(itemUid);
		return memberCard;
	}

	@Override
	public void create(VCard obj) throws ServerFault {
		throw new UnsupportedOperationException("VCardValidator is contextualized");
	}

	@Override
	public void update(VCard oldValue, VCard newValue) throws ServerFault {
		throw new UnsupportedOperationException("VCardValidator is contextualized");
	}

	@Override
	public void create(VCard obj, Map<String, String> params) throws ServerFault {
		validate(obj, Optional.ofNullable(params.get("containerUid")));
	}

	@Override
	public void update(VCard oldValue, VCard newValue, Map<String, String> params) throws ServerFault {
		validate(newValue, Optional.ofNullable(params.get("containerUid")));
	}

}
