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
package net.bluemind.ui.adminconsole.dataprotect.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface DPTexts extends Messages {

	public static final DPTexts INST = GWT.create(DPTexts.class);

	String navigatorTitle();

	String navigatorDesc();

	String genInfos(String version, int domains, int users, int mailshares, int ous, int cals, int adbooks,
			int resources);

	String colType();

	String colEntity();

	String colDomain();

	String colActions();

	String confirmForget();

	String retPolicyTitle();

	String retPolicyDesc();

	String retDaily();

	String retWeekly();

	String retMonthly();

	String confirm();

	String restoreDialogTitle();

	String cancel();

	String restore();

	String backup();

	String backupEmails();

	String sync();

	String backupES();

	String replacemailbox();

	String subfoldermailbox();

	String restorefilehosting();

	String replaceou();

	String completerestore();

	String replaceresources();

	String replacebooks();

	String replacedomainbooks();

	String replacecalendars();

	String replacedomaincalendars();

	String replacetodolists();

	String sendresources();

	String sendbooksvcf();

	String senddomainbooksvcf();

	String sendcalendarsics();

	String senddomaincalendarsics();

	String sendtodolistics();

	String replacenotes();

	String replacewebappdata();

	String retentionDaysTitle();

	String replacesmimecerts();
}
