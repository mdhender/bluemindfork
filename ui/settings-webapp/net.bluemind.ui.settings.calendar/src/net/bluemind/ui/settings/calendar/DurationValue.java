package net.bluemind.ui.settings.calendar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.ListBox;

public class DurationValue extends Composite implements HasEnabled {

	private boolean enabled;
	private IntegerBox duration;
	private ListBox unit;

	private final CalendarMessages messages = GWT.create(CalendarMessages.class);

	public DurationValue() {
		HorizontalPanel panel = new HorizontalPanel();

		duration = new IntegerBox();
		duration.setWidth("70px");
		duration.setMaxLength(8);
		panel.add(duration);

		unit = new ListBox();
		unit.addItem(messages.seconds(), "seconds");
		unit.addItem(messages.minutes(), "minutes");
		unit.addItem(messages.hours(), "hours");
		unit.addItem(messages.days(), "days");
		panel.add(unit);

		Button reset = new Button(messages.deactivate());
		reset.addClickHandler(e -> duration.setValue(null));
		panel.add(reset);

		initWidget(panel);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public Integer getValue() {
		if (duration.getValue() == null) {
			return null;
		}

		int idx = unit.getSelectedIndex();

		if (idx == 1) {
			return duration.getValue() * 60;
		} else if (idx == 2) {
			return duration.getValue() * 3600;
		} else if (idx == 3) {
			return duration.getValue() * 86400;
		}

		return duration.getValue();
	}

	public void setValue(Integer value) {

		int val = value;
		int idx = 0;
		if (value % 86400 == 0) {
			val = value / 86400;
			idx = 3;
		} else if (value % 3600 == 0) {
			val = value / 3600;
			idx = 2;
		} else if (value % 60 == 0) {
			val = value / 60;
			idx = 1;
		}

		duration.setValue(val);
		unit.setSelectedIndex(idx);
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		duration.setEnabled(enabled);
		duration.setEnabled(enabled);
	}

}
