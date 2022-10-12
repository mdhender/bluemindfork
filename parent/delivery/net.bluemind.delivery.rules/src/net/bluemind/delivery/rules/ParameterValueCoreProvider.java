package net.bluemind.delivery.rules;

import java.util.List;
import java.util.stream.Stream;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;

public class ParameterValueCoreProvider implements ParameterValueProvider {
	private static final String DYNAMIC_PARAMETER_PREFIX = "BM_DYNAMIC_";

	private final ResolvedBox box;
	private final IServiceProvider serviceProvider;

	public ParameterValueCoreProvider(ResolvedBox box, IServiceProvider serviceProvider) {
		this.box = box;
		this.serviceProvider = serviceProvider;
	}

	@Override
	public List<String> provides(List<String> parameters) {
		return parameters.stream().flatMap(this::provideDynamicParameter).toList();
	}

	private Stream<String> provideDynamicParameter(String parameter) {
		if (parameter == null || !parameter.startsWith(DYNAMIC_PARAMETER_PREFIX)) {
			return Stream.of(parameter);
		}
		String action = parameter.replace(DYNAMIC_PARAMETER_PREFIX, "");
		if (action.equals("ADDRESSES_ME")) {
			return box.mbox.value.emails.stream().map(email -> email.address);
		}
		return Stream.of(parameter);
	}

}
