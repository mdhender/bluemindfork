package net.bluemind.directory.hollow.datamodel.producer;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.hollow.datamodel.producer.Value.StringValue;
import net.bluemind.externaluser.api.ExternalUser;

public class ExternalUserSerializer extends ContactInfosSerializer {

	private final ItemValue<ExternalUser> externalUser;

	protected ExternalUserSerializer(ItemValue<ExternalUser> externalUser, ItemValue<DirEntry> dirEntry,
			String domainUid) {
		super(dirEntry, domainUid);
		this.externalUser = externalUser;
	}

	@Override
	protected VCard contactInfos() {
		return externalUser.value.contactInfos;
	}

	@Override
	public Value get(Property property) {
		switch (property) {
		case DisplayName:
			return new StringValue(externalUser.displayName);
		case SmtpAddress:
			return getDefaultSmtp();
		default:
			return super.get(property);
		}
	}

	private Value getDefaultSmtp() {
		return (dirEntry.value.email != null) ? new StringValue(dirEntry.value.email) : Value.NULL;
	}
}
