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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.core.api.BMApi;
import net.bluemind.tag.api.TagRef;

/**
 * The vcard contains contact information, typically pertaining to a single
 * contact or group of contacts.
 */
@BMApi(version = "3")
public class VCard {

	public Kind kind = Kind.individual;

	/**
	 * To identify the source of directory information contained in the content
	 * type.
	 */
	public String source;

	/**
	 * To specify the kind of object the vCard represents.
	 */
	@BMApi(version = "3")
	public static enum Kind {
		/**
		 * for a vCard representing a group of persons or entities.
		 */
		group, //

		/**
		 * for a vCard representing a single person or entity.
		 */
		individual,

		/**
		 * for a named geographical place
		 */
		location, //

		/**
		 * for a vCard representing an organization.
		 */
		org
	}

	public Identification identification = new Identification();
	public List<DeliveryAddressing> deliveryAddressing = Collections.emptyList();
	public Communications communications = new Communications();
	public Organizational organizational = new Organizational();
	public Explanatory explanatory = new Explanatory();
	public Security security = new Security();
	public Related related = new Related();

	/**
	 * These types are used to capture information associated with the
	 * identification and naming of the entity associated with the vCard.
	 */
	@BMApi(version = "3")
	public static class Identification {

		public FormatedName formatedName = new FormatedName();
		public Identification.Name name = new Name();
		public Identification.Nickname nickname = new Nickname();
		public boolean photo = false;

		public Date birthday;
		public Date anniversary;
		public Gender gender = new Gender();

		/**
		 * To specify the formatted text corresponding to the name of the object the
		 * vCard represents.
		 */
		@BMApi(version = "3")
		public static class FormatedName extends BasicAttribute {
			public static FormatedName create(String value, List<Parameter> parameters) {
				FormatedName ret = new FormatedName();
				ret.value = value;
				ret.parameters = parameters;
				return ret;
			}

			public static FormatedName create(String value) {
				return create(value, Arrays.<Parameter>asList());
			}

			public FormatedName copy() {
				return create(value, copyParameters());
			}
		}

		/**
		 * To specify the components of the name of the object the vCard represents.
		 */
		@BMApi(version = "3")
		public static class Name extends BasicAttribute {
			public String familyNames;
			public String givenNames;
			public String additionalNames;
			public String prefixes;
			public String suffixes;

			public static Name create(String familyNames, String givenNames, String additionalNames, String prefixes,
					String suffixes, List<Parameter> parameters) {
				Name ret = new Name();
				ret.familyNames = familyNames;
				ret.givenNames = givenNames;
				ret.additionalNames = additionalNames;
				ret.prefixes = prefixes;
				ret.suffixes = suffixes;

				ret.parameters = parameters;
				return ret;
			}

			public Name copy() {
				return create(familyNames, givenNames, additionalNames, prefixes, suffixes, copyParameters());
			}

		}

		/**
		 * To specify the text corresponding to the nickname of the object the vCard
		 * represents.
		 */
		@BMApi(version = "3")
		public static class Nickname extends BasicAttribute {
			public static Nickname create(String value) {
				Nickname ret = new Nickname();
				ret.value = value;
				return ret;
			}

			public static Nickname create(String value, List<Parameter> parameters) {
				Nickname ret = new Nickname();
				ret.value = value;
				ret.parameters = parameters;
				return ret;
			}

			public Nickname copy() {
				return create(value, copyParameters());
			}
		}

		/**
		 * To specify the components of the sex and gender identity of the object the
		 * vCard represents.
		 */
		@BMApi(version = "3")
		public static class Gender extends BasicAttribute {
			public String text;

			public static Gender create(String value, String text) {
				Gender ret = new Gender();
				ret.value = value;
				ret.text = text;
				return ret;
			}

			public Gender copy() {
				return create(value, text);
			}
		}

		public Identification copy() {

			Identification ret = new Identification();
			ret.formatedName = formatedName.copy();
			ret.name = name.copy();
			ret.nickname = nickname.copy();
			ret.birthday = birthday;
			ret.anniversary = anniversary;
			ret.gender = gender.copy();
			return ret;
		}

	}

