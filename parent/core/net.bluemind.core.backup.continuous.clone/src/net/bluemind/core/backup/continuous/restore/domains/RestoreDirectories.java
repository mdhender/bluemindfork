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

public class RestoreDirectories {

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

	public void restore(DataElement de) {
		Map<String, ItemValue<Domain>> domains = new HashMap<>();

		monitor.begin(1, "one to process");

		String jsString = new String(de.payload);
		JsonObject parsed = new JsonObject(jsString);
		JsDirEntry js = new JsDirEntry();
		js.domainUid = de.key.uid;
		js.parsed = parsed;
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
				break;
			case CALENDAR:
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
				break;
			default:
				// OK
				break;
			}
		} else {
			Kind kind = js.kind;

			switch (kind) {
			case USER:
				ItemValue<FullDirEntry<User>> user = dirUserReader.read(js.jsString);
				if (!user.value.value.system) {
					IUser userApi = target.instance(IUser.class, domain.uid);
					processUser(domain, monitor, userApi, user);
				}
				break;
			case GROUP:
				IGroup groupApi = target.instance(IGroup.class, domain.uid);
				ItemValue<FullDirEntry<Group>> group = dirGroupReader.read(js.jsString);
				// user & admin group have a generated uid
				ItemValue<Group> existing = groupApi.byName(group.value.value.name);
				if (existing != null) {
					monitor.log("Update group " + group.value.value);
					groupApi.update(existing.uid, group.value.value);
				} else {
					monitor.log("Create group " + group);

					if (group.externalId != null) {
						groupApi.createWithExtId(group.uid, group.externalId, group.value.value);
					} else {
						groupApi.create(group.uid, group.value.value);
					}
				}
				break;
			case MAILSHARE:
				ItemValue<FullDirEntry<Mailshare>> share = dirMailshareReader.read(js.jsString);
				if (!share.value.value.system) {
					IMailshare shareApi = target.instance(IMailshare.class, domain.uid);
					processMailshare(domain, monitor, shareApi, share);
				}
				break;
			case RESOURCE:
				ItemValue<FullDirEntry<ResourceDescriptor>> res = dirResourceReader.read(js.jsString);
				if (!res.value.value.system) {
					IResources resApi = target.instance(IResources.class, domain.uid);

					ResourceDescriptor existingres = resApi.get(res.uid);
					IResourceTypes typeApi = target.instance(IResourceTypes.class, domain.uid);
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

					if (existingres != null) {
						monitor.log("Update resource " + res.value.value);
						resApi.update(res.uid, res.value.value);
					} else {
						monitor.log("Create resource " + res.value.value);
						resApi.create(res.uid, res.value.value);
					}
				}
				break;
			case EXTERNALUSER:
				ItemValue<FullDirEntry<ExternalUser>> ext = dirExtUReader.read(js.jsString);
				IExternalUser extApi = target.instance(IExternalUser.class, domain.uid);

				ItemValue<ExternalUser> existingExt = extApi.getComplete(ext.uid);
				if (existingExt != null) {
					monitor.log("Update external-user " + ext.value.value);
					extApi.update(ext.uid, ext.value.value);
				} else {
					monitor.log("Create external-user " + ext.value.value);
					if (ext.externalId != null) {
						extApi.createWithExtId(ext.uid, ext.externalId, ext.value.value);
					} else {
						extApi.create(ext.uid, ext.value.value);
					}
				}
				break;
			default:
				System.err.println("Not supported kind " + kind + " yet");
			}
		}
	}

	private void processUser(ItemValue<Domain> dom, IServerTaskMonitor monitor, IUser userApi,
			ItemValue<FullDirEntry<User>> user) {
		System.err.println(user.value.value.login + " hash: " + user.value.value.password);
		ItemValue<User> existing = userApi.getComplete(user.uid);
		ItemValue<Mailbox> mbox = ItemValue.create(user.uid, user.value.mailbox);
		state.storeMailbox(user.uid, mbox);
		ItemValue<User> userItem = ItemValue.create(user.item(), user.value.value);
		if (existing != null) {
			monitor.log("Update user " + user.value.value);
			userApi.updateWithItem(user.uid, userItem);
		} else {
			monitor.log("Create user " + user.value.value);
			userApi.createWithItem(user.uid, userItem);
		}

	}

	private void processMailshare(ItemValue<Domain> domain, IServerTaskMonitor monitor, IMailshare shareApi,
			ItemValue<FullDirEntry<Mailshare>> share) {
		ItemValue<Mailshare> existingShare = shareApi.getComplete(share.uid);
		if (existingShare != null) {
			monitor.log("Update mailshare " + share.value.value);
			shareApi.update(share.uid, share.value.value);
		} else {
			monitor.log("Create mailshare " + share.value.value);

			ItemValue<Mailbox> mbox = ItemValue.create(share.uid, share.value.mailbox);
			state.storeMailbox(share.uid, mbox);
			shareApi.create(share.uid, share.value.value);
		}
	}

	private static class JsDirEntry {
		String domainUid;
		String jsString;
		JsonObject parsed;
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
