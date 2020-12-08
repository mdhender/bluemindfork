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
package net.bluemind.eas.backend.bm.contacts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Impp;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.Explanatory.Url;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.MSContact;
import net.bluemind.eas.data.formatter.PlainBodyFormatter;
import net.bluemind.eas.dto.search.GAL;
import net.bluemind.eas.dto.search.SearchRequest;
import net.bluemind.eas.dto.search.SearchResult;

/**
 * Converts between BM & MS Exchange contact models
 * 
 * 
 */
public class ContactConverter {

	private static Logger logger = LoggerFactory.getLogger(ContactConverter.class);

	/**
	 * BM to Device
	 * 
	 * @param service
	 * 
	 * @param c
	 * @return
	 */
	public MSContact convert(IAddressBook service, ItemValue<VCard> vcardItem) {
		VCard vcard = vcardItem.value;
		MSContact msc = new MSContact();

		msc.setFirstName(vcard.identification.name.givenNames);
		msc.setLastName(vcard.identification.name.familyNames);
		msc.setMiddleName(vcard.identification.name.additionalNames);
		msc.setTitle(vcard.identification.name.prefixes);
		msc.setSuffix(vcard.identification.name.suffixes);
		msc.setNickName(vcard.identification.nickname.value);

		msc.setJobTitle(vcard.organizational.title);
		msc.setDepartment(vcard.organizational.org.department);
		if (vcard.organizational.org.company != null) {
			msc.setCompanyName(vcard.organizational.org.company);
		}
		if (vcard.explanatory != null && vcard.explanatory.urls != null && vcard.explanatory.urls.size() > 0) {
			msc.setWebPage(vcard.explanatory.urls.get(0).value);
		}

		if (vcard.identification.birthday != null) {
			Calendar bday = Calendar.getInstance();
			bday.setTime(vcard.identification.birthday);
			bday.set(Calendar.HOUR_OF_DAY, 0);
			bday.set(Calendar.MINUTE, 0);
			bday.set(Calendar.SECOND, 0);
			bday.set(Calendar.MILLISECOND, 0);
			msc.setBirthday(bday.getTime());
		}

		if (vcard.identification.anniversary != null) {
			Calendar anniv = Calendar.getInstance();
			anniv.setTime(vcard.identification.anniversary);
			anniv.set(Calendar.HOUR_OF_DAY, 0);
			anniv.set(Calendar.MINUTE, 0);
			anniv.set(Calendar.SECOND, 0);
			anniv.set(Calendar.MILLISECOND, 0);
			msc.setAnniversary(anniv.getTime());
		}

		if (true == vcard.identification.photo) {
			try {
				byte[] photo = service.getPhoto(vcardItem.uid);
				if (photo != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Syncing photo of uid {}, size: {}", vcardItem.uid, photo.length);
					}
					msc.setPicture(Base64.getEncoder().encodeToString(photo));
				}
			} catch (ServerFault e) {
				logger.warn("Cannot load contact photo of uid {}:{}", vcardItem.uid, e.getMessage());
			}
		}

		msc.setSpouse(vcard.related.spouse);
		msc.setManagerName(vcard.related.manager);
		msc.setAssistantName(vcard.related.assistant);

		PlainBodyFormatter pf = new PlainBodyFormatter();
		msc.setData(pf.convert(vcard.explanatory.note));

		setPhones(vcard, msc);

		setEmails(vcard, msc);

		setAddresses(vcard, msc);

		setIMs(vcard, msc);

