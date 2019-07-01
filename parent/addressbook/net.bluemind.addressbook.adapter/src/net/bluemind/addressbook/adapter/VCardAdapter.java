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

package net.bluemind.addressbook.adapter;

import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.codec.DecoderException;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.core.api.Regex;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.lib.ical4j.vcard.property.AddressbookServerKind;
import net.bluemind.lib.ical4j.vcard.property.AddressbookServerMember;
import net.bluemind.tag.api.TagRef;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.vcard.ParameterFactory;
import net.fortuna.ical4j.vcard.ParameterFactoryRegistry;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.Property.Id;
import net.fortuna.ical4j.vcard.property.Address;
import net.fortuna.ical4j.vcard.property.BDay;
import net.fortuna.ical4j.vcard.property.Categories;
import net.fortuna.ical4j.vcard.property.Email;
import net.fortuna.ical4j.vcard.property.Fn;
import net.fortuna.ical4j.vcard.property.Gender;
import net.fortuna.ical4j.vcard.property.Impp;
import net.fortuna.ical4j.vcard.property.Key;
import net.fortuna.ical4j.vcard.property.Kind;
import net.fortuna.ical4j.vcard.property.Member;
import net.fortuna.ical4j.vcard.property.N;
import net.fortuna.ical4j.vcard.property.Nickname;
import net.fortuna.ical4j.vcard.property.Note;
import net.fortuna.ical4j.vcard.property.Org;
import net.fortuna.ical4j.vcard.property.Role;
import net.fortuna.ical4j.vcard.property.Telephone;
import net.fortuna.ical4j.vcard.property.Title;
import net.fortuna.ical4j.vcard.property.Url;
import net.fortuna.ical4j.vcard.property.Version;

