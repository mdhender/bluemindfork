import Roles from "@bluemind/roles";

import getCalendarSection from "./CalendarSection";
import getMyAccountSection from "./MyAccountSection";
import getWebmailSection from "./WebmailSection";

/**
 * Here is the 'heart' of the Settings.
 * Each section, like "mail", holds several categories, like "main" or "advanced". Each category holds groups of fields.
 * These fields are created using Dynamic Components (see PrefContent).
 */
export default function (applications, roles, vueI18N) {
    const sections = [getMyAccountSection(roles, vueI18N)];

    if (roles.includes(Roles.HAS_MAIL)) {
        sections.push(getWebmailSection(roles, vueI18N, applications));
    }
    if (roles.includes(Roles.HAS_CALENDAR)) {
        sections.push(getCalendarSection(vueI18N, applications));
    }

    return sections;
}
