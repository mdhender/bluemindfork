/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.container.model.ItemValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "deduplicate", description = "Deduplicate an addressbook")
public class DeduplicateAddressBookCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("contact");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return DeduplicateAddressBookCommand.class;
		}
	}

	@Parameters(paramLabel = "<email>", description = "email address")
	public String email;

	@Option(names = "--addressbook-uid", description = "the addressbook uid. Default value: CollectedContacts addressbook")
	public String addressBookUid;

	@Option(names = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	private CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		if (!Regex.EMAIL.validate(email)) {
			throw new CliException("Invalid email : " + email);
		}

		String domain = cliUtils.getDomainUidFromEmailOrDomain(email);
		String userUid = cliUtils.getUserUidFromEmail(email);

		if (addressBookUid == null) {
			addressBookUid = IAddressBookUids.collectedContactsUserAddressbook(userUid);
		}

		ctx.info("deduplicate addressbook : " + addressBookUid);

		deduplicateEntries(addressBookUid, domain);

	}

	private void deduplicateEntries(String addressBookUid, String domain) {
		IAddressBook addressBook = ctx.adminApi().instance(IAddressBook.class, addressBookUid);
		List<String> domainEmails = getAllEmailsInDomainAddressBook(domain);
		List<String> contactsUids = addressBook.allUids();
		int contactsUidsSize = contactsUids.size();
		int deletedContactsCounter = 0;

		List<ItemValue<VCard>> allVcards = new ArrayList<>();
		for (List<String> subList : Lists.partition(contactsUids, 500)) {
			List<ItemValue<VCard>> slice = addressBook.multipleGet(subList);
			getInternalDuplicatedContacts(slice, domainEmails).forEach(uid -> {
				if (!dry) {
					addressBook.delete(uid);
				} else {
					ctx.info("DRY: delete " + uid);
				}
			});
			allVcards.addAll(slice);
		}
		getExternalDuplicatedContacts(allVcards).forEach(uid -> {
			if (!dry) {
				addressBook.delete(uid);
			} else {
				ctx.info("DRY: delete " + uid);
			}
		});

		deletedContactsCounter = contactsUidsSize - addressBook.allUids().size();
		ctx.info(Integer.toString(deletedContactsCounter) + " were removed out of "
				+ Integer.toString(contactsUidsSize));
	}

	private List<String> getAllEmailsInDomainAddressBook(String domain) {

		IAddressBook domainAddressBook = ctx.adminApi().instance(IAddressBook.class,
				IAddressBookUids.userVCards(domain));
		List<String> allUids = domainAddressBook.allUids();
		List<String> emails = new ArrayList<>();

		for (List<String> subList : Lists.partition(allUids, 500)) {
			List<ItemValue<VCard>> slice = domainAddressBook.multipleGet(subList);
			emails.addAll(getEmails(slice));
		}
		return emails;
	}

	private List<String> getEmails(List<ItemValue<VCard>> list) {
		List<String> emails = new ArrayList<>();
		if (!list.isEmpty()) {
			for (ItemValue<VCard> item : list) {
				if (!item.value.communications.emails.isEmpty()) {
					for (Email mail : item.value.communications.emails) {
						if (mail != null) {
							emails.add(mail.value);
						}
					}
				}
			}
		}
		return emails;
	}

	private List<String> getInternalDuplicatedContacts(List<ItemValue<VCard>> list, List<String> domainEmails) {
		List<String> uids = new ArrayList<>();
		if (!list.isEmpty()) {
			for (ItemValue<VCard> item : list) {
				if (item.value.communications.emails.size() == 1
						&& Strings.isNullOrEmpty(item.value.organizational.role)
						&& item.value.communications.tels.isEmpty()
						&& domainEmails.contains(item.value.communications.emails.get(0).value)) {
					uids.add(item.uid);
				}
			}
		}
		return uids;
	}

	private List<String> getExternalDuplicatedContacts(List<ItemValue<VCard>> list) {

		List<String> uids = new ArrayList<>();
		List<ItemValue<VCard>> duplicatedCards = list.stream().filter(
				vCard -> vCard.value.communications.emails != null && vCard.value.communications.emails.size() == 1)
				.collect(Collectors.toList());

		Map<String, List<ItemValue<VCard>>> groupByEmailsMap = duplicatedCards.stream()
				.collect(Collectors.groupingBy(item -> item.value.communications.emails.get(0).value));

		for (List<ItemValue<VCard>> vCards : groupByEmailsMap.values()) {
			if (vCards.size() != 1) {

				Map<String, List<ItemValue<VCard>>> vCardsByDisplayName = vCards.stream()
						.collect(Collectors.groupingBy(item -> item.displayName));
				for (List<ItemValue<VCard>> displayNames : vCardsByDisplayName.values()) {
					if (displayNames.size() != 1) {
						List<ItemValue<VCard>> finalValue = displayNames.stream()
								.filter(vCard -> vCard.value.communications.tels.isEmpty()
										&& Strings.isNullOrEmpty(vCard.value.organizational.role)
										&& Strings.isNullOrEmpty(vCard.value.organizational.title)
										&& Strings.isNullOrEmpty(vCard.value.explanatory.note)
										&& vCard.value.identification.birthday == null
										&& vCard.value.explanatory.urls.isEmpty())
								.collect(Collectors.toList());
						if (finalValue.size() > 1) {
							uids.addAll(finalValue.subList(1, finalValue.size()).stream().map(item -> item.uid)
									.collect(Collectors.toList()));
						}
					}

				}
			}
		}
		return uids;
	}
}
