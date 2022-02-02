package net.bluemind.core.backup.continuous.restore.domains.crud;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailflow.api.IMailflowRules;
import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;

public class RestoreMailflow extends CrudRestore<MailRuleActionAssignmentDescriptor> {
	private static final ValueReader<ItemValue<MailRuleActionAssignmentDescriptor>> reader = JsonUtils
			.reader(new TypeReference<ItemValue<MailRuleActionAssignmentDescriptor>>() {
			});

	private final IServiceProvider target;

	public RestoreMailflow(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return "mailflow";
	}

	@Override
	protected ValueReader<ItemValue<MailRuleActionAssignmentDescriptor>> reader() {
		return reader;
	}

	@Override
	protected IMailflowRules api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(IMailflowRules.class, domain.uid);
	}
}
