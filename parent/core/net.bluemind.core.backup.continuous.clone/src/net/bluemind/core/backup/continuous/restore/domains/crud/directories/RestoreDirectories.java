package net.bluemind.core.backup.continuous.restore.domains.crud.directories;

import static net.bluemind.directory.api.BaseDirEntry.Kind.ADDRESSBOOK;
import static net.bluemind.directory.api.BaseDirEntry.Kind.CALENDAR;
import static net.bluemind.directory.api.BaseDirEntry.Kind.DOMAIN;
import static net.bluemind.directory.api.BaseDirEntry.Kind.EXTERNALUSER;
import static net.bluemind.directory.api.BaseDirEntry.Kind.GROUP;
import static net.bluemind.directory.api.BaseDirEntry.Kind.MAILSHARE;
import static net.bluemind.directory.api.BaseDirEntry.Kind.ORG_UNIT;
import static net.bluemind.directory.api.BaseDirEntry.Kind.RESOURCE;
import static net.bluemind.directory.api.BaseDirEntry.Kind.USER;

import java.util.Collections;

import com.fasterxml.jackson.core.type.TypeReference;

import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.Seppuku;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.ISeppukuAckListener;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.backup.continuous.restore.domains.crud.AbstractCrudRestore;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.directory.service.DirEntryHandlers;
import net.bluemind.domain.api.Domain;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.service.IInCoreGroup;
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
	private final RestoreState state;

	private final ISeppukuAckListener byeAck;

	private final DomainDirEntryRestore domainDirEntryRestore;
	private final DomainCalendarCrudRestore domainCalendarRestore;
	private final DomainAddressBookCrudRestore domainAddressBookRestore;

	private final ExternalUserCrudRestore externalUserRestore;
	private final ResourceCrudRestore resourceRestore;
	private final MailshareCrudRestore mailshareRestore;
	private final GroupCrudRestore groupRestore;
	private final UserCrudRestore userRestore;
	private final OrgUnitCrudRestore orgUnitRestore;

	public RestoreDirectories(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target,
			ISeppukuAckListener byeAck, RestoreState state) {
		this.log = log;
		this.target = target;
		this.byeAck = byeAck;
		this.state = state;
		this.domainDirEntryRestore = new DomainDirEntryRestore(log, domain);
		this.domainCalendarRestore = new DomainCalendarCrudRestore(log, domain);
		this.domainAddressBookRestore = new DomainAddressBookCrudRestore(log, domain);
		this.externalUserRestore = new ExternalUserCrudRestore(log, domain);
		this.resourceRestore = new ResourceCrudRestore(log, domain, target);
		this.mailshareRestore = new MailshareCrudRestore(log, domain, target);
		this.groupRestore = new GroupCrudRestore(log, domain, target);
		this.userRestore = new UserCrudRestore(log, domain, target);
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
			domainDirEntryRestore.restore(key, payload);
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

	private class DomainDirEntryRestore implements RestoreDomainType {

		private final ValueReader<VersionnedItem<DirEntry>> reader = JsonUtils
				.reader(new TypeReference<VersionnedItem<DirEntry>>() {
				});

		private RestoreLogger log;
		private ItemValue<Domain> domain;

		private DomainDirEntryRestore(RestoreLogger log, ItemValue<Domain> domain) {
			this.log = log;
			this.domain = domain;
		}

		@Override
		public String type() {
			return DOMAIN.name();
		}

		@Override
		public void restore(RecordKey key, String payload) {
			VersionnedItem<DirEntry> item = reader.read(payload);

			DirEntryHandler dirEntryApi = DirEntryHandlers.byKind(DOMAIN);
			BmContext context = ((ServerSideServiceProvider) target).getContext();
			ItemValue<DirEntry> previous = dirEntryApi.get(context, domain.uid, item.uid);
			if (previous != null && previous.internalId != item.internalId) {
				log.deleteByProduct(type(), key);
				dirEntryApi.delete(context, domain.uid, item.uid);
				log.create(type(), key);
				dirEntryApi.create(context, domain.uid, item);
			} else if (previous != null) {
				log.update(type(), key);
				dirEntryApi.update(context, domain.uid, item);
			}
		}

	}

	private class DomainAddressBookCrudRestore
			extends AbstractCrudRestore<DirEntry, AddressBookDescriptor, IAddressBooksMgmt> {

		private final ValueReader<VersionnedItem<DirEntry>> reader = JsonUtils
				.reader(new TypeReference<VersionnedItem<DirEntry>>() {
				});

		private DomainAddressBookCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return ADDRESSBOOK.name();
		}

		@Override
		protected ValueReader<VersionnedItem<DirEntry>> reader() {
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
		protected boolean exists(IAddressBooksMgmt api, RecordKey key, VersionnedItem<DirEntry> item) {
			// In case of a Domain AddressBook container, the owner is a dir entry of type
			// addressbook.
			DirEntryHandler dirEntryApi = DirEntryHandlers.byKind(ADDRESSBOOK);
			BmContext context = ((ServerSideServiceProvider) target).getContext();
			ItemValue<DirEntry> previous = dirEntryApi.get(context, domain.uid, item.uid);
			if (previous != null && previous.internalId != item.internalId) {
				log.deleteByProduct(type(), key);
				dirEntryApi.delete(context, domain.uid, item.uid);
				log.create(type(), key);
				dirEntryApi.create(context, domain.uid, item);
				return true;
			}
			return previous != null;
		}

		@Override
		protected ItemValue<AddressBookDescriptor> map(VersionnedItem<DirEntry> item, boolean isCreate) {
			AddressBookDescriptor bookDesc = new AddressBookDescriptor();
			bookDesc.owner = domain.uid;
			if (!isCreate) {
				AddressBookDescriptor existing = api().getComplete(item.uid);
				bookDesc.owner = existing.owner;
			}
			bookDesc.domainUid = domain.uid;
			bookDesc.name = item.value.displayName;
			bookDesc.orgUnitUid = item.value.orgUnitUid;
			bookDesc.settings = Collections.emptyMap();
			return ItemValue.create(item.item(), bookDesc);
		}

		@Override
		protected void delete(IAddressBooksMgmt api, RecordKey key, String uid) {
			api.delete(uid);
		}
	}

	private class DomainCalendarCrudRestore extends AbstractCrudRestore<DirEntry, CalendarDescriptor, ICalendarsMgmt> {

		private final ValueReader<VersionnedItem<DirEntry>> reader = JsonUtils
				.reader(new TypeReference<VersionnedItem<DirEntry>>() {
				});

		private DomainCalendarCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return CALENDAR.name();
		}

		@Override
		protected ValueReader<VersionnedItem<DirEntry>> reader() {
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
		protected boolean exists(ICalendarsMgmt api, RecordKey key, VersionnedItem<DirEntry> item) {
			DirEntryHandler dirEntryApi = DirEntryHandlers.byKind(CALENDAR);
			BmContext context = ((ServerSideServiceProvider) target).getContext();
			ItemValue<DirEntry> previous = dirEntryApi.get(context, domain.uid, item.uid);
			if (previous != null && previous.internalId != item.internalId) {
				log.deleteByProduct(type(), key);
				DirEntryHandlers.byKind(CALENDAR).delete(context, domain.uid, item.uid);
				log.create(type(), key);
				DirEntryHandlers.byKind(CALENDAR).create(context, domain.uid, item);
			}
			return previous != null;
		}

		@Override
		protected ItemValue<CalendarDescriptor> map(VersionnedItem<DirEntry> item, boolean isCreate) {
			CalendarDescriptor calDesc = new CalendarDescriptor();
			calDesc.owner = domain.uid;
			if (!isCreate) {
				CalendarDescriptor existing = api().getComplete(item.uid);
				calDesc.owner = existing.owner;
			}
			calDesc.domainUid = domain.uid;
			calDesc.name = item.value.displayName;
			calDesc.orgUnitUid = item.value.orgUnitUid;
			calDesc.settings = Collections.emptyMap();
			return ItemValue.create(item.item(), calDesc);
		}

		@Override
		protected void delete(ICalendarsMgmt api, RecordKey key, String uid) {
			api.delete(uid);
		}
	}

	private class ExternalUserCrudRestore extends CrudDirEntryRestore.WithoutMailbox<ExternalUser> {

		private final ValueReader<VersionnedItem<FullDirEntry<ExternalUser>>> reader = JsonUtils
				.reader(new TypeReference<VersionnedItem<FullDirEntry<ExternalUser>>>() {
				});

		private ExternalUserCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return EXTERNALUSER.name();
		}

		@Override
		protected ValueReader<VersionnedItem<FullDirEntry<ExternalUser>>> reader() {
			return reader;
		}

		@Override
		protected IExternalUser api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IExternalUser.class, domain.uid);
		}
	}

	private class ResourceCrudRestore extends CrudDirEntryRestore.WithMailbox<ResourceDescriptor> {

		private final ValueReader<VersionnedItem<FullDirEntry<ResourceDescriptor>>> reader = JsonUtils
				.reader(new TypeReference<VersionnedItem<FullDirEntry<ResourceDescriptor>>>() {
				});

		private ResourceCrudRestore(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
			super(log, domain, target);
		}

		@Override
		public String type() {
			return RESOURCE.name();
		}

		@Override
		protected ValueReader<VersionnedItem<FullDirEntry<ResourceDescriptor>>> reader() {
			return reader;
		}

		@Override
		protected IResources api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IResources.class, domain.uid);
		}

		@Override
		protected void create(IRestoreDirEntryWithMailboxSupport<ResourceDescriptor> api, RecordKey key,
				VersionnedItem<FullDirEntry<ResourceDescriptor>> item) {
			createFakeResourceTypesIfNotExists(domain.uid, key, item);
			super.create(api, key, item);
		}

		@Override
		protected void update(IRestoreDirEntryWithMailboxSupport<ResourceDescriptor> api, RecordKey key,
				VersionnedItem<FullDirEntry<ResourceDescriptor>> item) {
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

		private final ValueReader<VersionnedItem<FullDirEntry<Mailshare>>> reader = JsonUtils
				.reader(new TypeReference<VersionnedItem<FullDirEntry<Mailshare>>>() {
				});

		private MailshareCrudRestore(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
			super(log, domain, target);
		}

		@Override
		public String type() {
			return MAILSHARE.name();
		}

		@Override
		protected ValueReader<VersionnedItem<FullDirEntry<Mailshare>>> reader() {
			return reader;
		}

		@Override
		protected IMailshare api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IMailshare.class, domain.uid);
		}

		@Override
		protected boolean filter(RecordKey key, VersionnedItem<FullDirEntry<Mailshare>> item) {
			return item.value.value.system;
		}

		@Override
		protected void create(IRestoreDirEntryWithMailboxSupport<Mailshare> api, RecordKey key,
				VersionnedItem<FullDirEntry<Mailshare>> item) {
			ItemValue<Mailbox> mbox = ItemValue.create(item.uid, item.value.mailbox);
			state.storeMailbox(item.uid, mbox);
			super.create(api, key, item);
		}
	}

	private class GroupCrudRestore extends CrudDirEntryRestore.WithMailbox<Group> {

		private final ValueReader<VersionnedItem<FullDirEntry<Group>>> reader = JsonUtils
				.reader(new TypeReference<VersionnedItem<FullDirEntry<Group>>>() {
				});

		private GroupCrudRestore(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
			super(log, domain, target);
		}

		@Override
		public String type() {
			return GROUP.name();
		}

		@Override
		protected ValueReader<VersionnedItem<FullDirEntry<Group>>> reader() {
			return reader;
		}

		@Override
		protected IInCoreGroup api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IInCoreGroup.class, domain.uid);
		}
	}

	private class UserCrudRestore extends CrudDirEntryRestore.WithMailbox<User> {

		private final ValueReader<VersionnedItem<FullDirEntry<User>>> reader = JsonUtils
				.reader(new TypeReference<VersionnedItem<FullDirEntry<User>>>() {
				});

		private UserCrudRestore(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
			super(log, domain, target);
		}

		@Override
		public String type() {
			return USER.name();
		}

		@Override
		protected ValueReader<VersionnedItem<FullDirEntry<User>>> reader() {
			return reader;
		}

		@Override
		protected IUser api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IUser.class, domain.uid);
		}

		@Override
		protected void create(IRestoreDirEntryWithMailboxSupport<User> api, RecordKey key,
				VersionnedItem<FullDirEntry<User>> item) {
			ItemValue<Mailbox> mbox = ItemValue.create(item.uid, item.value.mailbox);
			state.storeMailbox(item.uid, mbox);
			super.create(api, key, item);
		}
	}

	private class OrgUnitCrudRestore extends CrudDirEntryRestore.WithoutMailbox<OrgUnit> {

		private final ValueReader<VersionnedItem<FullDirEntry<OrgUnit>>> reader = JsonUtils
				.reader(new TypeReference<VersionnedItem<FullDirEntry<OrgUnit>>>() {
				});

		private OrgUnitCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		@Override
		public String type() {
			return ORG_UNIT.name();
		}

		@Override
		protected ValueReader<VersionnedItem<FullDirEntry<OrgUnit>>> reader() {
			return reader;
		}

		@Override
		protected IOrgUnits api(ItemValue<Domain> domain, RecordKey key) {
			return target.instance(IOrgUnits.class, domain.uid);
		}
	}
}