	/**
	 * These types are concerned with information related to the delivery addressing
	 * or label for the vCard object.
	 */
	@BMApi(version = "3")
	public static class DeliveryAddressing {
		public Address address = new Address();

		/**
		 * To specify the components of the delivery address for the vCard object.
		 */
		@BMApi(version = "3")
		public static class Address extends BasicAttribute {
			public String postOfficeBox;
			public String extentedAddress;
			public String streetAddress;
			public String locality;
			public String region;
			public String postalCode;
			public String countryName;

			public static Address create(String label, String postOfficeBox, String extentedAddress,
					String streetAddress, String locality, String region, String postalCode, String countryName,
					List<Parameter> parameters) {
				Address ret = new Address();

				ret.value = label;
				ret.postOfficeBox = postOfficeBox;
				ret.extentedAddress = extentedAddress;
				ret.streetAddress = streetAddress;
				ret.locality = locality;
				ret.region = region;
				ret.postalCode = postalCode;
				ret.countryName = countryName;
				ret.parameters = parameters;

				return ret;

			}

			public Address copy() {
				return create(value, postOfficeBox, extentedAddress, streetAddress, locality, region, postalCode,
						countryName, copyParameters());
			}

		}

		public static DeliveryAddressing create(Address addr) {
			DeliveryAddressing ret = new DeliveryAddressing();
			ret.address = addr;
			return ret;
		}

		public DeliveryAddressing copy() {
			return create(address.copy());
		}
	}

	/**
	 * These properties describe information about how to communicate with the
	 * object the vCard represents.
	 */
	@BMApi(version = "3")
	public static class Communications {

		public List<Tel> tels = Collections.emptyList();
		public List<Email> emails = Collections.emptyList();
		public List<Impp> impps = Collections.emptyList();
		public List<Lang> langs = Collections.emptyList();

		/**
		 * To specify the telephone number for telephony communication with the object
		 * the vCard represents.
		 */
		@BMApi(version = "3")
		public static class Tel extends BasicAttribute {
			public String ext;

			public static Tel create(String value, List<Parameter> parameters) {
				Tel ret = new Tel();
				ret.value = value;
				ret.parameters = parameters;
				return ret;
			}

			public Tel copy() {
				return create(value, copyParameters());
			}

		}

		/**
		 * To specify the electronic mail address for communication with the object the
		 * vCard represents.
		 */
		@BMApi(version = "3")
		public static class Email extends BasicAttribute {
			public static Email create(String value) {
				return create(value, Arrays.<Parameter>asList());
			}

			public static Email create(String value, List<Parameter> parameters) {
				Email ret = new Email();
				ret.value = value;
				ret.parameters = parameters;
				return ret;
			}

			public boolean defaultEmail() {
				for (Parameter parameter : parameters) {
					if (parameter.label.toLowerCase().equals("default")
							&& parameter.value.toLowerCase().equals("true")) {
						return true;
					}
				}
				return false;
			}

			public Email copy() {
				return create(value, copyParameters());
			}
		}

		/**
		 * To specify the URI for instant messaging and presence protocol communications
		 * with the object the vCard represents.
		 */
		@BMApi(version = "3")
		public static class Impp extends BasicAttribute {
			public static Impp create(String value, List<Parameter> parameters) {
				Impp ret = new Impp();
				ret.value = value;
				ret.parameters = parameters;
				return ret;
			}

			public Impp copy() {
				return create(value, copyParameters());

			}
		}

		/**
		 * To specify the language(s) that may be used for contacting the entity
		 * associated with the vCard.
		 */
		@BMApi(version = "3")
		public static class Lang extends BasicAttribute {
			public static Lang create(String value, List<Parameter> parameters) {
				Lang ret = new Lang();
				ret.value = value;
				ret.parameters = parameters;
				return ret;
			}

			public Lang copy() {
				return create(value, copyParameters());
			}
		}

