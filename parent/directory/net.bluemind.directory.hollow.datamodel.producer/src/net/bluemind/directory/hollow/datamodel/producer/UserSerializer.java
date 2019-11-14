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
package net.bluemind.directory.hollow.datamodel.producer;

import java.util.Optional;

import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.hollow.datamodel.producer.Value.ByteArrayValue;
import net.bluemind.directory.hollow.datamodel.producer.Value.StringValue;
import net.bluemind.directory.hollow.datamodel.utils.Pem;
import net.bluemind.user.api.User;

public class UserSerializer extends DirEntrySerializer {

	private final ItemValue<User> user;

	public UserSerializer(ItemValue<User> user, ItemValue<DirEntry> dirEntry, String domainUid) {
		super(dirEntry, domainUid);
		this.user = user;
	}

	@Override
	public Value get(Property property) {
		switch (property) {
		case DisplayName:
			return new StringValue(user.displayName);
		case Surname:
			return new StringValue(
					Optional.ofNullable(user.value.contactInfos.identification.name.givenNames).orElse(null));
		case SmtpAddress:
			return getDefaultSmtp();
		case GivenName:
			return new StringValue(
					Optional.ofNullable(user.value.contactInfos.identification.name.givenNames).orElse(null));
		case Title:
			return new StringValue(Optional.ofNullable(user.value.contactInfos.organizational.title).orElse(null));
		case DepartmentName:
			return new StringValue(
					Optional.ofNullable(user.value.contactInfos.organizational.org.department).orElse(null));
		case CompanyName:
			return new StringValue(
					Optional.ofNullable(user.value.contactInfos.organizational.org.company).orElse(null));
		case Assistant:
			return new StringValue(Optional.ofNullable(user.value.contactInfos.related.assistant).orElse(null));
		case StreetAddress:
			if (!user.value.contactInfos.deliveryAddressing.isEmpty()) {
				return new StringValue(
						Optional.ofNullable(user.value.contactInfos.deliveryAddressing.get(0).address.streetAddress)
								.orElse(null));
			} else {
				return Value.NULL;
			}
		case postOfficeBox:
			if (!user.value.contactInfos.deliveryAddressing.isEmpty()) {
				return new StringValue(
						Optional.ofNullable(user.value.contactInfos.deliveryAddressing.get(0).address.postOfficeBox)
								.orElse(null));
			} else {
				return Value.NULL;
			}
		case Locality:
			if (!user.value.contactInfos.deliveryAddressing.isEmpty()) {
				return new StringValue(Optional
						.ofNullable(user.value.contactInfos.deliveryAddressing.get(0).address.locality).orElse(null));
			} else {
				return Value.NULL;
			}
		case StateOrProvince:
			if (!user.value.contactInfos.deliveryAddressing.isEmpty()) {
				return new StringValue(Optional
						.ofNullable(user.value.contactInfos.deliveryAddressing.get(0).address.region).orElse(null));
			} else {
				return Value.NULL;
			}
		case PostalCode:
			if (!user.value.contactInfos.deliveryAddressing.isEmpty()) {
				return new StringValue(Optional
						.ofNullable(user.value.contactInfos.deliveryAddressing.get(0).address.postalCode).orElse(null));
			} else {
				return Value.NULL;
			}
		case Country:
			if (!user.value.contactInfos.deliveryAddressing.isEmpty()) {
				return new StringValue(
						Optional.ofNullable(user.value.contactInfos.deliveryAddressing.get(0).address.countryName)
								.orElse(null));
			} else {
				return Value.NULL;
			}
		case Account:
		case AddressBookDisplayNamePrintableAscii:
			return new StringValue(user.value.login);
		case BusinessTelephoneNumber:
			return new StringValue(getPhoneNumber("voice", "work"));
		case HomeTelephoneNumber:
			return new StringValue(getPhoneNumber("voice", "home"));
		case MobileTelephoneNumber:
			return new StringValue(getPhoneNumber("voice", "cell"));
		case PagerTelephoneNumber:
			return Value.NULL;
		case PrimaryFaxNumber:
			return new StringValue(getPhoneNumber("fax", "work"));
		case AssistantTelephoneNumber:
			return Value.NULL;
		case UserX509Certificate:
			Optional<byte[]> pkcs7 = new Pem(user.value.contactInfos.security.key.value).toPcks7();
			return new ByteArrayValue(pkcs7.orElse(null));
		case AddressBookX509Certificate:
			Optional<byte[]> der = new Pem(user.value.contactInfos.security.key.value).toDer();
			return new ByteArrayValue(der.orElse(null));
		default:
			return super.get(property);
		}
	}

	private Value getDefaultSmtp() {
		if (dirEntry.value.email != null) {
			return new StringValue(dirEntry.value.email);
		} else {
			return new StringValue(user.value.login + "@" + domainUid);
		}
	}

	private String getPhoneNumber(String type, String classifier) {
		String bestChoice = null;
		for (Tel tel : user.value.contactInfos.communications.tels) {
			if (tel.containsValues("TYPE", classifier, type)) {
				return tel.value;
			} else if (tel.containsValues("TYPE", type)) {
				bestChoice = tel.value;
			}
		}
		return null != bestChoice ? bestChoice : null;
	}

}
