package net.bluemind.core.backup.continuous.restore.domains;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;

public class RestoreMembership implements RestoreDomainType {
	private static final Logger logger = LoggerFactory.getLogger(RestoreMembership.class);

	private final ValueReader<ItemValue<GroupMembership>> membersReader = JsonUtils
			.reader(new TypeReference<ItemValue<GroupMembership>>() {
			});

	private final IServerTaskMonitor monitor;
	private ItemValue<Domain> domain;
	private final IServiceProvider target;

	public RestoreMembership(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target) {
		this.monitor = monitor;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "memberships";
	}

	@Override
	public void restore(DataElement de) {
		try {
			monitor.log("Processing membership:\n" + de.key + "\n" + new String(de.payload));
			ItemValue<GroupMembership> ms = membersReader.read(new String(de.payload));

			IGroup groupApi = target.instance(IGroup.class, domain.uid);
			ItemValue<Group> existingGroup = groupApi.getComplete(ms.uid);
			if (existingGroup == null) {
				ItemValue<Group> clonedGroup = ItemValue.create(ms.item(), ms.value.group);
				groupApi.createWithItem(clonedGroup);
			}
			if (ms.value.added) {
				monitor.log("Saving 1 member for group " + ms.uid);
				groupApi.add(ms.uid, Arrays.asList(ms.value.member));
			} else {
				monitor.log("Removing 1 member for group " + ms.uid);
				groupApi.remove(ms.uid, Arrays.asList(ms.value.member));
			}
		} catch (Throwable t) {
			monitor.log("Failed to restore membership: " + t.getMessage());
		}
	}
}
