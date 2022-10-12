package net.bluemind.mailbox.api.rules;

import java.util.Arrays;
import java.util.List;

public interface ParameterValueProvider {

	List<String> provides(List<String> parameters);

	default String provides(String parameter) {
		return provides(Arrays.asList(parameter)).get(0);
	}
}
