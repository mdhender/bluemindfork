package net.bluemind.core.backup.continuous.restore.domains;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.MoreObjects;

import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.IClonePhaseObserver;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class RestoreDirectories implements RestoreDomainType {

	private static final Logger logger = LoggerFactory.getLogger(RestoreDirectories.class);

	private final ValueReader<ItemValue<FullDirEntry<User>>> dirUserReader = JsonUtils
			.reader(new TypeReference<ItemValue<FullDirEntry<User>>>() {
			});
	private final ValueReader<ItemValue<FullDirEntry<Group>>> dirGroupReader = JsonUtils
			.reader(new TypeReference<ItemValue<FullDirEntry<Group>>>() {
			});
	private final ValueReader<ItemValue<FullDirEntry<Mailshare>>> dirMailshareReader = JsonUtils
			.reader(new TypeReference<ItemValue<FullDirEntry<Mailshare>>>() {
			});
	private final ValueReader<ItemValue<FullDirEntry<ResourceDescriptor>>> dirResourceReader = JsonUtils
			.reader(new TypeReference<ItemValue<FullDirEntry<ResourceDescriptor>>>() {
			});
	private final ValueReader<ItemValue<FullDirEntry<ExternalUser>>> dirExtUReader = JsonUtils
			.reader(new TypeReference<ItemValue<FullDirEntry<ExternalUser>>>() {
			});
	private final ValueReader<ItemValue<DirEntry>> rawEntryReader = JsonUtils
			.reader(new TypeReference<ItemValue<DirEntry>>() {
			});

	private final IServerTaskMonitor monitor;
	private final IServiceProvider target;
	private final ArrayList<IClonePhaseObserver> observers;
	private final RestoreState state;

	public RestoreDirectories(IServerTaskMonitor monitor, IServiceProvider target,
			ArrayList<IClonePhaseObserver> observers, RestoreState state) {
		this.monitor = monitor;
		this.target = target;
		this.observers = observers;
		this.state = state;
	}

	public String type() {
		return "dir";
	}

	public void restore(DataElement de) {
		if ("net.bluemind.core.backup.continuous.events.BubbleEventsVerticle.FullDirEntry".equals(de.key.valueClass)) {
			return;
		}
		Map<String, ItemValue<Domain>> domains = new HashMap<>();

		monitor.begin(1, "one to process");

		String jsString = new String(de.payload);
		JsonObject parsed = new JsonObject(jsString);
		JsDirEntry js = new JsDirEntry();
		js.domainUid = de.key.uid;
		if (parsed.getJsonObject("value").containsKey("entry")) {
			js.kind = BaseDirEntry.Kind.valueOf(parsed.getJsonObject("value").getJsonObject("entry").getString("kind"));
		}
		js.jsString = jsString;

		observers.forEach(obs -> obs.beforeMailboxesPopulate(monitor));

		processEntry(monitor.subWork(1), domains, js);

		monitor.end(true, "Finished processing dir entries", "OK");

	}

	private void processEntry(IServerTaskMonitor monitor, Map<String, ItemValue<Domain>> domains, JsDirEntry js) {
		ItemValue<Domain> domain = domains.computeIfAbsent(js.domainUid, uid -> {
			IDomains domApi = target.instance(IDomains.class);
			return domApi.get(uid);
		});
		if (js.kind == null) {
			// should be a domain
			ItemValue<DirEntry> entry = rawEntryReader.read(js.jsString);
			System.err.println("on entry " + entry);
			switch (entry.value.kind) {
			case DOMAIN:
				// skip
				break;
			case ADDRESSBOOK:
				processAddressBook(monitor, entry, domain);
				break;
			case CALENDAR:
				processCalendar(monitor, entry, domain);
				break;
			default:
				// OK
				break;
			}
		} else {
			Kind kind = js.kind;

			switch (kind) {
			case USER:
				processUser(monitor, js);
				break;
			case GROUP:
				processGroup(monitor, js);
				break;
			case MAILSHARE:
				processMailshare(monitor, js);
				break;
			case RESOURCE:
				processResource(monitor, js);
				break;
			case EXTERNALUSER:
				processExternalUser(monitor, js);
				break;
			default:
				System.err.println("Not supported kind " + kind + " yet");
			}
		}
	}

	private void processCalendar(IServerTaskMonitor monitor, ItemValue<DirEntry> entry, ItemValue<Domain> domain) {
		ICalendarsMgmt calApi = target.instance(ICalendarsMgmt.class);

		CalendarDescriptor existing = calApi.getComplete(entry.uid);
		CalendarDescriptor calDesc = new CalendarDescriptor();
		calDesc.domainUid = domain.uid;
		calDesc.owner = domain.uid;
		calDesc.name = entry.displayName;
		calDesc.orgUnitUid = entry.value.orgUnitUid;
		if (existing != null) {
			monitor.log("Update calendar " + calDesc);
			calDesc.owner = existing.owner;
			calApi.update(entry.uid, calDesc);
		} else {
			monitor.log("Create calendar " + calDesc);
			calApi.create(entry.uid, calDesc);
		}
	}

	private void processAddressBook(IServerTaskMonitor monitor, ItemValue<DirEntry> entry, ItemValue<Domain> domain) {
		IAddressBooksMgmt bookApi = target.instance(IAddressBooksMgmt.class);
		if (!entry.uid.equals("addressbook_" + domain.uid)) {
			AddressBookDescriptor existing = bookApi.getComplete(entry.uid);
			AddressBookDescriptor bookDesc = new AddressBookDescriptor();
			bookDesc.owner = domain.uid;
			bookDesc.domainUid = domain.uid;
			bookDesc.name = entry.displayName;
			bookDesc.orgUnitUid = entry.value.orgUnitUid;
			if (existing != null) {
				monitor.log("Update addressbook " + bookDesc);
				bookDesc.owner = existing.owner;
				bookApi.update(entry.uid, bookDesc);
			} else {
				monitor.log("Create addressbook " + bookDesc);
				bookApi.create(entry.uid, bookDesc, false);
			}
		}
	}

	private void processExternalUser(IServerTaskMonitor monitor, JsDirEntry js) {
		ItemValue<FullDirEntry<ExternalUser>> ext = dirExtUReader.read(js.jsString);
		IExternalUser extApi = target.instance(IExternalUser.class, js.domainUid);

		ItemValue<ExternalUser> existingExt = extApi.getComplete(ext.uid);
		ItemValue<ExternalUser> externalUserItem = ItemValue.create(ext.item(), ext.value.value);
		if (existingExt != null) {
			monitor.log("Update external-user " + ext.value.value);
			extApi.updateWithItem(ext.uid, externalUserItem);
		} else {
			monitor.log("Create external-user " + ext.value.value);
			extApi.createWithItem(ext.uid, externalUserItem);
		}
	}

	private void processResource(IServerTaskMonitor monitor, JsDirEntry js) {
		ItemValue<FullDirEntry<ResourceDescriptor>> res = dirResourceReader.read(js.jsString);
		if (res.value.value.system) {
			return;
		}
		IResources resApi = target.instance(IResources.class, js.domainUid);

		ResourceDescriptor existingres = resApi.get(res.uid);
		IResourceTypes typeApi = target.instance(IResourceTypes.class, js.domainUid);
		String wantedType = res.value.value.typeIdentifier;
		ResourceTypeDescriptor knownType = typeApi.get(wantedType);
		if (knownType == null) {
			ResourceTypeDescriptor rtd = new ResourceTypeDescriptor();
			rtd.label = "Auto-created " + wantedType;
			rtd.properties = Collections.emptyList();
			rtd.templates = Collections.emptyMap();
			monitor.log("Auto creating resource type " + rtd);
			typeApi.create(wantedType, rtd);
		}

		ItemValue<ResourceDescriptor> resourceItem = ItemValue.create(res.item(), res.value.value);
		if (existingres != null) {
			monitor.log("Update resource " + res.value.value);
			resApi.updateWithItem(res.uid, resourceItem);
		} else {
			monitor.log("Create resource " + res.value.value);
			resApi.createWithItem(res.uid, resourceItem);
		}
	}

	private void processMailshare(IServerTaskMonitor monitor, JsDirEntry js) {
		ItemValue<FullDirEntry<Mailshare>> share = dirMailshareReader.read(js.jsString);
		if (share.value.value.system) {
			return;
		}
		IMailshare shareApi = target.instance(IMailshare.class, js.domainUid);
		ItemValue<Mailshare> existingShare = shareApi.getComplete(share.uid);
		ItemValue<Mailshare> mailshareItem = ItemValue.create(share.item(), share.value.value);
		if (existingShare != null) {
			monitor.log("Update mailshare " + share.value.value);
			shareApi.updateWithItem(share.uid, mailshareItem);
		} else {
			monitor.log("Create mailshare " + share.value.value);
			ItemValue<Mailbox> mbox = ItemValue.create(share.uid, share.value.mailbox);
			state.storeMailbox(share.uid, mbox);
			shareApi.createWithItem(share.uid, mailshareItem);
		}
	}

	private void processGroup(IServerTaskMonitor monitor, JsDirEntry js) {
		IGroup groupApi = target.instance(IGroup.class, js.domainUid);
		ItemValue<FullDirEntry<Group>> group = dirGroupReader.read(js.jsString);
		// user & admin group have a generated uid
		ItemValue<Group> existing = groupApi.byName(group.value.value.name);
		ItemValue<Group> groupItem = ItemValue.create(group.item(), group.value.value);
		if (existing != null) {
			monitor.log("Update group " + group.value.value);
			groupApi.updateWithItem(group.uid, groupItem);
		} else {
			monitor.log("Create group " + group);
			groupApi.createWithItem(group.uid, groupItem);
		}
	}

	private void processUser(IServerTaskMonitor monitor, JsDirEntry js) {
		ItemValue<FullDirEntry<User>> user = dirUserReader.read(js.jsString);
		if (user.value.value.system) {
			return;
		}
		IUser userApi = target.instance(IUser.class, js.domainUid);
		System.err.println(user.value.value.login + " hash: " + user.value.value.password);
		ItemValue<User> existing = userApi.getComplete(user.uid);
		ItemValue<User> userItem = ItemValue.create(user.item(), user.value.value);
		if (existing != null) {
			monitor.log("Update user " + user.value.value);
			userApi.updateWithItem(user.uid, userItem);
		} else {
			monitor.log("Create user " + user.value.value);
			ItemValue<Mailbox> mbox = ItemValue.create(user.uid, user.value.mailbox);
			state.storeMailbox(user.uid, mbox);
			userApi.createWithItem(user.uid, userItem);
		}
	}

	private static class JsDirEntry {
		String domainUid;
		String jsString;
		Kind kind;
	}

	private static class FullDirEntry<T> {
		public DirEntry entry;
		public VCard vcard;
		public Mailbox mailbox;

		public T value;

		@Override
		public String toString() {
			return MoreObjects.toStringHelper("DE").add("entry", entry).add("vcard", vcard).add("mbox", mailbox)
					.toString();
		}
	}
}
