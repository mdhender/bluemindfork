package net.bluemind.todolist.adapter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.parser.ICal4jHelper;
import net.bluemind.lib.ical4j.data.CalendarBuilder;
import net.bluemind.todolist.api.VTodo;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.model.property.PercentComplete;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.Strings;

public class VTodoAdapter extends ICal4jHelper<VTodo> {

	/**
	 * Initialize Calendar component
	 * 
	 * @return ICal Calendar component
	 */
	public static Calendar createTodoList() {
		Calendar todoList = new Calendar();
		todoList.getProperties().add(new ProdId("-//BlueMind//BlueMind TodoList//FR"));
		todoList.getProperties().add(Version.VERSION_2_0);
		todoList.getProperties().add(CalScale.GREGORIAN);
		return todoList;
	}

	/**
	 * Create an iCalendar {@link VToDo} component from a {@link VTodo} object.
	 * 
	 * @param BlueMind Vtodo
	 * @return ICalendar VToDo
	 */
	public static VToDo adaptTodo(String uid, VTodo vtodo) {
		VToDo ret = new VToDo();

		parseICalendarElement(uid, ret, vtodo);

		PropertyList properties = ret.getProperties();
		properties.add(Version.VERSION_2_0);

		if (uid != null) {
			Uid uidP = new Uid(uid);
			properties.add(uidP);
		}
		// DUE
		if (vtodo.due != null) {
			Due due = new Due(convertToIcsDate(vtodo.due));
			properties.add(due);
		}

		// PERCENT
		if (vtodo.percent != null) {
			PercentComplete percent = new PercentComplete(vtodo.percent);
			properties.add(percent);
		}

		// COMPLETED
		if (vtodo.completed != null) {
			DateTime ical4jCompleted = new DateTime(new BmDateTimeWrapper(vtodo.completed).toUTCTimestamp());
			Completed completed = new Completed(ical4jCompleted);
			properties.add(completed);
		}

		if (vtodo.status != null) {
			Status status = Status.VTODO_NEEDS_ACTION;
			switch (vtodo.status) {
			case Cancelled:
				status = Status.VTODO_CANCELLED;
				break;
			case Completed:
				status = Status.VTODO_COMPLETED;
				break;
			case InProcess:
				status = Status.VTODO_IN_PROCESS;
				break;
			default:
				logger.warn("no VTODO status value for {}", vtodo.status);
				break;
			}

			properties.add(status);
		}
		return ret;
	}

	/**
	 * @param ics
	 * @return
	 * @throws ServerFault
	 */
	public List<ItemValue<VTodo>> convertToVTodoList(String ics) throws ServerFault {
		List<ItemValue<VTodo>> ret = new ArrayList<>();
		InputStream is = new ByteArrayInputStream(ics.getBytes());
		try (Reader reader = new InputStreamReader(is);
				UnfoldingReader unfoldingReader = new UnfoldingReader(reader, true)) {
			CalendarBuilder builder = new CalendarBuilder();
			BiConsumer<Calendar, Component> componentConsumer = (calendar, component) -> {
				if (!Component.VTODO.equals(component.getName())) {
					return;
				}

				// X-WR-TIMEZONE
				Optional<String> globalTZ = calendar.getProperty("X-WR-TIMEZONE") != null
						? Optional.of(calendar.getProperty("X-WR-TIMEZONE").getValue())
						: Optional.empty();
				net.fortuna.ical4j.model.component.VToDo ical4j = (net.fortuna.ical4j.model.component.VToDo) component;

				Item item = new Item();
				if (ical4j.getCreated() != null) {
					item.created = ical4j.getCreated().getDate();
				}
				if (ical4j.getLastModified() != null) {
					item.updated = ical4j.getLastModified().getDate();
				}
				if (ical4j.getUid() != null) {
					item.uid = ical4j.getUid().getValue();
				}

				VTodo vtodo = new VTodo();
				vtodo = parseIcs(vtodo, ical4j, globalTZ, Optional.empty()).value;

				// DUE
				vtodo.due = parseIcsDate(ical4j.getDue(), globalTZ, Collections.emptyMap());

				// PERCENT
				if (ical4j.getPercentComplete() != null) {
					vtodo.percent = new Integer(ical4j.getPercentComplete().getValue());
				}

				// COMPLETE
				if (ical4j.getDateCompleted() != null) {
					vtodo.completed = parseIcsDate(ical4j.getDateCompleted(), globalTZ, Collections.emptyMap());
				}

				// DESC
				if (ical4j.getDescription() != null) {
					vtodo.description = ical4j.getDescription().getValue();
				}
				// STATUS
				if (ical4j.getStatus() != null) {
					String s = ical4j.getStatus().getValue();
					vtodo.status = ICalendarElement.Status.NeedsAction;
					switch (s) {
					case "NEEDS-ACTION":
						vtodo.status = ICalendarElement.Status.NeedsAction;
						break;
					case "CANCELLED":
						vtodo.status = ICalendarElement.Status.Cancelled;
						break;
					case "COMPLETED":
						vtodo.status = ICalendarElement.Status.Completed;
						break;
					case "IN-PROCESS":
						vtodo.status = ICalendarElement.Status.InProcess;
						break;
					default:
						logger.warn("unkown status from VTODO {}", vtodo.status);
						break;
					}
				}
				ItemValue<VTodo> itemValue = ItemValue.create(item, vtodo);
				ret.add(itemValue);

			};

			builder.build(unfoldingReader, componentConsumer);

		} catch (Exception e) {
			logger.error("Exception during ICS import. {}", e.getMessage());
			throw new ServerFault(e);
		}

		return ret;
	}

	public static String convertToIcs(ItemValue<VTodo> task) {
		Calendar todolist = VTodoAdapter.createTodoList();
		StringBuffer buffer = new StringBuffer();
		buffer.append(Calendar.BEGIN);
		buffer.append(':');
		buffer.append(Calendar.VCALENDAR);
		buffer.append(Strings.LINE_SEPARATOR);
		buffer.append(todolist.getProperties());
		buffer.append(Strings.LINE_SEPARATOR);
		buffer.append(VTodoAdapter.adaptTodo(task.uid, task.value).toString());
		buffer.append(Calendar.END);
		buffer.append(':');
		buffer.append(Calendar.VCALENDAR);
		return buffer.toString();
	}
}
