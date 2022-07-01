package net.bluemind.domain.api;

import net.bluemind.core.container.model.ItemValue;

public interface IInCoreDomains extends IDomains {

	void restore(ItemValue<Domain> item, boolean isCreate);
}
