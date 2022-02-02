package net.bluemind.core.backup.continuous.restore.domains.crud;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptions;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.IInternalUserSubscription;

public class RestoreOwnerSubscriptions extends CrudRestore<ContainerSubscriptionModel> {

	private static final ValueReader<ItemValue<ContainerSubscriptionModel>> reader = JsonUtils
			.reader(new TypeReference<ItemValue<ContainerSubscriptionModel>>() {
			});
	private final IServiceProvider target;
	private final IInternalUserSubscription subscriptionApi;

	public RestoreOwnerSubscriptions(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
		this.subscriptionApi = target.instance(IInternalUserSubscription.class, domain.uid);
	}

	@Override
	public String type() {
		return IOwnerSubscriptionUids.TYPE;
	}

	@Override
	protected ValueReader<ItemValue<ContainerSubscriptionModel>> reader() {
		return reader;
	}

	@Override
	protected IInternalOwnerSubscriptions api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(IInternalOwnerSubscriptions.class, domain.uid, key.owner);
	}

	@Override
	protected boolean filter(RecordKey key, ItemValue<ContainerSubscriptionModel> item) {
		return key.owner.equals(item.value.owner);
	}

	@Override
	protected void create(IRestoreCrudSupport<ContainerSubscriptionModel> api, RecordKey key,
			ItemValue<ContainerSubscriptionModel> item) {
		super.create(api, key, item);
		subscriptionApi.subscribe(key.owner, subscribedContainer(item.value));
	}

	@Override
	protected void update(IRestoreCrudSupport<ContainerSubscriptionModel> api, RecordKey key,
			ItemValue<ContainerSubscriptionModel> item) {
		super.update(api, key, item);
		subscriptionApi.subscribe(key.owner, subscribedContainer(item.value));
	}

	@Override
	protected void delete(IRestoreCrudSupport<ContainerSubscriptionModel> api, RecordKey key, String uid) {
		super.delete(api, key, uid);
		ContainerSubscriptionModel existing = api.get(uid);
		subscriptionApi.unsubscribe(key.owner, subscribedContainer(existing));
	}

	private ContainerDescriptor subscribedContainer(ContainerSubscriptionModel ownerSubscription) {
		return ContainerDescriptor.create(ownerSubscription.containerUid, ownerSubscription.name,
				ownerSubscription.owner, ownerSubscription.containerType, domain.uid,
				ownerSubscription.defaultContainer);
	}
}