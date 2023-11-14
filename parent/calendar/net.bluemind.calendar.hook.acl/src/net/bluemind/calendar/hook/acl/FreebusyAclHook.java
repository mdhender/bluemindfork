package net.bluemind.calendar.hook.acl;

import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.core.container.api.IInternalContainerManagement;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;

public class FreebusyAclHook implements IAclHook {

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		if (ICalendarUids.TYPE.equals(container.type)
				&& (container.uid.equals(ICalendarUids.defaultUserCalendar(container.owner))
						|| container.uid.equals(ICalendarUids.resourceCalendar(container.owner)))) {
			String containerUidToSynchronize = IFreebusyUids.getFreebusyContainerUid(container.owner);
			synchronizeFreebusyRights(context, containerUidToSynchronize, current);
		}	
	}

	private void synchronizeFreebusyRights(BmContext context, String containerUidToSynchronize,
			List<AccessControlEntry> current) {
		List<AccessControlEntry> fbAcl = current.stream()
				.filter(ace -> ace.verb.can(Verb.Freebusy) || ace.verb.can(Verb.Manage))
				.map(calAce -> createFbAce(calAce)).collect(Collectors.toList());
		IInternalContainerManagement cmgmt = context.su().provider().instance(IInternalContainerManagement.class,
				containerUidToSynchronize);
		cmgmt.setAccessControlList(fbAcl, false);
	}

	private AccessControlEntry createFbAce(AccessControlEntry calAce) {
		Verb destinationVerb = calAce.verb.can(Verb.Manage) ? Verb.Manage : Verb.Read;
		return AccessControlEntry.create(calAce.subject, destinationVerb);
	}
}
