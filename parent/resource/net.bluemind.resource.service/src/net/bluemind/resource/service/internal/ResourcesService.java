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
package net.bluemind.resource.service.internal;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IFreebusyMgmt;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.utils.ImageUtils;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.service.DirDomainValue;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirEventProducer;
import net.bluemind.domain.api.Domain;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Rule;
import net.bluemind.mailbox.service.IInCoreMailboxes;
import net.bluemind.resource.api.EventInfo;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.helper.IResourceTemplateHelper;
import net.bluemind.resource.helper.ResourceTemplateHelpers;
import net.bluemind.resource.hook.IResourceHook;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;

public class ResourcesService implements IResources {
	private static final Logger logger = LoggerFactory.getLogger(ResourcesService.class);
	private BmContext context;
	private ResourceContainerStoreService storeService;
	private ResourceValidator validator = new ResourceValidator();
	private IResourceTypes types;
	private String domainUid;
	private Sanitizer extSanitizer;
	private Validator extValidator;
	private RBACManager rbacManager;

	private DirEventProducer dirEventProducer;
	private IInCoreMailboxes mailboxes;

	private ResourceMailboxAdapter mailboxAdapter;
	private IUser userService;
	private IUserSettings userSettingsService;
	private List<IResourceHook> hooks = getHooks();
	private static final IResourceTemplateHelper RESOURCE_TEMPLATE_HELPER = ResourceTemplateHelpers.getInstance();

	public ResourcesService(BmContext context, ItemValue<Domain> domain, Container resourcesContainer)
			throws ServerFault {
		this.context = context;
		this.domainUid = domain.uid;
		this.types = context.provider().instance(IResourceTypes.class, domainUid);
		storeService = new ResourceContainerStoreService(context, domain, resourcesContainer);
		extSanitizer = new Sanitizer(context);
		extValidator = new Validator(context);

		rbacManager = new RBACManager(context).forContainer(resourcesContainer);
		dirEventProducer = new DirEventProducer(domainUid, VertxPlatform.eventBus());

		mailboxes = context.su().provider().instance(IInCoreMailboxes.class, domainUid);

		mailboxAdapter = new ResourceMailboxAdapter();

		this.userService = context.su().provider().instance(IUser.class, domainUid);
		this.userSettingsService = context.su().provider().instance(IUserSettings.class, domainUid);
	}

	private static List<IResourceHook> getHooks() {
		RunnableExtensionLoader<IResourceHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.resource", "resourceHook", "hook", "class");
	}

	@Override
	public void create(String uid, ResourceDescriptor rd) throws ServerFault {
		ItemValue<ResourceDescriptor> resourceDescriptorItem = ItemValue.create(uid, rd);
		createWithItem(resourceDescriptorItem);
	}