		public Communications copy() {
			Communications ret = new Communications();
			ret.emails = new ArrayList<>(emails.size());
			for (int i = 0; i < emails.size(); i++) {
				ret.emails.add(emails.get(i).copy());
			}

			ret.impps = new ArrayList<>(impps.size());
			for (int i = 0; i < impps.size(); i++) {
				ret.impps.add(impps.get(i).copy());
			}

			ret.tels = new ArrayList<>(tels.size());
			for (int i = 0; i < tels.size(); i++) {
				ret.tels.add(tels.get(i).copy());
			}

			ret.langs = new ArrayList<>(langs.size());
			for (int i = 0; i < langs.size(); i++) {
				ret.langs.add(langs.get(i).copy());
			}

			return ret;
		}
	}

	/**
	 * These properties are concerned with information associated with
	 * characteristics of the organization or organizational units of the object
	 * that the vCard represents.
	 *
	 */
	@BMApi(version = "3")
	public static class Organizational {

		/**
		 * To specify the position or job of the object the vCard represents.
		 */
		public String title;

		/**
		 * To specify the function or part played in a particular situation by the
		 * object the vCard represents.
		 */
		public String role;

		public Org org = new Org();

		/**
		 * To include a member in the group this vCard represents.
		 */
		public List<Member> member = Collections.emptyList();

		/**
		 * To specify the organizational name and units associated with the vCard.
		 *
		 */
		@BMApi(version = "3")
		public static class Org {
			public String company;
			public String division;
			public String department;

			public static Org create(String companyName, String division, String department) {
				Org ret = new Org();
				ret.company = companyName;
				ret.division = division;
				ret.department = department;
				return ret;
			}

			public Org copy() {
				return create(company, division, department);
			}
		}

		public static Organizational create(String title, String role, Org org, List<Member> member) {
			Organizational ret = new Organizational();
			ret.title = title;
			ret.role = role;
			ret.org = org;
			ret.member = member;
			return ret;
		}

		@BMApi(version = "3")
		public static class Member {
			public String commonName;
			public String mailto;
			public String containerUid;
			public String itemUid;

			public static Member create(String containerUid, String itemUid, String commonName, String mailto) {
				Member ret = new Member();
				ret.containerUid = containerUid;
				ret.itemUid = itemUid;
				ret.commonName = commonName;
				ret.mailto = mailto;
				return ret;
			}

			public Member copy() {
				return create(containerUid, itemUid, commonName, mailto);
			}
		}

		public Organizational copy() {
			Organizational ret = new Organizational();
			ret.title = title;
			ret.org = org.copy();
			ret.member = new ArrayList<>(member.size());
			for (Member mem : member) {
				ret.member.add(mem.copy());
			}

			ret.role = role;

			return ret;
		}
	}

	/**
	 * 
	 * These properties are concerned with additional explanations, such as that
	 * related to informational notes or revisions specific to the vCard.
	 */
	@BMApi(version = "3")
	public static class Explanatory {

		/**
		 * To specify a uniform resource locator associated with the object to which the
		 * vCard refers. Examples for individuals include personal web sites, blogs, and
		 * social networking site identifiers.
		 */
		public List<Url> urls = Collections.emptyList();

		/**
		 * To specify application category information about the vCard, also known as
		 * "tags".
		 */
		public List<TagRef> categories = Collections.emptyList();

		/**
		 * To specify supplemental information or a comment that is associated with the
		 * vCard.
		 */
		public String note;

		@BMApi(version = "3")
		public static class Url extends BasicAttribute {
			public static Url create(String value, List<Parameter> parameters) {
				Url ret = new Url();
				ret.value = value;
				ret.parameters = parameters;
				return ret;
			}

			public Url copy() {
				return create(value, copyParameters());
			}

		}

		public static Explanatory create(List<Url> urls, List<TagRef> categories, String note) {
			Explanatory ret = new Explanatory();
			ret.urls = urls;
			ret.categories = categories;
			ret.note = note;
			return ret;
		}

