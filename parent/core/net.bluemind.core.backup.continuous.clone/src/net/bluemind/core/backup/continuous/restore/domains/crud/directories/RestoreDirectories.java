package net.bluemind.core.backup.continuous.restore.domains.crud.directories;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.Seppuku;
import net.bluemind.core.backup.continuous.restore.IClonePhaseObserver;
import net.bluemind.core.backup.continuous.restore.ISeppukuAckListener;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.backup.continuous.restore.domains.crud.AbstractCrudRestore;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.domain.api.Domain;
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
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserSettings;

public class RestoreDirectories implements RestoreDomainType {

	private final ValueReader<ItemValue<UserSettings>> userSettingsReader = JsonUtils
			.reader(new TypeReference<ItemValue<UserSettings>>() {
			});

	private final ValueReader<ItemValue<DirEntry>> rawEntryReader = JsonUtils
			.reader(new TypeReference<ItemValue<DirEntry>>() {
			});

	private final ValueReader<ItemValue<Seppuku>> byeReader = JsonUtils.reader(new TypeReference<ItemValue<Seppuku>>() {
	});

	private final RestoreLogger log;
	private final IServiceProvider target;
	private final List<IClonePhaseObserver> observers;
	private final RestoreState state;

	private final ISeppukuAckListener byeAck;

	private final DomainCalendarCrudRestore domainCalendarRestore;
	private final DomainAddressBookCrudRestore domainAddressBookRestore;

	private final ExternalUserCrudRestore externalUserRestore;
	private final ResourceCrudRestore resourceRestore;
	private final MailshareCrudRestore mailshareRestore;
	private final GroupCrudRestore groupRestore;
	private final UserCrudRestore userRestore;
	private final OrgUnitCrudRestore orgUnitRestore;

	public RestoreDirectories(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target,
			List<IClonePhaseObserver> observers, ISeppukuAckListener byeAck, RestoreState state) {
		this.log = log;
		this.target = target;
		this.observers = observers;
		this.byeAck = byeAck;
		this.state = state;
		this.domainCalendarRestore = new DomainCalendarCrudRestore(log, domain);
		this.domainAddressBookRestore = new DomainAddressBookCrudRestore(log, domain);
		this.externalUserRestore = new ExternalUserCrudRestore(log, domain);
		this.resourceRestore = new ResourceCrudRestore(log, domain);
		this.mailshareRestore = new MailshareCrudRestore(log, domain);
		this.groupRestore = new GroupCrudRestore(log, domain);
		this.userRestore = new UserCrudRestore(log, domain);
		this.orgUnitRestore = new OrgUnitCrudRestore(log, domain);
	}

	@Override
	public String type() {
		return "dir";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		if ("net.bluemind.core.backup.continuous.events.BubbleEventsVerticle.FullDirEntry".equals(key.valueClass)) {
			log.filter(type(), key);
			return;
		}

		if ("net.bluemind.core.backup.continuous.dto.Seppuku".equals(key.valueClass)) {
			log.seppuku(type(), key);
			ItemValue<Seppuku> bye = byeReader.read(payload);
			byeAck.onSeppukuAck(bye.value);
			return;
		} else if ("net.bluemind.user.api.UserSettings".equals(key.valueClass)) {
			log.set(type(), "UserSettings", key);
			ItemValue<UserSettings> settings = userSettingsReader.read(payload);
			IUserSettings setApi = target.instance(IUserSettings.class, key.uid);
			setApi.set(settings.uid, settings.value.values);
			return;
		}

		observers.forEach(obs -> obs.beforeMailboxesPopulate(log.monitor()));

		processEntry(log.subWork(1), key, payload);

	}

	private void processEntry(RestoreLogger log, RecordKey key, String payload) {

		JsonObject parsed = new JsonObject(payload);
		Kind kind;
		if (parsed.getJsonObject("value").containsKey("entry")) {
			kind = Kind.valueOf(parsed.getJsonObject("value").getJsonObject("entry").getString("kind"));
		} else {
			ItemValue<DirEntry> entry = rawEntryReader.read(payload);
			kind = entry.value.kind;
		}

		switch (kind) {
		case DOMAIN:
			log.filter(type(), kind.name(), key);
			break;
		case ADDRESSBOOK:
			domainAddressBookRestore.restore(key, payload);
			break;
		case CALENDAR:
			domainCalendarRestore.restore(key, payload);
			break;
		case USER:
			userRestore.restore(key, payload);
			break;
		case GROUP:
			groupRestore.restore(key, payload);
			break;
		case MAILSHARE:
			mailshareRestore.restore(key, payload);
			break;
		case RESOURCE:
			resourceRestore.restore(key, payload);
			break;
		case EXTERNALUSER:
			externalUserRestore.restore(key, payload);
			break;
		case ORG_UNIT:
			orgUnitRestore.restore(key, payload);
			break;
		default:
			log.skip(type(), kind.name(), key, payload);
		}
	}

