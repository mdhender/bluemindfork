package net.bluemind.externaluser.service.internal;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.externaluser.api.ExternalUser;

public class ExternalUserSanitizerFactory implements ISanitizerFactory<ExternalUser> {

	@Override
	public Class<ExternalUser> support() {
		return ExternalUser.class;
	}

	@Override
	public ISanitizer<ExternalUser> create(BmContext context, Container container) {
		return new ExternalUserSanitizer(context);
	}

}