		return msc;
	}

	/**
	 * @param vcard
	 * @param msc
	 */
	private void setIMs(VCard vcard, MSContact msc) {
		List<Impp> impps = vcard.communications.impps;
		int i = 0;
		for (Impp impp : impps) {
			switch (i++) {
			case 0:
				msc.setIMAddress(impp.value);
				break;
			case 1:
				msc.setIMAddress2(impp.value);
				break;
			case 2:
				msc.setIMAddress3(impp.value);
				break;
			}
			if (i > 2) {
				break;
			}
		}
	}

	/**
	 * @param vcard
	 * @param msc
	 */
	private void setAddresses(VCard vcard, MSContact msc) {
		List<DeliveryAddressing> addresses = vcard.deliveryAddressing;
		for (DeliveryAddressing da : addresses) {
			if (da.address.containsValues("TYPE", "work")) {
				msc.setBusinessStreet(da.address.streetAddress);
				msc.setBusinessPostalCode(da.address.postalCode);
				msc.setBusinessAddressCity(da.address.locality);
				msc.setBusinessState(da.address.countryName);
				msc.setBusinessAddressCountry(da.address.countryName);
			} else if (da.address.containsValues("TYPE", "home")) {
				msc.setHomeAddressStreet(da.address.streetAddress);
				msc.setHomeAddressPostalCode(da.address.postalCode);
				msc.setHomeAddressCity(da.address.locality);
				msc.setHomeAddressState(da.address.countryName);
				msc.setHomeAddressCountry(da.address.countryName);
			} else {
				msc.setOtherAddressStreet(da.address.streetAddress);
				msc.setOtherAddressPostalCode(da.address.postalCode);
				msc.setOtherAddressCity(da.address.locality);
				msc.setOtherAddressState(da.address.countryName);
				msc.setOtherAddressCountry(da.address.countryName);
			}

		}
	}

	/**
	 * @param vcard
	 * @param msc
	 */
	private void setPhones(VCard vcard, MSContact msc) {
		String type = "TYPE";
		List<Tel> tels = vcard.communications.tels;
		for (Tel tel : tels) {
			String value = tel.value;
			if (value.trim().isEmpty()) {
				continue;
			}

			if (tel.containsValues(type, "cell", "voice") || tel.containsUniqueValue(type, "cell")) {
				msc.setMobilePhoneNumber(value);
			} else if (tel.containsValues(type, "work", "voice") || tel.containsUniqueValue(type, "work")) {
				if (msc.getBusinessPhoneNumber() == null) {
					msc.setBusinessPhoneNumber(value);
				} else if (msc.getBusiness2PhoneNumber() == null) {
					msc.setBusiness2PhoneNumber(value);
				} else {
					logger.info("business phone number {} ({}) cannot get translated to mscontact", value,
							tel.parameters);
				}
			} else if (tel.containsValues(type, "home", "voice") || tel.containsUniqueValue(type, "home")) {
				if (msc.getHomePhoneNumber() == null) {
					msc.setHomePhoneNumber(value);
				} else if (msc.getHome2PhoneNumber() == null) {
					msc.setHome2PhoneNumber(value);
				} else {
					logger.info("home phone number {} ({}) cannot get translated to mscontact", value, tel.parameters);
				}
			} else if (tel.containsValues(type, "work", "fax")) {
				msc.setBusinessFaxNumber(value);
			} else if (tel.containsValues(type, "home", "fax")) {
				msc.setHomeFaxNumber(value);
			} else if (tel.containsValues(type, "assistant", "voice") || tel.containsUniqueValue(type, "assistant")) {
				msc.setAssistantPhoneNumber(value);
			} else if (tel.containsValues(type, "company", "voice") || tel.containsUniqueValue(type, "company")) {
				msc.setCompanyMainPhone(value);
			} else if (tel.containsValues(type, "car", "voice") || tel.containsUniqueValue(type, "car")) {
				msc.setCarPhoneNumber(value);
			} else if (tel.containsValues(type, "pager", "voice") || tel.containsUniqueValue(type, "pager")) {
				msc.setPagerNumber(value);
			} else if (tel.containsValues(type, "radio", "voice") || tel.containsUniqueValue(type, "radio")) {
				msc.setRadioPhoneNumber(value);
			} else {
				msc.setMobilePhoneNumber(value); // defaults to mobile phone
			}

		}

	}

	/**
	 * @param vcard
	 * @param msc
	 */
	private void setEmails(VCard vcard, MSContact msc) {
		List<net.bluemind.addressbook.api.VCard.Communications.Email> emails = new ArrayList<>(
				vcard.communications.emails);

		int pos = 0;
		for (net.bluemind.addressbook.api.VCard.Communications.Email email : emails) {
			msc.setEmail1Address(email.value);
			emails.remove(email);
			pos++;
			break;
		}

		for (net.bluemind.addressbook.api.VCard.Communications.Email email : emails) {
			switch (pos) {
			case 0:
				msc.setEmail1Address(email.value);
				pos++;
				break;
			case 1:
				msc.setEmail2Address(email.value);
				pos++;
				break;
			case 2:
				msc.setEmail3Address(email.value);
				pos++;
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Device to BM
	 * 
	 * @param c
	 * @return
	 */
	public VCard contact(MSContact c) {
		VCard vcard = new VCard();
		vcard.identification = new VCard.Identification();
		vcard.explanatory = new VCard.Explanatory();
		vcard.deliveryAddressing = new ArrayList<DeliveryAddressing>(3);
		vcard.communications = new VCard.Communications();

		vcard.organizational.title = c.getTitle();

		vcard.related.spouse = c.getSpouse();
		vcard.related.manager = c.getManagerName();
		vcard.related.assistant = c.getAssistantName();

		vcard.identification.birthday = c.getBirthday();
		vcard.identification.anniversary = c.getAnniversary();

		vcard.identification.name = VCard.Identification.Name.create(c.getLastName(), c.getFirstName(),
				c.getMiddleName(), null, c.getSuffix(), Collections.emptyList());
		vcard.identification.nickname = VCard.Identification.Nickname.create(c.getNickName());

		vcard.organizational = VCard.Organizational.create(c.getJobTitle(), null,
				VCard.Organizational.Org.create(c.getCompanyName(), null, c.getDepartment()), Collections.emptyList());

		vcard.communications.tels = new ArrayList<net.bluemind.addressbook.api.VCard.Communications.Tel>();

		addPhone(vcard, c.getHomePhoneNumber(), "home", "voice");
		addPhone(vcard, c.getHome2PhoneNumber(), "home", "voice");
		addPhone(vcard, c.getBusinessPhoneNumber(), "work", "voice");
		addPhone(vcard, c.getBusiness2PhoneNumber(), "work", "voice");
		addPhone(vcard, c.getMobilePhoneNumber(), "cell", "voice");
		addPhone(vcard, c.getPagerNumber(), "pager", "voice");

		addPhone(vcard, c.getRadioPhoneNumber(), "voice", "radio");
		addPhone(vcard, c.getAssistantPhoneNumber(), "voice", "assistant");
		addPhone(vcard, c.getCarPhoneNumber(), "voice", "car");
		addPhone(vcard, c.getCompanyMainPhone(), "voice", "company");

		addPhone(vcard, c.getBusinessFaxNumber(), "work", "fax");
		addPhone(vcard, c.getHomeFaxNumber(), "home", "fax");

		vcard.communications.emails = new ArrayList<net.bluemind.addressbook.api.VCard.Communications.Email>(3);

		addEmail(vcard, c.getEmail1Address(), true);
		addEmail(vcard, c.getEmail2Address(), false);
		addEmail(vcard, c.getEmail3Address(), false);

		vcard.deliveryAddressing = new ArrayList<net.bluemind.addressbook.api.VCard.DeliveryAddressing>(3);

		addAddress(vcard, c.getBusinessStreet(), c.getBusinessPostalCode(), c.getBusinessAddressCity(),
				c.getBusinessAddressCountry(), c.getBusinessState(), Arrays.asList("work"));

		addAddress(vcard, c.getHomeAddressStreet(), c.getHomeAddressPostalCode(), c.getHomeAddressCity(),
				c.getHomeAddressCountry(), c.getHomeAddressState(), Arrays.asList("home"));

		addAddress(vcard, c.getOtherAddressStreet(), c.getOtherAddressPostalCode(), c.getOtherAddressCity(),
				c.getOtherAddressCountry(), c.getOtherAddressState(), Collections.<String>emptyList());

		vcard.communications.impps = new ArrayList<net.bluemind.addressbook.api.VCard.Communications.Impp>(3);
		addIM(vcard, c.getIMAddress());
		addIM(vcard, c.getIMAddress2());
		addIM(vcard, c.getIMAddress3());

		vcard.explanatory.urls = new ArrayList<net.bluemind.addressbook.api.VCard.Explanatory.Url>(1);

		addWebsite(vcard, c.getWebPage());

		vcard.explanatory.note = c.getData();

		vcard.identification.photo = null != c.getPicture();

		return vcard;
	}

	/**
	 * @param vcard
	 * @param label
	 * @param value
	 */
	private void addWebsite(VCard vcard, String value) {
		if (StringUtils.isNotBlank(value)) {
			Url url = VCard.Explanatory.Url.create(value, Collections.emptyList());
			vcard.explanatory.urls.add(url);
		}
	}

	/**
	 * @param vcard
	 * @param label
	 * @param value
	 */
	private void addIM(VCard vcard, String value) {
		if (StringUtils.isNotBlank(value)) {
			VCard.Communications.Impp impp = VCard.Communications.Impp.create(value,
					Collections.<Parameter>emptyList());
			vcard.communications.impps.add(impp);
		}
	}

	/**
	 * @param vcard
	 * @param lbl
	 * @param street
	 * @param postalCode
	 * @param city
	 * @param country
	 * @param state
	 */
	private void addAddress(VCard vcard, String street, String postalCode, String city, String country, String state,
			List<String> labels) {
		if (StringUtils.isNotBlank(street) || StringUtils.isNotBlank(postalCode) || StringUtils.isNotBlank(city)
				|| StringUtils.isNotBlank(country) || StringUtils.isNotBlank(state)) {

			ArrayList<Parameter> parameterList = new ArrayList<>();
			for (String label : labels) {
				parameterList.add(Parameter.create("TYPE", label));
			}

			VCard.DeliveryAddressing.Address a = VCard.DeliveryAddressing.Address.create(null, "", "", street, city,
					state, postalCode, country, parameterList);
			DeliveryAddressing da = new DeliveryAddressing();
			da.address = a;
			vcard.deliveryAddressing.add(da);
		}
	}

	/**
	 * @param vcard
	 * @param label
	 * @param value
	 */
	private void addEmail(VCard vcard, String value, boolean isFirst) {
		if (null == value) {
			return;
		}
		net.bluemind.addressbook.api.VCard.Communications.Email e = VCard.Communications.Email.create(value,
				Collections.<Parameter>emptyList());

		if (isFirst) {
			e.parameters = Arrays.asList(Parameter.create("DEFAULT", "true"), Parameter.create("TYPE", "work"));
		} else {
			e.parameters = Arrays.asList(Parameter.create("TYPE", "work"));
		}
		vcard.communications.emails.add(e);
	}

	/**
	 * @param vcard
	 * @param label
	 * @param msPhone
	 */
	private void addPhone(VCard vcard, String msPhone, String... labels) {
		if (!Strings.isNullOrEmpty(msPhone)) {
			List<Parameter> labelParameters = new ArrayList<>();
			for (String label : labels) {
				labelParameters.add(Parameter.create("TYPE", label));
			}
			Tel t = VCard.Communications.Tel.create(msPhone, labelParameters);
			vcard.communications.tels.add(t);
		}
	}

	public SearchResult convertToSearchResult(ItemValue<VCard> item, SearchRequest.Store.Options.Picture picture,
			IAddressBook service) {

		GAL res = new GAL();
		res.firstname = item.value.identification.name.givenNames;
		res.lastname = item.value.identification.name.familyNames;

		Joiner joiner = Joiner.on(" ").skipNulls();
		String dn = joiner.join(item.value.identification.name.givenNames,
				item.value.identification.name.additionalNames, item.value.identification.name.familyNames);
		res.setDisplayName(dn.trim());

		res.alias = item.value.identification.nickname.value;
		res.company = item.value.organizational.org.company;
		res.office = item.value.organizational.org.division;
		res.title = item.value.organizational.title;
		res.emailAddress = item.value.defaultMail();
		res.homePhone = phone(item.value.communications.tels, "home", "voice");
		res.mobilePhone = phone(item.value.communications.tels, "cell", "voice");
		res.phone = phone(item.value.communications.tels, "work", "voice");

		if (picture != null) {
			// BM-7273 force no photo because of iOS crash
			res.picture = new GAL.Picture();
			res.picture.status = GAL.Picture.Status.NoPhoto;

			// res.picture = new GAL.Picture();
			// // FIXME
			// if (item.value.identification.photo) {
			// try {
			// byte[] photo = service.getPhoto(item.uid);
			// if (photo != null) {
			// String b64 = Base64.getEncoder().encodeToString(photo);
			// if (picture.maxSize != null && b64.getBytes().length >
			// picture.maxSize) {
			// res.picture.status = GAL.Picture.Status.MaxSizeExceeded;
			// } else {
			// res.picture.status = GAL.Picture.Status.Success;
			// res.picture.data = b64;
			// }
			// } else {
			// res.picture.status = GAL.Picture.Status.NoPhoto;
			// }
			// } catch (Exception e) {
			// logger.warn("Cannot load contaxt photo of uid {}:{}", item.uid,
			// e.getMessage());
			// res.picture.status = GAL.Picture.Status.NoPhoto;
			// }
			//
			// } else {
			// res.picture.status = GAL.Picture.Status.NoPhoto;
			// }
		}

		if (res.getDisplayName() == null) {
			return null;
		}

		SearchResult sr = new SearchResult();
		sr.searchProperties.gal = res;
		return sr;
	}

	private String phone(List<Tel> tels, String... labels) {
		for (Tel tel : tels) {
			if (tel.containsValues("TYPE", labels)) {
				return tel.value;
			}
		}
		return null;

	}

	public void mergeEmails(VCard newVCard, VCard oldVCard) {
		newVCard.communications.emails.forEach(email -> {
			oldVCard.communications.emails.forEach(oldEmail -> {
				if (email.value.equals(oldEmail.value)) {
					email.parameters = oldEmail.parameters;
				}
			});
		});
	}
}
