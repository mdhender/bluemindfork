package net.bluemind.mailflow.service.validators;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.mailflow.hook.IMailflowConfigValidator;

public class MailflowJournalingConfigValidator implements IMailflowConfigValidator {

	@Override
	public String getAction() {
		return "JournalingAction";
	}

	@Override
	public void validate(Map<String, String> configuration) throws ServerFault {
		if (configuration == null || configuration.isEmpty()) {
			throw new ServerFault("Mailflow configuration is null");
		}
		String targetEmail = configuration.get("targetEmail");
		if (targetEmail == null || targetEmail.isEmpty()) {
			throw new ServerFault("Target email must not be null");
		} else if (!Regex.EMAIL.validate(targetEmail)) {
			throw new ServerFault(String.format("Target email '%s' does not match a valid email", targetEmail));
		}

		String filteredEmails = configuration.get("emailsFiltered");
		if (filteredEmails != null && !filteredEmails.isEmpty()) {
			List<String> errorEmailList = Arrays.asList(filteredEmails.split(";")).stream()
					.filter(e -> !Regex.EMAIL.validate(e)).collect(Collectors.toList());
			if (!errorEmailList.isEmpty()) {
				throw new ServerFault(String.format("Filtered email(s) '%s' don't match a valid email",
						errorEmailList.stream().collect(Collectors.joining(";"))));
			}
		}
	}

}