public final class VCardAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(VCardAdapter.class);
	private static final Joiner join = Joiner.on(',').skipNulls();
	private static final String containerSeparator = "#";
	private static final ParameterFactoryRegistry parameterFactoryRegistry;

	private static final Map<String, String> KNOWN_TYPES = new HashMap<>();

	static {
		KNOWN_TYPES.put("home", "home");
		KNOWN_TYPES.put("work", "work");
		KNOWN_TYPES.put("text", "text");
		KNOWN_TYPES.put("voice", "voice");
		KNOWN_TYPES.put("fax", "fax");
		KNOWN_TYPES.put("cell", "cell");
		KNOWN_TYPES.put("video", "video");
		KNOWN_TYPES.put("pager", "pager");
		KNOWN_TYPES.put("ttytdd", "textphone");
		parameterFactoryRegistry = new ParameterFactoryRegistry();
		parameterFactoryRegistry.register("EXTENDED", new ExtendedFactory());
	}

	public static final List<net.fortuna.ical4j.vcard.VCard> parse(String vcard) {
		List<net.fortuna.ical4j.vcard.VCard> ret = new LinkedList<>();
		try (ProgressiveVCardBuilder builder = new ProgressiveVCardBuilder(new StringReader(vcard))) {
			while (builder.hasNext()) {
				net.fortuna.ical4j.vcard.VCard card = builder.next();
				if (card != null) {
					ret.add(card);
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Cannot parse vcard string", e);
		}
		return ret;
	}

	public static final ItemValue<VCard> adaptCard(net.fortuna.ical4j.vcard.VCard card,
			Function<String, String> uidGenerator) {
		String retUid = UIDGenerator.uid();
		VCard retCard = new VCard();

		Property uidProp = card.getProperty(Id.UID);
		if (uidProp != null) {
			String value = uidProp.getValue();
			if (value.contains(",")) {
				retUid = uidGenerator.apply(value.split(",")[1]);
			}
		}

		Property fn = card.getProperty(Id.FN);
		if (fn != null) {
			retCard.identification.formatedName = VCard.Identification.FormatedName.create(fn.getValue(),
					fromVCard(fn.getParameters()));
		}

		N name = (N) card.getProperty(Id.N);
		if (name != null) {
			retCard.identification.name = VCard.Identification.Name.create(name.getFamilyName(), name.getGivenName(),
					asString(name.getAdditionalNames()), asString(name.getPrefixes()), asString(name.getSuffixes()),
					fromVCard(name.getParameters()));

		}

		Nickname nn = (Nickname) card.getProperty(Id.NICKNAME);
		if (nn != null) {
			retCard.identification.nickname = VCard.Identification.Nickname.create(nn.getValue());
		}

		BDay bday = (BDay) card.getProperty(Id.BDAY);
		if (bday != null) {
			retCard.identification.birthday = bday.getDate();
		}

		Gender gender = (Gender) card.getProperty(Id.GENDER);
		if (gender != null) {
			retCard.identification.gender = VCard.Identification.Gender.create(gender.getValue(), null);
		}

		List<Property> props = card.getProperties(Id.ADR);
		List<DeliveryAddressing> das = new ArrayList<>(props.size());
		for (Property p : props) {
			Address adr = (Address) p;
			net.fortuna.ical4j.vcard.Parameter valueParam = p.getParameter(net.fortuna.ical4j.vcard.Parameter.Id.VALUE);
			String value = null;
			if (valueParam != null) {
				value = valueParam.getValue();
			}
			das.add(VCard.DeliveryAddressing.create(VCard.DeliveryAddressing.Address.create(value, adr.getPoBox(),
					adr.getExtended(), adr.getStreet(), adr.getLocality(), adr.getRegion(), adr.getPostcode(),
					adr.getCountry(), fromVCard(adr.getParameters()))));
		}

		retCard.deliveryAddressing = das;

		List<Property> telProps = card.getProperties(Id.TEL);
		List<Tel> tels = new ArrayList<>(telProps.size());
		for (Property p : telProps) {
			Telephone tel = (Telephone) p;

			tels.add(Tel.create(tel.getValue(), fromVCard(tel.getParameters())));
		}

		retCard.communications.tels = tels;

		List<Property> mailProps = card.getProperties(Id.EMAIL);
		List<Communications.Email> mails = new ArrayList<>(mailProps.size());
		for (Property p : mailProps) {
			Email mail = (Email) p;
			String emailValue = mail.getValue();
			if (!Regex.EMAIL.validate(emailValue)) {
				try {
					Mailbox mbox = AddressBuilder.DEFAULT.parseMailbox(emailValue);
					emailValue = mbox.getAddress();
				} catch (org.apache.james.mime4j.dom.field.ParseException e) {
					// skip email
				}
				// if everything fail, we import email as it (because rfc
				// authorize it)
			}
			mails.add(Communications.Email.create(emailValue, fromVCard(mail.getParameters())));
		}
		retCard.communications.emails = mails;

		List<Property> imppProps = card.getProperties(Id.IMPP);
		List<Communications.Impp> impps = new ArrayList<>(imppProps.size());
		for (Property p : imppProps) {
			Impp impp = (Impp) p;
			impps.add(Communications.Impp.create(impp.getValue(), fromVCard(impp.getParameters())));
		}
		retCard.communications.impps = impps;

		Role role = (Role) card.getProperty(Id.ROLE);
		if (role != null) {
			retCard.organizational.role = role.getValue();
		}

		Title title = (Title) card.getProperty(Id.TITLE);
		if (title != null) {
			retCard.organizational.title = title.getValue();
		}

		Org org = (Org) card.getProperty(Id.ORG);
		if (org != null) {
			String[] values = org.getValues();
			String company = null;
			String division = null;
			String department = null;
			if (values.length >= 1) {
				company = values[0];
			}
			if (values.length >= 2) {
				division = values[1];
			}
			if (values.length >= 3) {
				department = values[2];
			}
			retCard.organizational.org = VCard.Organizational.Org.create(company, division, department);
		}

		List<Property> urlProps = card.getProperties(Id.URL);
		List<VCard.Explanatory.Url> urls = new ArrayList<>(urlProps.size());
		for (Property p : urlProps) {
			Url url = (Url) p;
			urls.add(VCard.Explanatory.Url.create(url.getValue(), fromVCard(url.getParameters())));
		}
		retCard.explanatory.urls = urls;

		Property key = card.getProperty(Id.KEY);
		if (key != null) {
			retCard.security.key = VCard.Security.Key.create(key.getValue(), fromVCard(key.getParameters()));
		}

		List<Property> categoriesProps = card.getProperties(Id.CATEGORIES);
		List<TagRef> cats = new ArrayList<>(categoriesProps.size());
		for (Property p : categoriesProps) {
			Categories cat = (Categories) p;
			@SuppressWarnings("unchecked")
			Iterator<String> it = cat.getCategories().iterator();
			while (it.hasNext()) {
				String v = it.next();
				// FIXME auto create tags ?
				TagRef ref = new TagRef();
				ref.color = "000000";
				ref.label = v;
				cats.add(ref);
			}
		}
		retCard.explanatory.categories = cats;

		Note noteProp = (Note) card.getProperty(Id.NOTE);
		if (noteProp != null) {
			retCard.explanatory.note = noteProp.getValue();
		}

		// handle X-Macintosh
		// X-ADDRESSBOOKSERVER-KIND
		Property xKind = card.getExtendedProperty("ADDRESSBOOKSERVER-KIND");
		if (xKind != null && "group".equals(xKind.getValue())) {
			retCard.kind = VCard.Kind.group;

			List<Property> members = card.getExtendedProperties("ADDRESSBOOKSERVER-MEMBER");

			retCard.organizational.member = members.stream().filter(p -> p.getValue().startsWith("urn:uuid:"))
					.map(p -> {
						String value = p.getValue().substring("urn:uuid:".length());
						String containerUid = null;
						String uid = null;
						String memberName = getParamValue(p.getParameters(), "X-CN").orElse(null);
						String memberEmail = getParamValue(p.getParameters(), "X-MAILTO").orElse(null);
						if (value.contains(containerSeparator)) {
							String[] splitted = value.split(containerSeparator);
							containerUid = splitted[0];
							uid = splitted[1];
							if (memberName == null) {
								memberName = uid;
							}
						} else {
							uid = uidGenerator.apply(value);
							if (Regex.EMAIL.validate(value) && memberEmail == null) {
								memberEmail = value;
							}
							if (memberName == null) {
								memberName = value;
							}
						}
						return VCard.Organizational.Member.create(containerUid, uid, memberName, memberEmail);
					}).collect(Collectors.toList());
		}

		return ItemValue.create(retUid, retCard);
	}

	private static Optional<String> getParamValue(List<net.fortuna.ical4j.vcard.Parameter> list, String parameterId) {
		for (net.fortuna.ical4j.vcard.Parameter p : list) {
			if (p.getId() == net.fortuna.ical4j.vcard.Parameter.Id.EXTENDED && p.toString().contains(parameterId)) {
				return Optional.of(p.getValue());
			}
		}
		return Optional.empty();
	}

	public static final net.fortuna.ical4j.vcard.VCard adaptCard(String containerUid, VCard vcard,
			VCardVersion version) {

		net.fortuna.ical4j.vcard.VCard ret = new net.fortuna.ical4j.vcard.VCard();

		List<Property> properties = ret.getProperties();

		properties.add(new Version("3.0"));

		switch (vcard.kind) {
		case group:
			if (version == VCardVersion.v4) {
				properties.add(new Kind("group"));
			} else {
				properties.add(new AddressbookServerKind("group"));
			}
			break;
		case individual:
			properties.add(new Kind("individual"));
			break;
		case org:
			properties.add(new Kind("org"));
			break;
		default:
			throw new IllegalArgumentException("doesnt support kind [" + vcard.kind + "]");
		}
		if (vcard.identification.formatedName != null) {
			properties.add(new Fn(vcard.identification.formatedName.value));
		}

		if (vcard.identification.name != null) {
			properties.add(new N(toVCard(vcard.identification.name.parameters),
					valueOrEmpty(vcard.identification.name.familyNames) + ";"
							+ valueOrEmpty(vcard.identification.name.givenNames) + ";"
							+ valueOrEmpty(vcard.identification.name.additionalNames) + ";"
							+ valueOrEmpty(vcard.identification.name.prefixes) + ";"
							+ valueOrEmpty(vcard.identification.name.suffixes)));
		}

		if (vcard.identification.nickname != null && vcard.identification.nickname.value != null) {
			properties.add(new Nickname(toVCard(vcard.identification.nickname.parameters),
					vcard.identification.nickname.value));
		}

		if (vcard.identification.birthday != null) {
			properties.add(new BDay(new Date(vcard.identification.birthday.getTime())));
		}

		if (vcard.identification.gender != null) {
			properties.add(new Gender(vcard.identification.gender.value));
		}

		for (DeliveryAddressing da : vcard.deliveryAddressing) {
			if (da.address != null) {
				properties
						.add(new Address(da.address.postOfficeBox, da.address.extentedAddress, da.address.streetAddress,
								da.address.locality, da.address.region, da.address.postalCode, da.address.countryName));

			}
		}

		for (Tel tel : vcard.communications.tels) {
			try {
				properties.add(new Telephone(toVCard(tel.parameters), tel.value));
			} catch (URISyntaxException e) {
				LOGGER.warn("error during vcard export", e);
			}

		}

		for (Communications.Email mail : vcard.communications.emails) {
			properties.add(new Email(toVCard(mail.parameters), mail.value));
		}

		for (Communications.Impp impp : vcard.communications.impps) {
			try {
				properties.add(new Impp(toVCard(impp.parameters), impp.value));
			} catch (URISyntaxException e) {
				LOGGER.warn("error during vcard export", e);
			}
		}

		if (vcard.organizational.role != null) {
			properties.add(new Role(vcard.organizational.role));
		}

		if (vcard.organizational.title != null) {
			properties.add(new Title(vcard.organizational.title));
		}

		if (vcard.organizational.org != null) {
			String[] values = { //
					vcard.organizational.org.company != null ? vcard.organizational.org.company : "", //
					vcard.organizational.org.division != null ? vcard.organizational.org.division : "", //
					vcard.organizational.org.department != null ? vcard.organizational.org.department : "" };
			properties.add(new Org(values));
		}

		for (VCard.Explanatory.Url url : vcard.explanatory.urls) {
			try {
				properties.add(new Url(toVCard(url.parameters), url.value));
			} catch (URISyntaxException e) {
				LOGGER.warn(e.getMessage());
			}
		}
		if (vcard.explanatory.note != null) {
			properties.add(new Note(vcard.explanatory.note));
		}

		if (vcard.security.key != null && vcard.security.key.value != null) {
			try {
				properties.add(new Key(toVCard(vcard.security.key.parameters), vcard.security.key.value));
			} catch (URISyntaxException | DecoderException e) {
				LOGGER.warn(e.getMessage());
			}
		}

		for (VCard.Organizational.Member m : vcard.organizational.member) {
			if (version == VCardVersion.v3) {
				if (m.itemUid != null) {
					String exportedUid = getExportUid(containerUid, m);
					properties.add(new AddressbookServerMember(getMemberParams(m), "urn:uuid:" + exportedUid));
				} else if (m.mailto != null) {
					properties.add(new AddressbookServerMember("urn:uuid:" + m.mailto));
				} else {
					LOGGER.warn("member doesnt have uri or mailto");
				}
			} else {
				try {
					if (m.itemUid != null) {
						String exportedUid = getExportUid(containerUid, m);
						properties.add(new Member(getMemberParams(m), exportedUid));
					} else if (m.mailto != null) {
						properties.add(new Member(Collections.<net.fortuna.ical4j.vcard.Parameter>emptyList(),
								"urn:uuid:" + m.mailto));
					} else {
						LOGGER.warn("member doesnt have uri or mailto");
					}
				} catch (URISyntaxException e) {
					LOGGER.warn("Invalid uri: " + e.getMessage(), e);
				}
			}
		}
		return ret;
	}

	@SuppressWarnings("serial")
	private static List<net.fortuna.ical4j.vcard.Parameter> getMemberParams(VCard.Organizational.Member member) {
		List<net.fortuna.ical4j.vcard.Parameter> params = new ArrayList<>();
		if (member.commonName != null) {
			params.add(new net.fortuna.ical4j.vcard.Parameter("CN") {

				@Override
				public String getValue() {
					return member.commonName;
				}

			});
		}
		if (member.mailto != null) {
			params.add(new net.fortuna.ical4j.vcard.Parameter("MAILTO") {

				@Override
				public String getValue() {
					return member.mailto;
				}

			});

		}
		return params;
	}

	private static String getExportUid(String containerUid, VCard.Organizational.Member member) {
		return member.containerUid == null || member.containerUid.equals(containerUid) ? member.itemUid
				: member.containerUid + containerSeparator + member.itemUid;
	}

	private static String asString(String[] values) {
		if (values == null)
			return null;

		return join.join(values);
	}

	private static List<Parameter> fromVCard(List<net.fortuna.ical4j.vcard.Parameter> parameters) {

		List<Parameter> ret = new ArrayList<>(parameters.size());
		for (net.fortuna.ical4j.vcard.Parameter p : parameters) {
			String value = p.getValue();
			if (p.getId().getPname().equals("TYPE")) {
				for (String v : value.split(",")) {
					if (KNOWN_TYPES.keySet().contains(v.toLowerCase())) {
						ret.add(Parameter.create(p.getId().getPname(), KNOWN_TYPES.get(v.toLowerCase())));
					} else {
						ret.add(Parameter.create(p.getId().getPname(), v));
					}
				}
			} else {
				ret.add(Parameter.create(p.getId().getPname(), p.getValue()));
			}
		}
		return ret;
	}

	private static List<net.fortuna.ical4j.vcard.Parameter> toVCard(List<Parameter> parameters) {
		List<net.fortuna.ical4j.vcard.Parameter> ret = new ArrayList<>(parameters.size());
		for (Parameter p : parameters) {
			ParameterFactory<?> paramFactory = parameterFactoryRegistry.getFactory(p.label);
			if (paramFactory == null) {
				LOGGER.warn("********************** Missing factory for label {}", p.label);
			} else {
				ret.add(paramFactory.createParameter(p.value));
			}
		}
		return ret;
	}

	private static String valueOrEmpty(String value) {
		if (value == null)
			return "";
		else
			return value;
	}

}
