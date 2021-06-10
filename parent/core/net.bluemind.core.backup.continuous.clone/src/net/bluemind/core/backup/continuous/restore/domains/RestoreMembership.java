package net.bluemind.core.backup.continuous.restore.domains;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;

public class RestoreMembership {

	private final ValueReader<ItemValue<GroupMembership>> membersReader = JsonUtils
			.reader(new TypeReference<ItemValue<GroupMembership>>() {
			});

	private final IServerTaskMonitor monitor;
	private final IServiceProvider target;

	public RestoreMembership(IServerTaskMonitor monitor, IServiceProvider target) {
		this.monitor = monitor;
		this.target = target;
	}

	public void restore(DataElement de) {
		ItemValue<GroupMembership> ms = membersReader.read(new String(de.payload));

		IGroup groupApi = target.instance(IGroup.class, de.key.owner);

		monitor.log("Saving " + ms.value.members.size() + " member(s) for group " + ms.uid);
		List<Member> current = Optional.ofNullable(groupApi.getMembers(ms.uid)).orElse(Collections.emptyList());
		groupApi.add(ms.uid, ms.value.members);
		Map<String, Member> indexed = current.stream()
				.collect(Collectors.toMap(m -> m.type.name() + ":" + m.uid, m -> m));
		Map<String, Member> newIndexed = ms.value.members.stream()
				.collect(Collectors.toMap(m -> m.type.name() + ":" + m.uid, m -> m));
		HashSet<String> extra = new HashSet<>(indexed.keySet());
		extra.removeAll(newIndexed.keySet());
		List<Member> toRemove = extra.stream().map(indexed::get).collect(Collectors.toList());
		if (!toRemove.isEmpty()) {
			monitor.log("Remove " + toRemove.size() + " extra member(s)");
			groupApi.remove(ms.uid, toRemove);
		}
	}
}
