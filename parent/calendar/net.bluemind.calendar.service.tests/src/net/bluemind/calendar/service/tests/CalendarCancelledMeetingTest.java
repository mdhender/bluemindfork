package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.internal.VEventCancellationSanitizer;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;

public class CalendarCancelledMeetingTest {

	@Test
	public void testUpdateEventMainSuccessesBecauseMainEventNotCancelled() {
		VEventSeries oldEvent = generateVEvent();

		VEventSeries toUpdateEvent = generateVEvent();
		toUpdateEvent.main.location = "Montauban";

		VEventCancellationSanitizer.sanitize(oldEvent, toUpdateEvent);
		assertEquals("Montauban", toUpdateEvent.main.location);

	}

	@Test
	public void testUpdateEventOccurenceSuccessesBecauseOccurenceNotCancelled() {
		VEventSeries oldEvent = generateVEvent();

		VEventSeries toUpdateEvent = generateVEvent();
		toUpdateEvent.occurrences.get(0).location = "Montauban";

		VEventCancellationSanitizer.sanitize(oldEvent, toUpdateEvent);
		assertEquals("Montauban", toUpdateEvent.occurrences.get(0).location);

	}

	@Test
	public void testUpdateEventAddOccurenceSuccessesStatusConfirmed() {
		VEventSeries oldEvent = generateVEvent();
		VEventSeries toUpdateEvent = generateVEvent();

		VEventOccurrence newOccurence = generateOccurence(oldEvent.main.dtstart, "new occurrence",
				List.of(ICalendarElement.Attendee.create(ICalendarElement.CUType.Individual, "",
						ICalendarElement.Role.Chair, ICalendarElement.ParticipationStatus.NeedsAction, true, "", "", "",
						"user01", null, null, null, "user02@attendee.lan")));
		toUpdateEvent.occurrences.add(newOccurence);

		VEventCancellationSanitizer.sanitize(oldEvent, toUpdateEvent);
		assertEquals(2, toUpdateEvent.occurrences.size());
		assertEquals("new occurrence", toUpdateEvent.occurrences.get(1).summary);

	}

	@Test
	public void testUpdateEventMainFailsBecauseMainEventCancelled() {
		VEventSeries oldEvent = generateVEvent();
		oldEvent.main.status = ICalendarElement.Status.Cancelled;

		VEventSeries toUpdateEvent = generateVEvent();
		toUpdateEvent.main.location = "Montauban";

		VEventCancellationSanitizer.sanitize(oldEvent, toUpdateEvent);
		assertEquals(oldEvent.main.location, toUpdateEvent.main.location);

	}

	@Test
	public void testUpdateEventOccurenceFailsBecauseOccurenceCancelled() {
		VEventSeries oldEvent = generateVEvent();
		oldEvent.occurrences.get(0).status = ICalendarElement.Status.Cancelled;

		VEventSeries toUpdateEvent = generateVEvent();
		toUpdateEvent.occurrences.get(0).recurid = oldEvent.occurrences.get(0).recurid;
		toUpdateEvent.occurrences.get(0).location = "Montauban";

		VEventCancellationSanitizer.sanitize(oldEvent, toUpdateEvent);
		assertEquals(oldEvent.occurrences.get(0).location, toUpdateEvent.occurrences.get(0).location);

	}

	@Test
	public void testUpdateEventAddOccurenceSuccessesStatusCancelled() {
		VEventSeries oldEvent = generateVEvent();
		VEventSeries toUpdateEvent = generateVEvent();

		VEventOccurrence newOccurence = generateOccurence(oldEvent.main.dtstart, "new occurrence",
				List.of(ICalendarElement.Attendee.create(ICalendarElement.CUType.Individual, "",
						ICalendarElement.Role.Chair, ICalendarElement.ParticipationStatus.NeedsAction, true, "", "", "",
						"user01", null, null, null, "user02@attendee.lan")));
		newOccurence.status = ICalendarElement.Status.Cancelled;
		toUpdateEvent.occurrences.add(newOccurence);

		VEventCancellationSanitizer.sanitize(oldEvent, toUpdateEvent);
		assertEquals(2, toUpdateEvent.occurrences.size());
		assertEquals("new occurrence", toUpdateEvent.occurrences.get(1).summary);

	}

