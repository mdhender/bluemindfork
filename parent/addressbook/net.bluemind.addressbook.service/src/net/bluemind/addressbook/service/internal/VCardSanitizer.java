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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.BasicAttribute;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Identification;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.addressbook.api.VCard.Security.Key;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.tag.service.TagsSanitizer;

public class VCardSanitizer implements ISanitizer<VCard> {

	public static final class Factory implements ISanitizerFactory<VCard> {

		@Override
		public Class<VCard> support() {
			return VCard.class;
		}

		@Override
		public ISanitizer<VCard> create(BmContext context, Container container) {
			return new VCardSanitizer(context);
		}

	}

	private VCardGroupSanitizer vcardGroupSanitizer;
	private TagsSanitizer tagsSanitizer;

	public VCardSanitizer(BmContext context) {
		this.vcardGroupSanitizer = new VCardGroupSanitizer(context);
		this.tagsSanitizer = new TagsSanitizer(context);
	}

	public void sanitize(VCard card, Optional<String> containerUid) throws ServerFault {
		sanitizeDefault(card);
		sanitizeFormattedName(card);
		sanitizeCnAndMailto(card);
		sanitizeParameterValues(card);
		sanitizeEmails(card);
		sanitizePemCertificates(card);
		sanitizeMembers(card, containerUid);
		tagsSanitizer.sanitize(card.explanatory.categories);
	}

	private void sanitizePemCertificates(VCard card) {
		if (card.security.key != null && card.security.key.value != null && card.security.key.value.isBlank()) {
			card.security.key = new Key();
		}
	}

	private void sanitizeMembers(VCard card, Optional<String> containerUid) {
		card.organizational.member = card.organizational.member.stream()
				.filter(member -> StringUtils.isNotBlank(member.mailto) || isDList(member, containerUid))
				.collect(Collectors.toList());
		containerUid.ifPresent(container -> {
			card.organizational.member.forEach(member -> {
				if (member.itemUid != null && member.containerUid == null) {
					member.containerUid = container;
				}
			});
		});
	}

