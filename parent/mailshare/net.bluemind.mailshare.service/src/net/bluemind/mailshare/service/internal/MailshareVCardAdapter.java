package net.bluemind.mailshare.service.internal;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.service.AbstractVCardAdapter;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareVCardAdapter extends AbstractVCardAdapter<Mailshare> {

	@Override
	public VCard asVCard(ItemValue<Domain> domain, String uid, Mailshare mailshare) throws ServerFault {
		VCard card = mailshare.card;
		card.kind = Kind.individual;
		card.source = "bm://" + domain.uid + "/mailshares/" + uid;

		card.communications.emails = getEmails(domain, mailshare.emails);

		if (card.identification.formatedName == null || card.identification.formatedName.value == null) {
			card.identification.formatedName = FormatedName.create(mailshare.name);
		}

		return card;

	}

}
