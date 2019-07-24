package net.bluemind.system.ldap.export.enhancer;

import java.util.Arrays;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.system.ldap.export.objects.DomainDirectoryGroup.MembersList;
import net.bluemind.user.api.User;

public class EnhancerTestImpl implements IEntityEnhancer {
	private final List<String> userAttrs = Arrays.asList("audio");

	@Override
	public Entry enhanceUser(ItemValue<Domain> domain, ItemValue<User> user, Entry entry) throws LdapException {
		entry.add("audio", "11119999");
		return entry;
	}

	@Override
	public List<String> userEnhancerAttributes() {
		return userAttrs;
	}

	@Override
	public Entry enhanceGroup(ItemValue<Domain> domain, ItemValue<Group> group, MembersList members, Entry entry)
			throws LdapException {
		return entry;
	}

	@Override
	public List<String> groupEnhancerAttributes() {
		return null;
	}

}