	private void createWithItem(ItemValue<ResourceDescriptor> resourceDescriptorItem) throws ServerFault {
		String uid = resourceDescriptorItem.uid;
		ResourceDescriptor rd = resourceDescriptorItem.value;
		rbacManager.forOrgUnit(rd.orgUnitUid).check(BasicRoles.ROLE_MANAGE_RESOURCE);

		extSanitizer.create(rd);
		extSanitizer.create(new DirDomainValue<>(domainUid, uid, rd));

		ParametersValidator.notNullAndNotEmpty(uid);
		validator.validate(rd);
		extValidator.create(rd);

		ResourceTypeDescriptor typeDesc = getTypeOrFail(rd.typeIdentifier);

		validator.validatePropertiesValue(rd, typeDesc);

		mailboxes.validate(uid, mailboxAdapter.asMailbox(domainUid, uid, rd));

		storeService.create(resourceDescriptorItem);

		mailboxes.created(uid, mailboxAdapter.asMailbox(domainUid, uid, rd));
		mailboxes.setMailboxFilter(uid, discardRule());

		// create calendar
		ContainerDescriptor calContainerDescriptor = ContainerDescriptor.create(ICalendarUids.TYPE + ":" + uid,
				rd.label, uid, ICalendarUids.TYPE, domainUid, true);
		IContainers containers = context.su().provider().instance(IContainers.class);
		containers.create(calContainerDescriptor.uid, calContainerDescriptor);

		String fbContainerUid = IFreebusyUids.getFreebusyContainerUid(uid);
		ContainerDescriptor containerDescriptor = ContainerDescriptor.create(fbContainerUid, "freebusy container", uid,
				IFreebusyUids.TYPE, domainUid, true);
		containers.create(fbContainerUid, containerDescriptor);
		context.su().provider().instance(IContainerManagement.class, fbContainerUid)
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(domainUid, Verb.Read)));

		context.su().provider().instance(IFreebusyMgmt.class, fbContainerUid).add(calContainerDescriptor.uid);

		dirEventProducer.changed(uid, storeService.getVersion());
	}

	private MailFilter discardRule() {
		Rule r = new MailFilter.Rule();
		r.active = true;
		r.criteria = "MATCHALL";
		r.discard = true;

		return MailFilter.create(r);
	}

	@Override
	public void update(String uid, ResourceDescriptor rd) throws ServerFault {
		ItemValue<ResourceDescriptor> resourceDescriptorItem = ItemValue.create(uid, rd);
		updateWithItem(resourceDescriptorItem);
	}

	private void updateWithItem(ItemValue<ResourceDescriptor> resourceDescriptorItem) throws ServerFault {
		String uid = resourceDescriptorItem.uid;
		checkManageResource(uid);

		ParametersValidator.notNullAndNotEmpty(uid);
		ResourceDescriptor rd = resourceDescriptorItem.value;

		ItemValue<DirEntryAndValue<ResourceDescriptor>> previousItemValue = storeService.get(uid, null);
		if (previousItemValue == null) {
			throw new ServerFault(notFoundMessage(uid), ErrorCode.NOT_FOUND);
		}

		ItemValue<ResourceDescriptor> previous = ItemValue.create(previousItemValue, previousItemValue.value.value);
		if (previous == null) {
			throw new ServerFault(notFoundMessage(uid), ErrorCode.NOT_FOUND);
		}

		if (!StringUtils.equals(rd.orgUnitUid, previous.value.orgUnitUid)) {
			rbacManager.forOrgUnit(rd.orgUnitUid).check(BasicRoles.ROLE_MANAGE_RESOURCE);
		}
		extSanitizer.update(previous.value, rd);
		extSanitizer.update(new DirDomainValue<>(domainUid, uid, previous.value),
				new DirDomainValue<>(domainUid, uid, rd));

		validator.validate(rd);
		extValidator.update(previous.value, rd);

		if (!previous.value.typeIdentifier.equals(rd.typeIdentifier)) {
			throw new ServerFault("type of resource can't be modified", ErrorCode.INVALID_PARAMETER);
		}

		ResourceTypeDescriptor typeDesc = getTypeOrFail(rd.typeIdentifier);
		validator.validatePropertiesValue(rd, typeDesc);

		mailboxes.validate(uid, mailboxAdapter.asMailbox(domainUid, uid, rd));

		storeService.update(resourceDescriptorItem);

		mailboxes.updated(uid, previousItemValue.value.mailbox, mailboxAdapter.asMailbox(domainUid, uid, rd));
		mailboxes.setMailboxFilter(uid, discardRule());

		ContainerModifiableDescriptor descriptor = new ContainerModifiableDescriptor();
		descriptor.defaultContainer = true;
		descriptor.name = rd.label;
		IContainerManagement container = context.su().provider().instance(IContainerManagement.class,
				ICalendarUids.TYPE + ":" + uid);
		container.update(descriptor);
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public TaskRef delete(String uid) throws ServerFault {
		checkManageResource(uid);
		return context.provider().instance(ITasksManager.class).run(monitor -> performDelete(uid, monitor));
	}

	private void performDelete(String uid, IServerTaskMonitor monitor) {

		monitor.begin(3, "Deleting resource " + uid + "@" + domainUid);

		ParametersValidator.notNullAndNotEmpty(uid);
		ItemValue<DirEntryAndValue<ResourceDescriptor>> previousItemValue = storeService.get(uid, null);
		if (previousItemValue == null) {
			monitor.end(false, notFoundMessage(uid), "[]");
			return;
		}

		ItemValue<ResourceDescriptor> previous = ItemValue.create(previousItemValue, previousItemValue.value.value);

		if (previous == null) {
			monitor.end(false, notFoundMessage(uid), "[]");
			return;
		}

		try {
			ICalendar cal = context.su().provider().instance(ICalendar.class, ICalendarUids.TYPE + ":" + uid);
			TaskRef tr = cal.reset();
			TaskUtils.wait(context.su().provider(), tr);
			context.su().provider().instance(IContainers.class).delete(ICalendarUids.TYPE + ":" + uid);
		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("calendar for resource {}@{} not found", uid, domainUid);
			} else {
				monitor.end(false, e.getMessage(), "[]");
				return;
			}
		}

		try {
			monitor.progress(2, "Deleting resource calendar ...");
			String fbContainerUid = IFreebusyUids.getFreebusyContainerUid(uid);
			IFreebusyMgmt mgm = context.su().provider().instance(IFreebusyMgmt.class, fbContainerUid);
			mgm.get().forEach(mgm::remove);
			context.su().provider().instance(IContainers.class).delete(fbContainerUid);
		} catch (Exception e) {
			logger.warn("Cannot delete Freebusy container of resource {}:{}", uid, e.getMessage());
		}

		hooks.forEach(hook -> hook.onBeforeDelete(context, previous));

		monitor.progress(2, "Deleting resource mailbox ...");
		mailboxes.deleted(uid, mailboxAdapter.asMailbox(domainUid, uid, previous.value));
		monitor.progress(3, "Resource mailbox deleted");

		storeService.delete(uid);
		dirEventProducer.deleted(uid, storeService.getVersion());

		monitor.end(true, "Resource deleted", JsonUtils.asString(""));

	}

	@Override
	public ResourceDescriptor get(String uid) throws ServerFault {
		ItemValue<ResourceDescriptor> itemValue = getComplete(uid);

		if (itemValue == null) {
			return null;
		}

		return itemValue.value;
	}

	private ResourceTypeDescriptor getTypeOrFail(String typeIdentifier) throws ServerFault {
		ResourceTypeDescriptor desc = types.get(typeIdentifier);
		if (desc == null) {
			throw new ServerFault("type descriptor " + typeIdentifier + " not found");
		}

		return desc;
	}

	private void checkManageResource(String uid) throws ServerFault {
		if (uid == null) {
			rbacManager.check(BasicRoles.ROLE_MANAGE_RESOURCE);
		} else {
			rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_RESOURCE);
		}
	}

	@Override
	public byte[] getIcon(String uid) throws ServerFault {
		ItemValue<ResourceDescriptor> resource = storeService.get(uid);
		if (resource == null) {
			return null;
		}

		byte[] data = storeService.getIcon(uid);
		if (data == null) {
			return types.getIcon(resource.value.typeIdentifier);
		} else {
			return data;
		}
	}

	@Override
	public void setIcon(String uid, byte[] icon) throws ServerFault {
		checkManageResource(uid);
		ResourceDescriptor previous = get(uid);
		if (previous == null) {
			throw new ServerFault(notFoundMessage(uid), ErrorCode.NOT_FOUND);
		}

		byte[] png = ImageUtils.checkAndSanitize(icon);
		storeService.setPhoto(uid, icon, png);
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public ItemValue<ResourceDescriptor> byEmail(String email) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.findByEmail(email);
	}

	private ItemValue<ResourceDescriptor> getComplete(String uid) throws ServerFault {
		// FIXME read will be fixed once every direntry will be in the same
		// container
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_MANAGER);
		ParametersValidator.notNullAndNotEmpty(uid);
		return storeService.get(uid);
	}

	@Override
	public List<String> byType(String typeUid) throws ServerFault {
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_MANAGER);
		return storeService.findByType(typeUid);
	}

	@Override
	public String addToEventDescription(final String resourceUid, final EventInfo eventInfo) {
		if (!RESOURCE_TEMPLATE_HELPER.containsTemplate(eventInfo.getDescription(), resourceUid)) {
			final String descForCalEvent = this.buildDescForCalEvent(resourceUid, eventInfo.getOrganizerUid());
			return RESOURCE_TEMPLATE_HELPER.addTemplate(eventInfo.getDescription(), descForCalEvent);
		} else {
			return eventInfo.getDescription();
		}
	}

	@Override
	public String removeFromEventDescription(String resourceUid, final EventInfo eventInfo) {
		return RESOURCE_TEMPLATE_HELPER.removeTemplate(eventInfo.getDescription(), resourceUid);
	}

	/** Build a piece of description using the resource's template - if any. */
	private String buildDescForCalEvent(final String resourceUid, final String origanizerUid) {
		final String organizerName = this.userService.getVCard(origanizerUid).identification.formatedName.value;
		final String organizerLanguage = this.userSettingsService.get(origanizerUid).get("lang");
		return RESOURCE_TEMPLATE_HELPER.processTemplate(this.domainUid, resourceUid, organizerLanguage, organizerName);
	}

	private String notFoundMessage(String uid) {
		return "Resource " + uid + " doesnt exists";
	}

	@Override
	public void restore(ItemValue<ResourceDescriptor> item, boolean isCreate) {
		if (isCreate) {
			createWithItem(item);
		} else {
			updateWithItem(item);
		}

	}

}
