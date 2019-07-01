/**
 * 
 */
package net.bluemind.calendar.hook.ics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEvent.Transparency;
import net.bluemind.calendar.hook.VEventUtil;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.tag.api.TagRef;

/**
 * @author mehdi
 *
 */
public class VEventUtilTest {

	private static final long NOW = System.currentTimeMillis();
	private VEvent old;
	private VEvent updated;

	@Before
	public void before() throws Exception {
		old = simpleVEvent();
		updated = simpleVEvent();
	}
	@Test
	public void testSimpleEventNotChanged() {
		assertFalse(VEventUtil.eventChanged(old, updated));
	}
	@Test
	public void testDTStartChanged() {
		updated.dtstart = date(30 * 60 * 1000);
		assertTrue(VEventUtil.eventChanged(old, updated));
	}
	@Test
	public void testDTEndChanged() {
		updated.dtend = date(30 * 60 * 1000);
		assertTrue(VEventUtil.eventChanged(old, updated));
	}
	@Test
	public void testSummaryChanged() {
		updated.summary = "Updated";
		assertTrue(VEventUtil.eventChanged(old, updated));
	}
	@Test
	public void testLocationChanged() {
		updated.location = "Frouzins";
		assertTrue(VEventUtil.eventChanged(old, updated));
	}
	@Test
	public void testDescriptionChanged() {
		updated.description = "Dolor sit amet";
		assertTrue(VEventUtil.eventChanged(old, updated));
	}
	@Test
	public void testPriorityChanged() {
		updated.priority = 2;
		assertTrue(VEventUtil.eventChanged(old, updated));
	}
	@Test
	public void testTransparencyNotChanged() {
		updated.transparency = Transparency.Transparent;
		assertTrue(VEventUtil.eventChanged(old, updated));
	}
	@Test
	public void testClassificationChanged() {
		updated.classification = Classification.Private;
		assertTrue(VEventUtil.eventChanged(old, updated));
	}
	@Test
	public void testOnwerChanged() {
		// Owner changed not handled by VEventUtil.eventChanged.
		
		setOwner(updated);
//		assertTrue(VEventUtil.eventChanged(old, updated));
		setOwner(old);
		assertFalse(VEventUtil.eventChanged(old, updated));
//		updated.organizer.mailto = "changed@bm.lan";
//		assertTrue(VEventUtil.eventChanged(old, updated));
//		setOwner(updated);
	}
	@Test
	public void testAttendeesChanged() {
		// Attendees changed not handled by VEventUtil.eventChanged.
		setAttendees(updated);
//		assertTrue(VEventUtil.eventChanged(old, updated));
		setAttendees(old);
		assertFalse(VEventUtil.eventChanged(old, updated));
//		updated.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
//				ParticipationStatus.NeedsAction, false, "attendee4", "", "", "", "", "", "attendee4",
//				"attendee4@bm.lan"));
//		assertTrue(VEventUtil.eventChanged(old, updated));
//		setAttendees(updated);
	}
	@Test
	public void testCategoriesChanged() {
		// Categories changed not handled by VEventUtil.eventChanged.
		setCategories(updated);
//		assertTrue(VEventUtil.eventChanged(old, updated));
		setCategories(old);
		assertFalse(VEventUtil.eventChanged(old, updated));
//		TagRef tag = new TagRef();
//		tag.containerUid = "container" ;
//		tag.itemUid = "item";
//		updated.categories.add(tag);
//		assertTrue(VEventUtil.eventChanged(old, updated));
//		setCategories(updated);
	}
	@Test
	public void testRDateChanged() {
		// RDate changed not handled by VEventUtil.eventChanged.
		setRDate(updated);
//		assertTrue(VEventUtil.eventChanged(old, updated));
		setRDate(old);
		assertFalse(VEventUtil.eventChanged(old, updated));
//		updated.rdate.add(date(72 * 60 * 60 * 1000));
//		assertTrue(VEventUtil.eventChanged(old, updated));
//		setRDate(updated);
	}
	@Test
	public void testRRuleChanged() {
		setRRule(updated);
		assertTrue(VEventUtil.eventChanged(old, updated));
		setRRule(old);
		assertFalse(VEventUtil.eventChanged(old, updated));
		updated.rrule.frequency = Frequency.WEEKLY;
		assertTrue(VEventUtil.eventChanged(old, updated));
		setRRule(updated);
		updated.rrule.count = 1000;
		assertTrue(VEventUtil.eventChanged(old, updated));
		setRRule(updated);
		updated.rrule.interval = 5;
		assertTrue(VEventUtil.eventChanged(old, updated));
		setRRule(updated);
	}
	@Test
	public void testExdateChanged() {
		// ExDate changed not handled by VEventUtil.eventChanged.
		setExDate(updated);
//		assertTrue(VEventUtil.eventChanged(old, updated));
		setExDate(old);
		assertFalse(VEventUtil.eventChanged(old, updated));
//		updated.exdate.add(date(72 * 60 * 60 * 1000));
//		assertTrue(VEventUtil.eventChanged(old, updated));
//		setExDate(updated);
	}

