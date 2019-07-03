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
package net.bluemind.ui.adminconsole.jobs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface JobTexts extends Messages {

	JobTexts INST = GWT.create(JobTexts.class);

	String jobId();

	String status();

	String job();

	String search();

	String jobKind();

	String planification();

	String lastExecs();

	String jobNeverExecuted();

	String domain();

	String nextExecution();

	String lastExecution();

	String description();

	String generalTab();

	String title(String s);

	String startNow();

	String filterPlaceholder();

	String duration();

	String seconds(String s);

	String removeExecs();

	String successStatus();

	String warningStatus();

	String failureStatus();

	String inProgressStatus();

	String close();

	String globalJobKind();

	String multidomainJobKind();

	String planKind();

	String opportunisticPlan();

	String opportunisticDesc();

	String scheduledPlan();

	String disabledPlan();

	String disabledDesc();

	String periodicMinRecKind();

	String periodicHourRecKind();

	String dailyRecKind();

	String every();

	String minutes();

	String hours();

	String gmtWarning();

	String dayMondayOneLetter();

	String dayTuesdayOneLetter();

	String dayWednesdayOneLetter();

	String dayThursdayOneLetter();

	String dayFridayOneLetter();

	String daySaturdayOneLetter();

	String daySundayOneLetter();

	String atTime();

	String invalidPlanification();

	String viewLogs();

	String report();

	String reportEnable();

	String reportRecipients();

	String reportLevel();

	String planning();

	String scheduledJobs();

}
