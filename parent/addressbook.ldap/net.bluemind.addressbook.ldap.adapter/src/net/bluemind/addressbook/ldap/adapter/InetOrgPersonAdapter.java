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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.addressbook.ldap.adapter;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.addressbook.ldap.adapter.LdapContact.ErrCode;
import net.bluemind.addressbook.ldap.adapter.helper.VCardHelper;
import net.bluemind.addressbook.ldap.api.LdapParameters;
import net.bluemind.addressbook.ldap.api.LdapParameters.DirectoryType;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.utils.ImageUtils;
import net.bluemind.lib.ldap.SidGuidHelper;

public class InetOrgPersonAdapter {

	private static final Logger logger = LoggerFactory.getLogger(InetOrgPersonAdapter.class);

	// ID
	private static final String LDAP_FIRSTNAME = "givenName";
	private static final String LDAP_LASTNAME = "sn";

	// ORGANIZATIONAL
	private static final String LDAP_O = "o";
	private static final String LDAP_OU = "ou";
	private static final String LDAP_TITLE = "title";

	private static final String LDAP_DESCRIPTION = "description";
	private static final String LDAP_PHOTO = "jpegPhoto";
	private static final String AD_PHOTO = "thumbnailPhoto";

	// Emails
	private static final String LDAP_EMAIL_HOME = "mail";

	// Phones
	private static final String LDAP_PHONE_WORK = "telephoneNumber";
	private static final String LDAP_PHONE_HOME = "homePhone";
	private static final String LDAP_PHONE_MOBILE = "mobile";
	private static final String LDAP_PHONE_FAX = "facsimileTelephoneNumber";
	private static final String LDAP_PAGER = "pager";

	// DELIVERY ADDRESS
	private static final String LDAP_STREET = "street";
	private static final String LDAP_POSTALCODE = "postalCode";
	private static final String LDAP_LOCALITY = "l";
	private static final String LDAP_POSTAL_ADDRESS = "postalAddress";
	private static final String LDAP_HOME_POSTAL_ADDRESS = "homePostalAddress";

	public static String getUid(LdapParameters.DirectoryType type, Entry entry, String uid)
			throws LdapInvalidAttributeValueException {
		String ret = null;

		if (entry.containsAttribute(uid)) {
			if (type == DirectoryType.ldap) {
				ret = entry.get(uid).getString();
			} else {
				ret = SidGuidHelper.convertGuidToString(entry.get(uid).getBytes());
			}
		}

		return ret;
	}

	public static LdapContact getVCard(Entry entry, DirectoryType type, String uid)
			throws LdapInvalidAttributeValueException {
		LdapContact lc = new LdapContact();
		lc.vcard = new VCard();

		if (entry.containsAttribute(uid)) {
			lc.uid = getUid(type, entry, uid);
		}

		// ID
		if (entry.containsAttribute(LDAP_FIRSTNAME)) {
			lc.vcard.identification.name.givenNames = entry.get(LDAP_FIRSTNAME).getString();
		}

		if (entry.containsAttribute(LDAP_LASTNAME)) {
			lc.vcard.identification.name.familyNames = (entry.get(LDAP_LASTNAME).getString());
		}

		managePhoto(type, entry, lc);

		// ORGANIZATIONAL
		if (entry.containsAttribute(LDAP_O)) {
			lc.vcard.organizational.org.company = (entry.get(LDAP_O).getString());
		}

		if (entry.containsAttribute(LDAP_OU)) {
			lc.vcard.organizational.org.department = (entry.get(LDAP_OU).getString());
		}

		if (entry.containsAttribute(LDAP_TITLE)) {
			lc.vcard.organizational.title = (entry.get(LDAP_TITLE).getString());
		}

		// EXPLANATORY
		if (entry.containsAttribute(LDAP_DESCRIPTION)) {
			lc.vcard.explanatory.note = entry.get(LDAP_DESCRIPTION).getString();
		}

		// PHONES
		lc.vcard.communications.tels = new ArrayList<Tel>();
		lc.vcard.communications.tels
				.addAll(VCardHelper.managePhones(entry.get(LDAP_PHONE_FAX), Arrays.asList("fax", "work")));
		lc.vcard.communications.tels
				.addAll(VCardHelper.managePhones(entry.get(LDAP_PHONE_HOME), Arrays.asList("voice", "home")));
		lc.vcard.communications.tels
				.addAll(VCardHelper.managePhones(entry.get(LDAP_PHONE_WORK), Arrays.asList("voice", "work")));
		lc.vcard.communications.tels
				.addAll(VCardHelper.managePhones(entry.get(LDAP_PHONE_MOBILE), Arrays.asList("cell", "voice")));
		lc.vcard.communications.tels
				.addAll(VCardHelper.managePhones(entry.get(LDAP_PAGER), Arrays.asList("pager", "voice")));

		// EMAILS
		lc.vcard.communications.emails = new ArrayList<Email>();
		lc.vcard.communications.emails = VCardHelper.manageEmails(entry.get(LDAP_EMAIL_HOME), "home");

		// POSTAL ADDRESSES
		lc.vcard.deliveryAddressing = new ArrayList<DeliveryAddressing>();
		if (entry.containsAttribute(LDAP_STREET) || entry.containsAttribute(LDAP_POSTALCODE)
				|| entry.containsAttribute(LDAP_LOCALITY)) {

			DeliveryAddressing addr = new DeliveryAddressing();
			addr.address.parameters = new ArrayList<Parameter>();
			addr.address.parameters.add(Parameter.create("TYPE", "work"));
			if (entry.containsAttribute(LDAP_STREET)) {
				addr.address.streetAddress = (entry.get(LDAP_STREET).getString());
			}
			if (entry.containsAttribute(LDAP_POSTALCODE)) {
				addr.address.postalCode = (entry.get(LDAP_POSTALCODE).getString());
			}
			if (entry.containsAttribute(LDAP_LOCALITY)) {
				addr.address.locality = (entry.get(LDAP_LOCALITY).getString());
			}

			lc.vcard.deliveryAddressing.add(addr);

		}

		lc.vcard.deliveryAddressing.addAll(VCardHelper.manageAddress(entry.get(LDAP_POSTAL_ADDRESS), "work"));
		lc.vcard.deliveryAddressing.addAll(VCardHelper.manageAddress(entry.get(LDAP_HOME_POSTAL_ADDRESS), "home"));

		return lc;
	}

	private static void managePhoto(DirectoryType type, Entry entry, LdapContact lc)
			throws LdapInvalidAttributeValueException {
		// PHOTO
		String photoAttr = LDAP_PHOTO;
		if (type == LdapParameters.DirectoryType.ad) {
			photoAttr = AD_PHOTO;
		}

		if (entry.containsAttribute(photoAttr)) {
			if (!entry.get(photoAttr).isHumanReadable()) {
				try {
					lc.photo = ImageUtils.resize(ImageUtils.checkAndSanitize(entry.get(photoAttr).getBytes()), 96, 96);
				} catch (ServerFault sf) {
					logger.warn("Fail to fetch photo for vcard uid: '{}' ({})", lc.uid,
							lc.vcard.identification.formatedName.value, sf.getMessage());
					lc.err = ErrCode.jpegPhoto;
				}
			} else {
				// non-base64 jpegPhoto
				logger.info("Unsupppoted jpegPhoto '{}'", entry.get(photoAttr));
				lc.err = ErrCode.jpegPhoto;
			}
		}
	}
}