	@Test
	public void testCancelledEventsAreNotModified() {

		VEventSeries oldEvent = generateVEvent();
		VEventSeries toUpdateEvent01 = generateVEvent();

		// Sets the new event status to cancelled
		toUpdateEvent01.occurrences.get(0).status = ICalendarElement.Status.Cancelled;
		toUpdateEvent01.occurrences.get(0).attendees
				.get(0).partStatus = ICalendarElement.ParticipationStatus.NeedsAction;
		VEventCancellationSanitizer.sanitize(oldEvent, toUpdateEvent01);
		assertEquals(ICalendarElement.ParticipationStatus.NeedsAction,
				toUpdateEvent01.occurrences.get(0).attendees.get(0).partStatus);

		// Changes the user participation status after the meeting has been cancelled
		VEventSeries toUpdateEvent02 = toUpdateEvent01.copy();
		toUpdateEvent02.occurrences.get(0).attendees.get(0).partStatus = ICalendarElement.ParticipationStatus.Accepted;
		VEventCancellationSanitizer.sanitize(toUpdateEvent01, toUpdateEvent02);
		assertEquals(ICalendarElement.ParticipationStatus.NeedsAction,
				toUpdateEvent02.occurrences.get(0).attendees.get(0).partStatus);
	}

	private VEventSeries generateVEvent() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.now().plusDays(3));
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = ICalendarElement.Classification.Private;
		event.status = ICalendarElement.Status.Confirmed;
		event.priority = 3;
		event.url = "https://www.bluemind.net";
		event.conference = "https//vi.sio.com/xxx";
		event.conferenceConfiguration.put("conf1", "val1");
		event.conferenceConfiguration.put("conf2", "val2");

		event.attachments = new ArrayList<>();
		AttachedFile attachment1 = new AttachedFile();
		attachment1.publicUrl = "http://somewhere/1";
		attachment1.name = "test.gif";
		attachment1.cid = "cid0123456789";
		event.attachments.add(attachment1);
		AttachedFile attachment2 = new AttachedFile();
		attachment2.publicUrl = "http://somewhere/2";
		attachment2.name = "test.png";
		event.attachments.add(attachment2);

		event.organizer = new VEvent.Organizer("test@bm.lan");

		List<ICalendarElement.Attendee> attendees = new ArrayList<>(1);
		ICalendarElement.Attendee me = ICalendarElement.Attendee.create(ICalendarElement.CUType.Individual, "",
				VEvent.Role.Chair, ICalendarElement.ParticipationStatus.Accepted, true, "", "", "", "external", null,
				null, null, "external@attendee.lan");
		attendees.add(me);

		event.attendees = attendees;

		series.main = event;

		VEventOccurrence occurrence = generateOccurence(series.main.dtstart, "Occurence 01",
				List.of(ICalendarElement.Attendee.create(ICalendarElement.CUType.Individual, "",
						ICalendarElement.Role.Chair, ICalendarElement.ParticipationStatus.Accepted, true, "", "", "",
						"user01", null, null, null, "user01@attendee.lan")));
		List<VEventOccurrence> occurrences = new ArrayList<>();
		occurrences.add(occurrence);
		series.occurrences = occurrences;
		return series;
	}

	private VEventOccurrence generateOccurence(BmDateTime recurId, String summary, List<Attendee> attendees) {
		VEventOccurrence occurrence = new VEventOccurrence();
		occurrence.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now().plusHours(2), Precision.DateTime);
		occurrence.recurid = recurId;
		occurrence.summary = summary;
		occurrence.location = "Toulouse";
		occurrence.description = "Lorem ipsum";
		occurrence.transparency = VEvent.Transparency.Opaque;
		occurrence.classification = ICalendarElement.Classification.Private;
		occurrence.status = ICalendarElement.Status.Confirmed;
		occurrence.priority = 42;
		occurrence.attendees.addAll(attendees);

		occurrence.organizer = new VEvent.Organizer();
		occurrence.organizer.uri = UUID.randomUUID().toString();
		occurrence.organizer.dir = "bm://users/org";
		return occurrence;
	}
}
