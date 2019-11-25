package net.bluemind.externaluser.service.internal;

import java.util.Arrays;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.externaluser.api.ExternalUser;

public class ExternalUserSanitizer implements ISanitizer<ExternalUser> {

	private BmContext context;

	public ExternalUserSanitizer(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(ExternalUser extUser) throws ServerFault {
		sanitize(extUser);
		new Sanitizer(context).create(extUser.contactInfos);
	}

	@Override
	public void update(ExternalUser current, ExternalUser updated) throws ServerFault {
		sanitize(updated);
		new Sanitizer(context).update(current.contactInfos, updated.contactInfos);
	}

	private void sanitize(ExternalUser extUser) {
		// sanitize vcard
		if (extUser.contactInfos == null) {
			extUser.contactInfos = new VCard();
		}

		if (extUser.contactInfos.defaultMail() == null && extUser.defaultEmail() != null) {
			extUser.contactInfos.communications.emails = Arrays.asList(VCard.Communications.Email.create(extUser.defaultEmailAddress()));
		} else if (extUser.contactInfos.defaultMail() != null) {
			extUser.emails = Arrays.asList(Email.create(extUser.contactInfos.defaultMail(), true));
		}
	}

}
