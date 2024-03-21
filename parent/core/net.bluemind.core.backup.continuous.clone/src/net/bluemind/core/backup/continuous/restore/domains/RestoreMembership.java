package net.bluemind.core.backup.continuous.restore.domains;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.tools.LockByKey;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.service.IInCoreGroup;

public class RestoreMembership implements RestoreDomainType {
	private static final Logger logger = LoggerFactory.getLogger(RestoreMembership.class);

	private final ValueReader<VersionnedItem<GroupMembership>> membersReader = JsonUtils
			.reader(new TypeReference<VersionnedItem<GroupMembership>>() {
			});

	private final RestoreLogger log;
	private final ItemValue<Domain> domain;
	private final IServiceProvider target;
	private final RestoreState state;

	private final LockByKey<String> lock = new LockByKey<>();

	public RestoreMembership(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target, RestoreState state) {
		this.log = log;
		this.domain = domain;
		this.target = target;
		this.state = state;
	}

	@Override
	public String type() {
		return "memberships";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		VersionnedItem<GroupMembership> ms = membersReader.read(payload);
		ms.uid = state.uidAlias(ms.uid);
		IInCoreGroup groupApi = target.instance(IInCoreGroup.class, domain.uid);

		String lockKey = domain.uid + "-" + ms.uid;
		try {
			lock.lock(lockKey);
			ItemValue<Group> existingGroup = groupApi.getComplete(ms.uid);

			// fix user / admin group with our old uid
			ItemValue<Group> existingByName = groupApi.byName(ms.value.group.name);
			if (existingByName != null && existingGroup == null) {
				state.mapUid(ms.uid, existingByName.uid);
				existingGroup = existingByName;
				ms.uid = existingByName.uid;
			}

			if (existingGroup == null || existingGroup.internalId != ms.internalId) {
				// If the existing group has been created during domain creation, it will not
				// have the right id: we delete it here.

				if (existingGroup != null) {
					log.deleteParent(type(), key, ms.uid);
					TaskRef ref = groupApi.delete(ms.uid);
					TaskUtils.logStreamWait(target, ref);
				}
				// We remove any existing datalocation to prevent default mailbboxes to be
				// created as we don't have their ids here. They will be created with
				// the full group restoration
				ms.value.group.dataLocation = null;
				ItemValue<Group> clonedGroup = ItemValue.create(ms.item(), ms.value.group);
				log.createParent(type(), key, clonedGroup.uid);
				groupApi.restore(clonedGroup, true);

			}

			if (ms.value.added) {
				log.create(type(), key);
				groupApi.add(ms.uid, Arrays.asList(ms.value.member));
			} else {
				log.delete(type(), key);
				groupApi.remove(ms.uid, Arrays.asList(ms.value.member));
			}
		} finally {
			lock.unlock(lockKey);
		}
	}
}
