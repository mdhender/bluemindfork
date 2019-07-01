/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.reminder.job;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BodyPart;

import freemarker.template.TemplateException;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public interface IAlarmSupport<T extends ICalendarElement> {

	public String getContainerType();

	public List<Reminder<T>> getReminder(BmDateTime dtalarm, BaseContainerDescriptor containerDescriptor)
			throws ServerFault;

	public String buildSubject(Map<String, String> settings, Map<String, Object> data);

	public Map<String, Object> extractEntityDataToMap(T entity, VAlarm valarm);

	public void logSchedInfo(Mailbox to, Reminder<T> reminder, IScheduler scheduler, IScheduledJobRunId rid);

	public BodyPart buildBody(String locale, Map<String, Object> data) throws IOException, TemplateException;

	public void addMQProperties(OOPMessage msg, ItemValue<T> entity, VAlarm valarm);

	public String getName();

	public void initContainerItemsCache(BmDateTime dtalarm, BaseContainerDescriptor containerDescriptor);

}
