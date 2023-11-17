package net.bluemind.calendar;

import java.util.EnumSet;
import java.util.stream.Collectors;


public class EventChanges {

	public enum Type {
		EVENT, URL, CONFERENCE, SUMMARY, RRULE, PRIORITY, LOCATION, DESCRIPTION, DTSTART, DTEND, TRANSPARENCY,
		CLASSIFICATION, ATTACHMENTS, ATTENDEES
	}

	private final EnumSet<Type> changes;

	public EventChanges(EnumSet<Type> changes) {
		this.changes = changes;
	}

	public EnumSet<Type> getChanges() {
		return changes.stream().filter(change -> !change.equals(Type.EVENT))
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(Type.class)));
	}

	public boolean isSignificantChange() {
		return changes.stream().anyMatch(change -> !change.equals(Type.ATTENDEES));
	}

	public boolean contains(Type change) {
		return changes.contains(change);
	}

	public boolean hasChanged() {
		return !changes.isEmpty();
	}
}