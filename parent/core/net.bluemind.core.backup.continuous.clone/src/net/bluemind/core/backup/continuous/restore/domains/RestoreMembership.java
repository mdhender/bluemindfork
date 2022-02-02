package net.bluemind.core.backup.continuous.restore.domains;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
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

	private final RestoreLogger log;
	private ItemValue<Domain> domain;
	private final IServiceProvider target;

	public RestoreMembership(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		this.log = log;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "memberships";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		ItemValue<GroupMembership> ms = membersReader.read(payload);

		IGroup groupApi = target.instance(IGroup.class, domain.uid);
		ItemValue<Group> existingGroup = groupApi.getComplete(ms.uid);
		if (existingGroup == null) {
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
	}
}
