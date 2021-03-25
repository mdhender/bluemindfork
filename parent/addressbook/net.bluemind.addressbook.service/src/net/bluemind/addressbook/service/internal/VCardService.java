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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.adapter.VCardAdapter;
import net.bluemind.addressbook.adapter.VCardVersion;
import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.core.api.ImportStats;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.ical4j.vcard.Builder;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.VCardBuilder;
import net.fortuna.ical4j.vcard.property.Photo;
import net.fortuna.ical4j.vcard.property.Uid;

public class VCardService implements IVCardService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VCardService.class);

	private AddressBookService addressbookService;
	private Container container;
	private BmContext context;

	private RBACManager rbacManager;

	public VCardService(BmContext context, AddressBookService service, Container container) {
		this.context = context;
		this.addressbookService = service;
		this.container = container;
		rbacManager = RBACManager.forContext(context).forContainer(container);

	}

	@Override
	public String exportAll() throws ServerFault {
		// acl checked in addressbookService

		StringBuilder sb = new StringBuilder();
		List<ItemValue<VCard>> cards = addressbookService.all();

		for (ItemValue<VCard> vcard : cards) {
			sb.append(adaptCard(vcard).toString());
		}
		return sb.toString();
	}

	@Override
	public String exportCards(List<String> uids) throws ServerFault {
		// acl checked in addressbookService

		StringBuilder sb = new StringBuilder();
		List<ItemValue<VCard>> vcards = addressbookService.multipleGet(uids);

		for (ItemValue<VCard> vcard : vcards) {
			if (vcard != null) {
				sb.append(adaptCard(vcard).toString());
			}
		}

		return sb.toString();
	}

	private net.fortuna.ical4j.vcard.VCard adaptCard(ItemValue<VCard> vcard) {
		net.fortuna.ical4j.vcard.VCard ret = VCardAdapter.adaptCard(container.uid, vcard.value, VCardVersion.v3);
		try {
			Uid cardUid = new Uid(new LinkedList<Parameter>(), container.uid + "," + vcard.uid);
			ret.getProperties().add(cardUid);
		} catch (URISyntaxException e) {
			LOGGER.error(e.getMessage(), e);
		}

		if (vcard.value.identification.photo) {
			try {
				byte[] photo = addressbookService.getPhoto(vcard.uid);
				if (photo != null) {
					ret.getProperties().add(new Photo(photo));
				}
			} catch (ServerFault e) {
				LOGGER.warn(e.getMessage(), e);
			}
		}

		return ret;
	}

	@Override
	public TaskRef importCards(final String vcard) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		return context.provider().instance(ITasksManager.class).run(new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				ImportStats res = importCards(vcard, monitor);
				LOGGER.info("{}/{} vcards imported in {}", res.importedCount(), res.total, container.uid);
			}
		});
	}

	@Override
	public ImportStats directImportCards(String vcard) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		try {
			return importCards(vcard, new NullTaskMonitor());
		} catch (IOException | ParserException e) {
			throw new ServerFault(e);
		}
	}

	private ImportStats importCards(String vcard, IServerTaskMonitor monitor)
			throws ServerFault, IOException, ParserException {
		monitor.begin(3, "Begin import");
		BufferedReader br = new BufferedReader(new StringReader(vcard));
		String line = null;
		StringBuilder sb = new StringBuilder(vcard.length());

		while ((line = br.readLine()) != null) {

			// Yahoo! Crap vcard workaround.
			// SOURCE:Yahoo! AddressBook (http://address.yahoo.com) => invalid
			// see http://tools.ietf.org/html/rfc2425#section-6.1
			//
			// REV;CHARSET=utf-8:53 => invalid
			// see http://tools.ietf.org/html/rfc6350#section-6.7.4
			if (line.startsWith("SOURCE") || line.startsWith("REV")) {
				continue;
			}
			sb.append(line);
			sb.append("\r\n");
		}
		VCardBuilder builder = Builder.from(new BufferedReader(new StringReader(sb.toString())));

		List<net.fortuna.ical4j.vcard.VCard> cards = builder.buildAll();
		List<ItemValue<VCard>> bmCards = new ArrayList<>(cards.size());

		String seed = "" + System.currentTimeMillis();
		for (net.fortuna.ical4j.vcard.VCard card : cards) {
			bmCards.add(
					VCardAdapter.adaptCard(card, s -> UUID.nameUUIDFromBytes(seed.concat(s).getBytes()).toString()));
		}
		bmCards.forEach(this::resolveCategories);
		monitor.progress(1, "Parsed " + bmCards.size() + " cards ");
		ImportStats ret = doImport(bmCards, monitor.subWork(2));
		monitor.end(true, ret.importedCount() + "/" + ret.total + " vcards imported in " + container.uid,
				JsonUtils.asString(ret));
		return ret;
	}

	private void resolveCategories(ItemValue<VCard> card) {
		String tagsContainerUid = "tags_" + container.owner;
		String tagsDomainContainerUid = "tags_" + container.domainUid;

		card.value.explanatory.categories.forEach(tag -> {
			boolean tagPresent = searchAndSetTag(tagsContainerUid, tag.label, tag);
			if (!tagPresent) {
				tagPresent = searchAndSetTag(tagsDomainContainerUid, tag.label, tag);
				if (!tagPresent) {
					ITags tagService = context.su().provider().instance(ITags.class, tagsContainerUid);
					String uid = UUID.randomUUID().toString();
					Tag t = Tag.create(tag.label, tag.color);
					tagService.create(uid, t);
					ItemValue<Tag> createdTag = tagService.getComplete(uid);
					tag.containerUid = tagsContainerUid;
					tag.itemUid = createdTag.uid;
				}
			}
		});
	}

	private boolean searchAndSetTag(String tagsContainer, String label, TagRef ref) {
		if (tagsContainer.equals("tags_global.virt")) {
			return false;
		}
		ITags tagService = context.su().provider().instance(ITags.class, tagsContainer);
		Optional<ItemValue<Tag>> storedTag = tagService.all().stream().filter(t -> t.value.label.equals(label))
				.findFirst();
		if (storedTag.isPresent()) {
			ref.containerUid = tagsContainer;
			ref.itemUid = storedTag.get().uid;
			return true;
		}
		return false;
	}

	private ImportStats doImport(List<ItemValue<VCard>> bmCards, IServerTaskMonitor monitor) throws ServerFault {

		monitor.begin(bmCards.size(), "Import " + bmCards.size() + " cards");

		ImportStats ret = new ImportStats();
		ret.total = bmCards.size();
		ret.uids = new ArrayList<>(ret.total);

		List<ItemValue<VCard>> toImport = new ArrayList<>(bmCards);

		Iterator<ItemValue<VCard>> it = toImport.iterator();
		VCard card;
		while (it.hasNext()) {
			VCardChanges changes = VCardChanges.create(new ArrayList<>(bmCards.size()), Collections.emptyList(),
					Collections.emptyList());

			while (it.hasNext() && changes.add.size() < 50) {
				ItemValue<VCard> next = it.next();
				card = next.value;
				String uid = next.uid;
				changes.add.add(VCardChanges.ItemAdd.create(uid, card));

			}
			ContainerUpdatesResult resp = addressbookService.updates(changes);
			ret.uids.addAll(resp.added);
			monitor.progress(changes.add.size(), "in progress");
		}
		return ret;
	}

}
