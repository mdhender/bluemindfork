import Roles from "@bluemind/roles";
import getPreferenceSections from "../components/preferences/sections/";

const mockedVueI18N = {
    t: () => ""
};

describe("PreferenceSections", () => {
    test("return a calendar section only if role allow it", () => {
        let roles = Roles.HAS_MAIL;
        const applications = [{ href: "/mail/" }];
        let sections = getPreferenceSections(applications, roles, mockedVueI18N);
        expect(sections.map(section => section.code)).not.toContain("calendar");

        applications.push({ href: "/cal/" });
        roles += "," + Roles.HAS_CALENDAR;
        sections = getPreferenceSections(applications, roles, mockedVueI18N);
        expect(sections.map(section => section.code)).toContain("calendar");
    });
});