	private class DomainAddressBookCrudRestore extends AbstractCrudRestore<DirEntry, AddressBookDescriptor, IAddressBooksMgmt> {

		private final ValueReader<ItemValue<DirEntry>> reader = JsonUtils
				.reader(new TypeReference<ItemValue<DirEntry>>() {
				});

		private DomainAddressBookCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return Kind.ADDRESSBOOK.name();
		}

		@Override
		protected ValueReader<ItemValue<DirEntry>> reader() {
			return reader;
		}

		@Override
		protected IAddressBooksMgmt api(ItemValue<Domain> domain, RecordKey key) {
			return api();
		}

		protected IAddressBooksMgmt api() {
			return target.instance(IAddressBooksMgmt.class);
		}

		@Override
		protected boolean filter(RecordKey key, ItemValue<DirEntry> item) {
			return item.uid.equals("addressbook_" + domain.uid);
		}

		@Override
		protected ItemValue<AddressBookDescriptor> map(ItemValue<DirEntry> item, boolean isCreate) {
			AddressBookDescriptor bookDesc = new AddressBookDescriptor();
			bookDesc.owner = domain.uid;
			if (!isCreate) {
				AddressBookDescriptor existing = api().getComplete(item.uid);
				bookDesc.owner = existing.owner;
			}
			bookDesc.domainUid = domain.uid;
			bookDesc.name = item.displayName;
			bookDesc.orgUnitUid = item.value.orgUnitUid;
			return ItemValue.create(item.item(), bookDesc);
		}

