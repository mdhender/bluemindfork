import { mapExtensions } from "@bluemind/extensions";
import { merge, reactive, sanitize } from "./builder";
import account from "./account";
import calendar from "./calendar";
import contacts from "./contacts";
import downloads from "./downloads";
import mail from "./mail";
import todolists from "./todolists";

export default function (vm) {
    let preferences = defaultSections(vm);
    preferences = merge(preferences, extendedPreferences());
    preferences = sanitize(preferences);
    return reactive(preferences, vm);
}

function defaultSections(vm) {
    let preferences = merge([], account(vm.$i18n));
    preferences = merge(preferences, calendar(vm.$i18n));
    preferences = merge(preferences, downloads(vm.$i18n));
    preferences = merge(preferences, mail(vm.$i18n));
    preferences = merge(preferences, contacts(vm.$i18n));
    return merge(preferences, todolists(vm.$i18n));
}

function extendedPreferences() {
    const sections = mapExtensions("webapp.preferences", ["section"]).section;
    return (sections || []).reduce((preferences, section) => {
        try {
            return merge(preferences, JSON.parse(section.raw));
        } catch {
            return preferences;
        }
    }, []);
}
