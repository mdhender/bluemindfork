package net.bluemind.domain.api;

import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;

public interface IInCoreDomains extends IDomains {

	void restore(ItemValue<Domain> item, boolean isCreate);

	void setProperties(String uid, Map<String, String> properties) throws ServerFault;

}