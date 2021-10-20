import iteratee from "lodash.iteratee";

import Roles from "@bluemind/roles";

import getCalendarSection from "./CalendarSection";
import getDownloadsSection from "./DownloadsSection";
import getMyAccountSection from "./MyAccountSection";
import getWebmailSection from "./WebmailSection";

/**
 * Here is the 'heart' of the Settings.
 * Each section, like "mail", holds several categories, like "main" or "advanced". Each category holds groups of fields.
 * These fields are created using Dynamic Components (see PrefContent).
 */
export default function (applications, roles, vueI18N) {
    const sections = [normalize(getMyAccountSection(roles, vueI18N))];

    if (roles.includes(Roles.HAS_MAIL)) {
        sections.push(normalize(getWebmailSection(roles, vueI18N, applications)));
    }
    if (roles.includes(Roles.HAS_CALENDAR)) {
        sections.push(normalize(getCalendarSection(vueI18N, applications)));
    }

    sections.push(normalize(getDownloadsSection(roles, vueI18N)));

    return sections;
}

function normalize(section) {
    section.categories = section.categories.map(category => normalizeCategory(category));
    return section;
}

function normalizeCategory(category) {
    category.groups = category.groups.map(group => normalizeGroup(group));
    return category;
}

function normalizeGroup(group) {
    switch (typeof group.notAvailable) {
        case "string":
            if (group.notAvailable === "false" || group.notAvailable === "true") {
                const value = group.notAvailable === "true";
                group.notAvailable = () => value;
            } else {
                group.notAvailable = iteratee(group.notAvailable);
            }
            break;
        case "function":
        case "object":
        case "array":
            group.notAvailable = iteratee(group.notAvailable);
            break;
        default:
            group.notAvailable = () => false;
    }
    return group;
}
