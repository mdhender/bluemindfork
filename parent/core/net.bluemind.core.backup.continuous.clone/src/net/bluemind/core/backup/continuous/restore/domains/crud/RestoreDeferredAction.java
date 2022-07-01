package net.bluemind.core.backup.continuous.restore.domains.crud;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.deferredaction.api.IInternalDeferredAction;
import net.bluemind.domain.api.Domain;

public class RestoreDeferredAction extends CrudItemRestore<DeferredAction> {

	private static final ValueReader<VersionnedItem<DeferredAction>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<DeferredAction>>() {
			});
	private final IServiceProvider target;

	public RestoreDeferredAction(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return IDeferredActionContainerUids.TYPE;
	}

	@Override
	protected ValueReader<VersionnedItem<DeferredAction>> reader() {
		return reader;
	}

	@Override
	protected IInternalDeferredAction api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(IInternalDeferredAction.class, key.uid);
	}

}
