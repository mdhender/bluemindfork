package net.bluemind.dav.server.proto.post;

import net.fortuna.ical4j.vcard.VCard;

public final class VCardPut {

	private final String updateHref;
	private final VCard vcard;

	public VCardPut(VCard vcard, String updateHref) {
		this.vcard = vcard;
		this.updateHref = updateHref;
	}

	public boolean isUpdate() {
		return updateHref != null;
	}

	public VCard getVcard() {
		return vcard;
	}

	public String getUpdateHref() {
		return updateHref;
	}

}
