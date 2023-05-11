/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

public class DpTextsHelper {

	public static String translate(String identifier) {
		if (identifier.startsWith("complete.restore")) {
			return DPTexts.INST.completerestore();
		}

		switch (identifier) {
		case "replace.mailbox":
			return DPTexts.INST.replacemailbox();
		case "subfolder.mailbox":
			return DPTexts.INST.subfoldermailbox();
		case "restore.filehosting":
			return DPTexts.INST.restorefilehosting();
		case "replace.resources":
			return DPTexts.INST.replaceresources();
		case "replace.ou":
			return DPTexts.INST.replaceou();
		case "replace.books":
			return DPTexts.INST.replacebooks();
		case "replace.domain.books":
			return DPTexts.INST.replacedomainbooks();
		case "replace.calendars":
			return DPTexts.INST.replacecalendars();
		case "replace.domain.calendars":
			return DPTexts.INST.replacedomaincalendars();
		case "replace.todolists":
			return DPTexts.INST.replacetodolists();
		case "replace.notes":
			return DPTexts.INST.replacenotes();
		case "replace.webappdata":
			return DPTexts.INST.replacewebappdata();
		case "send.resources":
			return DPTexts.INST.sendresources();
		case "send.books.vcf":
			return DPTexts.INST.sendbooksvcf();
		case "send.domain.books.vcf":
			return DPTexts.INST.senddomainbooksvcf();
		case "send.calendars.ics":
			return DPTexts.INST.sendcalendarsics();
		case "send.domain.calendars.ics":
			return DPTexts.INST.senddomaincalendarsics();
		case "send.todolist.ics":
			return DPTexts.INST.sendtodolistics();
		case "replace.smimecerts":
			return DPTexts.INST.replacesmimecerts();
		default:
			return identifier;
		}
	}

}