	private void setExDate(VEvent event) {
		event.exdate = new HashSet<BmDateTime>();
		event.exdate.add(date(24 * 60 * 60 * 1000));
		event.exdate.add(date(48 * 60 * 60 * 1000));

	}

	private void setRRule(VEvent event) {
		event.rrule = new VEvent.RRule();
		event.rrule.frequency = Frequency.DAILY;
		event.rrule.interval = 2;
		event.rrule.count = 5;
	}

	private void setRDate(VEvent event) {
		event.rdate = new HashSet<BmDateTime>();
		event.rdate.add(date(24 * 60 * 60 * 1000));
		event.rdate.add(date(48 * 60 * 60 * 1000));
	}

	private void setOwner(VEvent event) {
		event.organizer = new VEvent.Organizer("Billy", "organizer@bm.lan");
	}

	private void setAttendees(VEvent event) {
		Attendee attendee1 = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "attendee1", "", "", "", "", "", "attendee1",
				"attendee1@bm.lan");
		Attendee attendee2 = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "attendee2", "", "", "", "", "", "attendee2",
				"attendee2@bm.lan");
		Attendee attendee3 = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "attendee3", "", "", "", "", "", "attendee3",
				"attendee3@bm.lan");
		event.attendees = Arrays.asList(attendee1, attendee2, attendee3);
	}

	private void setCategories(VEvent event) {
		TagRef tag1 = new TagRef();
		tag1.containerUid = "container1" ;
		tag1.itemUid = "item1";
		TagRef tag2 = new TagRef();
		tag2.containerUid = "container2" ;
		tag2.itemUid = "item2";
		event.categories = Arrays.asList(tag1, tag2);
	}

	private BmDateTime date(long delta) {
		DateTimeZone tz = DateTimeZone.forID("Europe/Paris");
		DateTime date = new DateTime(NOW + delta, tz);
		return BmDateTimeWrapper.create(date, Precision.DateTime);
	}

	private VEvent simpleVEvent() {
		VEvent event = new VEvent();
		event.dtstart = date(0);
		event.dtend = date(1000 * 60 * 60);
		event.summary = "Summary";
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.priority = 1;
		event.transparency = Transparency.Opaque;
		event.classification = Classification.Public;
		event.status = Status.Tentative;
		event.attendees = new ArrayList<>();
		event.categories = new ArrayList<TagRef>(0);
		return event;
	}

	//
	// event.organizer = new VEvent.Organizer(null, "organizer@bm.lan");
	// // event.organizer.uri = CalendarContainerType.TYPE + ":Default:" +
	// // "u1";
	// event.attendees = new ArrayList<>();
	// event.categories = new ArrayList<TagRef>(0);
	//
	// event.rdate = new HashSet<BmDateTime>();
	// event.rdate.add(BmDateTimeWrapper.create(temp, Precision.Date));
	//
	// VEventSeries series = new VEventSeries();
	// series.main = event;
	// ItemValue<VEventSeries> event = defaultVEvent("invite");
	// event.value.main.attendees = Arrays
	// .asList(VEvent.Attendee.create(CUType.Individual, "",
	// Role.RequiredParticipant,
	// ParticipationStatus.NeedsAction, false, userUid, "", "", "", "", "", userUid,
	// userEmail));
	// ItemValue<VEventSeries> event = defaultVEventWithAttendee(string, userUid,
	// userEmail);
	//
	// event.value.icsUid = event.uid;
	// event.value.main.status = ICalendarElement.Status.NeedsAction;
	// event.value.main.rrule = new VEvent.RRule();
	// event.value.main.rrule.frequency = Frequency.DAILY;
	// event.value.main.rrule.interval = 10; // ?
	// return event;

}