	protected boolean isDList(Member member, Optional<String> containerUid) {
		String container = member.containerUid == null ? containerUid.orElse(null) : member.containerUid;
		if (container != null) {
			ItemValue<VCard> resolved = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IAddressBook.class, container).getComplete(member.itemUid);
			return resolved != null && resolved.value.kind == Kind.group;
		}
		return false;
	}

	private void sanitizeEmails(VCard card) {
		card.communications.emails = card.communications.emails.stream().filter(e -> e.value != null).map(e -> {
			e.value = e.value.toLowerCase().trim();
			return e;
		}).collect(Collectors.toList());
	}

	private void sanitizeDefault(VCard card) {
		if (card.identification == null) {
			card.identification = new VCard.Identification();
		} else {
			sanitizeDefaulIdentification(card.identification);
		}

		if (card.deliveryAddressing == null) {
			card.deliveryAddressing = Collections.emptyList();
		}

		if (card.communications == null) {
			card.communications = new VCard.Communications();
		}

		if (card.organizational == null) {
			card.organizational = new VCard.Organizational();
		}

		if (card.explanatory == null) {
			card.explanatory = new VCard.Explanatory();
		}

		if (card.related == null) {
			card.related = new VCard.Related();
		}

		if (card.security == null) {
			card.security = new VCard.Security();
		}
	}

	private void sanitizeDefaulIdentification(Identification identification) {
		if (identification.formatedName == null) {
			identification.formatedName = new Identification.FormatedName();
		} else {
			sanitizeBasicAttribute(identification.formatedName);
		}

		if (identification.name == null) {
			identification.name = new Identification.Name();
		} else {
			sanitizeBasicAttribute(identification.name);
		}

		if (identification.nickname == null) {
			identification.nickname = new Identification.Nickname();
		} else {
			sanitizeBasicAttribute(identification.nickname);
		}

		if (identification.gender == null) {
			identification.gender = new Identification.Gender();
		} else {
			sanitizeBasicAttribute(identification.gender);
		}

	}

	private void sanitizeParameterValues(VCard card) {
		card.deliveryAddressing = card.deliveryAddressing.stream().map(e -> {
			e.address = sanitizeBasicAttribute(e.address);
			return e;
		}).collect(Collectors.toList());
		card.explanatory.urls = card.explanatory.urls.stream().map(this::sanitizeBasicAttribute)
				.collect(Collectors.toList());
		card.communications.emails = card.communications.emails.stream().map(this::sanitizeBasicAttribute)
				.collect(Collectors.toList());
		card.communications.tels = card.communications.tels.stream().map(this::sanitizeBasicAttribute)
				.collect(Collectors.toList());
		card.communications.impps = card.communications.impps.stream().map(this::sanitizeBasicAttribute)
				.collect(Collectors.toList());
		card.communications.langs = card.communications.langs.stream().map(this::sanitizeBasicAttribute)
				.collect(Collectors.toList());
	}

	private <T extends BasicAttribute> T sanitizeBasicAttribute(T attr) {
		if (attr.parameters == null) {
			attr.parameters = Collections.emptyList();
		} else {
			attr.parameters = attr.parameters.stream() //
					.filter(this::checkNotEmpty) //
					.map(this::removeInvalidCharacters) //
					.collect(Collectors.toList());
		}
		return attr;

	}

	private boolean checkNotEmpty(Parameter p) {
		return StringUtils.isNotBlank(p.label) && StringUtils.isNotBlank(p.value);
	}

	private Parameter removeInvalidCharacters(Parameter p) {
		p.label = p.label.replace(";", "");
		p.value = p.value.replace(";", "");
		return p;
	}

	private void sanitizeCnAndMailto(VCard card) {
		vcardGroupSanitizer.sanitize(card);
	}

	public static void sanitizeFormattedName(VCard card, String login) {
		if (isNullFormattedName(card.identification.formatedName)) {
			if (card.identification.name == null && login != null) {
				card.identification.formatedName = FormatedName.create(login);
			} else {
				String formattedName = createFormattedNameFromIdentification(card);
				if (formattedName != null) {
					card.identification.formatedName = FormatedName.create(formattedName);
				}
			}
		}
	}

	private static boolean isNullFormattedName(FormatedName formatedName) {
		return formatedName == null || formatedName.value == null || formatedName.value.isBlank();
	}

	private void sanitizeFormattedName(VCard card) {
		String formattedName = createFormattedNameFromIdentification(card);
		if (formattedName != null && isNullFormattedName(formatedName.value)) {
			card.identification.formatedName = FormatedName.create(formattedName);
		}
	}

	private static String createFormattedNameFromIdentification(VCard card) {
		List<String> names = new ArrayList<>(3);
		String prefixes = card.identification.name.prefixes;
		String givenName = card.identification.name.givenNames;
		String additionalName = card.identification.name.additionalNames;
		String familyNames = card.identification.name.familyNames;
		String suffix = card.identification.name.suffixes;

		if (!Strings.isNullOrEmpty(givenName)) {
			card.identification.name.givenNames = givenName.trim();
			names.add(givenName.trim());
		}
		if (!Strings.isNullOrEmpty(additionalName)) {
			card.identification.name.additionalNames = additionalName.trim();
			names.add(additionalName.trim());
		}
		if (!Strings.isNullOrEmpty(familyNames)) {
			card.identification.name.familyNames = familyNames.trim();
			names.add(familyNames.trim());
		}
		if (!Strings.isNullOrEmpty(prefixes)) {
			card.identification.name.prefixes = prefixes.trim();
			if (names.isEmpty()) {
				names.add(prefixes.trim());
			}
		}
		if (!Strings.isNullOrEmpty(suffix)) {
			card.identification.name.suffixes = suffix.trim();
			if (names.isEmpty()) {
				names.add(suffix.trim());
			}
		}

		if (names.isEmpty() && card.kind == Kind.individual) {
			// card.kind == Kind.individual because group directly set
			// formatedName.value
			// org is not yet supported so we use companyname for company
			if (!Strings.isNullOrEmpty(card.organizational.org.company)) {
				names.add(card.organizational.org.company);
			}
		}

		if (names.isEmpty() && card.kind == Kind.individual) {
			// card.kind == Kind.individual because group directly set
			// formatedName.value
			String defaultEmail = null;
			for (Email mail : card.communications.emails) {
				defaultEmail = mail.value;
				if (mail.defaultEmail()) {
					break;
				}
			}
			if (null != defaultEmail) {
				names.add(defaultEmail);
			}
		}

		return !names.isEmpty() ? StringUtils.join(names, " ") : null;
	}

	@Override
	public void create(VCard obj) throws ServerFault {
		create(obj, Collections.emptyMap());
	}

	@Override
	public void update(VCard current, VCard obj) throws ServerFault {
		update(current, obj, Collections.emptyMap());
	}

	@Override
	public void create(VCard entity, Map<String, String> params) {
		sanitize(entity, Optional.ofNullable(params.get("containerUid")));
	}

	@Override
	public void update(VCard current, VCard entity, Map<String, String> params) {
		sanitize(entity, Optional.ofNullable(params.get("containerUid")));
	}

}