		@Override
		protected void delete(IAddressBooksMgmt api, RecordKey key, String uid) {
			api.delete(uid);
		}
	}

	private class DomainCalendarCrudRestore extends AbstractCrudRestore<DirEntry, CalendarDescriptor, ICalendarsMgmt> {

		private final ValueReader<ItemValue<DirEntry>> reader = JsonUtils
				.reader(new TypeReference<ItemValue<DirEntry>>() {
				});

		private DomainCalendarCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return Kind.CALENDAR.name();
		}

		@Override
		protected ValueReader<ItemValue<DirEntry>> reader() {
			return reader;
		}

		protected ICalendarsMgmt api() {
			return target.instance(ICalendarsMgmt.class);
		}

		@Override
		protected ICalendarsMgmt api(ItemValue<Domain> domain, RecordKey key) {
			return api();
		}

		@Override
		protected ItemValue<CalendarDescriptor> map(ItemValue<DirEntry> item, boolean isCreate) {
			CalendarDescriptor calDesc = new CalendarDescriptor();
			calDesc.owner = domain.uid;
			if (!isCreate) {
				CalendarDescriptor existing = api().getComplete(item.uid);
				calDesc.owner = existing.owner;
			}
			calDesc.domainUid = domain.uid;
			calDesc.name = item.displayName;
			calDesc.orgUnitUid = item.value.orgUnitUid;
			return ItemValue.create(item.item(), calDesc);
		}

		@Override
		protected void delete(ICalendarsMgmt api, RecordKey key, String uid) {
			api.delete(uid);
		}
	}

	private class ExternalUserCrudRestore extends CrudDirEntryRestore.WithoutMailbox<ExternalUser> {

		private final ValueReader<ItemValue<FullDirEntry<ExternalUser>>> reader = JsonUtils
				.reader(new TypeReference<ItemValue<FullDirEntry<ExternalUser>>>() {
				});

		private ExternalUserCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return Kind.EXTERNALUSER.name();
		}

		@Override
		protected ValueReader<ItemValue<FullDirEntry<ExternalUser>>> reader() {
			return reader;
		}

		@Override
		protected IExternalUser api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IExternalUser.class, domain.uid);
		}
	}

	private class ResourceCrudRestore extends CrudDirEntryRestore.WithMailbox<ResourceDescriptor> {

		private final ValueReader<ItemValue<FullDirEntry<ResourceDescriptor>>> reader = JsonUtils
				.reader(new TypeReference<ItemValue<FullDirEntry<ResourceDescriptor>>>() {
				});

		private ResourceCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return Kind.RESOURCE.name();
		}

		@Override
		protected ValueReader<ItemValue<FullDirEntry<ResourceDescriptor>>> reader() {
			return reader;
		}

		@Override
		protected IResources api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IResources.class, domain.uid);
		}

		@Override
		protected void create(IRestoreDirEntryWithMailboxSupport<ResourceDescriptor> api, RecordKey key,
				ItemValue<FullDirEntry<ResourceDescriptor>> item) {
			createFakeResourceTypesIfNotExists(domain.uid, key, item);
			super.create(api, key, item);
		}

		@Override
		protected void update(IRestoreDirEntryWithMailboxSupport<ResourceDescriptor> api, RecordKey key,
				ItemValue<FullDirEntry<ResourceDescriptor>> item) {
			createFakeResourceTypesIfNotExists(domain.uid, key, item);
			super.update(api, key, item);
		}

		private void createFakeResourceTypesIfNotExists(String domainUid, RecordKey key,
				ItemValue<FullDirEntry<ResourceDescriptor>> item) {
			IResourceTypes typeApi = target.instance(IResourceTypes.class, domainUid);
			String wantedType = item.value.value.typeIdentifier;
			ResourceTypeDescriptor knownType = typeApi.get(wantedType);
			if (knownType == null) {
				ResourceTypeDescriptor rtd = new ResourceTypeDescriptor();
				rtd.label = "Auto-created " + wantedType;
				rtd.properties = Collections.emptyList();
				rtd.templates = Collections.emptyMap();
				log.createParent(type(), key, wantedType);
				typeApi.create(wantedType, rtd);
			}
		}
	}

	private class MailshareCrudRestore extends CrudDirEntryRestore.WithMailbox<Mailshare> {

		private final ValueReader<ItemValue<FullDirEntry<Mailshare>>> reader = JsonUtils
				.reader(new TypeReference<ItemValue<FullDirEntry<Mailshare>>>() {
				});

		private MailshareCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return Kind.MAILSHARE.name();
		}

		@Override
		protected ValueReader<ItemValue<FullDirEntry<Mailshare>>> reader() {
			return reader;
		}

		@Override
		protected IMailshare api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IMailshare.class, domain.uid);
		}

		@Override
		protected boolean filter(RecordKey key, ItemValue<FullDirEntry<Mailshare>> item) {
			return item.value.value.system;
		}

		@Override
		protected void create(IRestoreDirEntryWithMailboxSupport<Mailshare> api, RecordKey key,
				ItemValue<FullDirEntry<Mailshare>> item) {
			ItemValue<Mailbox> mbox = ItemValue.create(item.uid, item.value.mailbox);
			state.storeMailbox(item.uid, mbox);
			super.create(api, key, item);
		}
	}

	private class GroupCrudRestore extends CrudDirEntryRestore.WithMailbox<Group> {

		private final ValueReader<ItemValue<FullDirEntry<Group>>> reader = JsonUtils
				.reader(new TypeReference<ItemValue<FullDirEntry<Group>>>() {
				});

		private GroupCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return Kind.GROUP.name();
		}

		@Override
		protected ValueReader<ItemValue<FullDirEntry<Group>>> reader() {
			return reader;
		}

		@Override
		protected IGroup api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IGroup.class, domain.uid);
		}
	}

	private class UserCrudRestore extends CrudDirEntryRestore.WithMailbox<User> {

		private final ValueReader<ItemValue<FullDirEntry<User>>> reader = JsonUtils
				.reader(new TypeReference<ItemValue<FullDirEntry<User>>>() {
				});

		private UserCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return Kind.USER.name();
		}

		@Override
		protected ValueReader<ItemValue<FullDirEntry<User>>> reader() {
			return reader;
		}

		@Override
		protected IUser api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IUser.class, domain.uid);
		}

		@Override
		protected void create(IRestoreDirEntryWithMailboxSupport<User> api, RecordKey key,
				ItemValue<FullDirEntry<User>> item) {
			ItemValue<Mailbox> mbox = ItemValue.create(item.uid, item.value.mailbox);
			state.storeMailbox(item.uid, mbox);
			super.create(api, key, item);
		}
	}

	private class OrgUnitCrudRestore extends CrudDirEntryRestore.WithoutMailbox<OrgUnit> {

		private final ValueReader<ItemValue<FullDirEntry<OrgUnit>>> reader = JsonUtils
				.reader(new TypeReference<ItemValue<FullDirEntry<OrgUnit>>>() {
				});

		private OrgUnitCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return Kind.ORG_UNIT.name();
		}

		@Override
		protected ValueReader<ItemValue<FullDirEntry<OrgUnit>>> reader() {
			return reader;
		}

		@Override
		protected IOrgUnits api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IOrgUnits.class, domain.uid);
		}
	}
}
