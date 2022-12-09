package net.bluemind.dav.server.proto.post;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.model.ItemValue;

public final class VEventPut {

	private final String updateHref;
	private final ItemValue<VEventSeries> event;

	public VEventPut(ItemValue<VEventSeries> event, String updateHref) {
		this.event = event;
		this.updateHref = updateHref;
	}

	public boolean isUpdate() {
		return updateHref != null;
	}

	public ItemValue<VEventSeries> getEvent() {
		return event;
	}

	public String getUpdateHref() {
		return updateHref;
	}

}
