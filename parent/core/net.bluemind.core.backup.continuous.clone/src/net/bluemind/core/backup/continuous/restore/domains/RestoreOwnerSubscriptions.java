package net.bluemind.core.backup.continuous.restore.domains;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptions;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.IInternalUserSubscription;

public class RestoreOwnerSubscriptions implements RestoreDomainType {

	private static final ValueReader<ItemValue<ContainerSubscriptionModel>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<ContainerSubscriptionModel>>() {
			});
	private final IServerTaskMonitor monitor;
	private final ItemValue<Domain> domain;
	private final IServiceProvider target;
	private final IInternalUserSubscription subscriptionApi;

	public RestoreOwnerSubscriptions(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target) {
		this.monitor = monitor;
		this.domain = domain;
		this.target = target;
		this.subscriptionApi = target.instance(IInternalUserSubscription.class, domain.uid);
	}

	@Override
	public String type() {
		return IOwnerSubscriptionUids.TYPE;
	}

	@Override
	public void restore(DataElement de) {
		String payload = new String(de.payload);
		IInternalOwnerSubscriptions ownerSubscriptionApi = target.instance(IInternalOwnerSubscriptions.class,
				domain.uid, de.key.owner);
		if (de.payload.length > 0) {
			subscribe(de.key, payload, ownerSubscriptionApi);
		} else {
			delete(de.key, ownerSubscriptionApi);
		}
	}

	private void subscribe(RecordKey key, String payload, IInternalOwnerSubscriptions ownerSubscriptionApi) {
		ItemValue<ContainerSubscriptionModel> item = mrReader.read(payload);
		ItemValue<ContainerSubscriptionModel> existing = ownerSubscriptionApi.getCompleteById(key.id);
		if (existing != null) {
			ownerSubscriptionApi.updateWithItem(item);
			monitor.log("Update subscription: '" + key.owner + "' subscribe to '" + item.value.containerUid + "'");
		} else {
			ownerSubscriptionApi.createWithItem(item);
			monitor.log("Create subscription: '" + key.owner + "' subscribe to '" + item.value.containerUid + "'");
		}
		subscriptionApi.subscribe(key.owner, subscribedContainer(item.value));
	}

	private void delete(RecordKey key, IInternalOwnerSubscriptions ownerSubscriptionApi) {
		ItemValue<ContainerSubscriptionModel> existing = ownerSubscriptionApi.getCompleteById(key.id);
		ownerSubscriptionApi.delete(existing.uid);
		subscriptionApi.unsubscribe(key.owner, subscribedContainer(existing.value));
		monitor.log("Delete subscription: '" + key.owner + "' unsubscribe from '" + existing.value.containerUid + "'");
	}

	private ContainerDescriptor subscribedContainer(ContainerSubscriptionModel ownerSubscription) {
		return ContainerDescriptor.create(ownerSubscription.containerUid, ownerSubscription.name,
				ownerSubscription.owner, ownerSubscription.containerType, domain.uid,
				ownerSubscription.defaultContainer);
	}
}