		public Explanatory copy() {
			Explanatory ret = new Explanatory();
			ret.urls = new ArrayList<>(urls.size());
			for (Url url : urls) {
				ret.urls.add(url.copy());
			}

			ret.categories = new ArrayList<>(categories.size());
			for (TagRef cat : categories) {
				ret.categories.add(cat);
			}

			ret.note = note;
			return ret;
		}
	}

	/**
	 * These properties are concerned with the security of communication pathways or
	 * access to the vCard.
	 */
	@BMApi(version = "3")
	public static class Security {

		/**
		 * To specify a public key or authentication certificate associated with the
		 * object that the vCard represents
		 */
		public Key key = new Key();

		@BMApi(version = "3")
		public static class Key extends BasicAttribute {
			public static Key create(String value, List<Parameter> parameters) {
				Key ret = new Key();
				ret.value = value;
				ret.parameters = parameters;
				return ret;
			}
		}

		public static Security create(Key key) {
			Security security = new Security();
			security.key = key;
			return security;
		}

	}

	/**
	 * 
	 * To specify a relationship the individual this vCard represents has with
	 * another.
	 *
	 */
	@BMApi(version = "3")
	public static class Related {
		public String spouse;
		public String manager;
		public String assistant;

		public Related copy() {
			Related ret = new Related();
			ret.spouse = spouse;
			ret.manager = manager;
			ret.assistant = assistant;
			return ret;
		}
	}

	@BMApi(version = "3")
	public static class BasicAttribute {
		public List<Parameter> parameters = Collections.emptyList();
		public String value;

		public String getParameterValue(String name) {
			for (Parameter p : parameters) {
				if (name.equals(p.label)) {
					return p.value;
				}
			}
			return null;
		}

		public List<String> getParameterValues(String name) {
			List<String> values = new ArrayList<>(parameters.size());
			for (Parameter p : parameters) {
				if (name.equals(p.label)) {
					values.add(p.value);
				}
			}
			return values;
		}

		public boolean containsValues(String name, String... values) {
			List<String> pValues = getParameterValues(name);
			if (pValues.isEmpty()) {
				return false;
			}
			for (String value : values) {
				if (!pValues.contains(value)) {
					return false;
				}
			}
			return true;
		}

		public boolean containsUniqueValue(String name, String value) {
			List<String> pValues = getParameterValues(name);
			if (pValues.isEmpty()) {
				return false;
			}
			if (!pValues.contains(value)) {
				return false;
			}
			return true;
		}

		public List<Parameter> copyParameters() {
			List<Parameter> ret = new ArrayList<>(parameters.size());
			for (Parameter p : parameters) {
				ret.add(Parameter.create(p.label, p.value));
			}
			return ret;
		}

	}

	@BMApi(version = "3")
	public static class Parameter {
		public String label;
		public String value;

		public static Parameter create(String label, String value) {
			Parameter ret = new Parameter();
			ret.label = label;
			ret.value = value;
			return ret;
		}

	}

	public String defaultMail() {
		String ret = null;
		// if there isn't default mail, it will be the first one
		if (communications.emails.size() >= 1) {
			ret = communications.emails.get(0).value;
		}
		for (Email mail : communications.emails) {
			if (mail.defaultEmail()) {
				ret = mail.value;
				break;
			}
		}

		return ret;
	}

	@Override
	public String toString() {
		return "VCard [kind=" + kind + ", cn="
				+ ((identification != null && identification.formatedName != null) ? identification.formatedName.value
						: "none")
				+ "]";
	}

	public VCard copy() {
		VCard ret = new VCard();
		ret.communications = communications.copy();

		ret.deliveryAddressing = new ArrayList<>(deliveryAddressing.size());
		for (int i = 0; i < deliveryAddressing.size(); i++) {
			ret.deliveryAddressing.add(deliveryAddressing.get(i).copy());
		}

		ret.explanatory = explanatory.copy();
		ret.identification = identification.copy();
		ret.kind = kind;
		ret.organizational = organizational.copy();
		ret.related = related.copy();
		ret.source = source;
		return ret;
	}

}
