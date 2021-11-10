import { mapExtensions } from "@bluemind/extensions";
import account from "./account";
import calendar from "./calendar";
import contact from "./contact";
import mail from "./mail";
import downloads from "./downloads";
import { merge, reactive, sanitize } from "./builder";

export default function (vm) {
    let preferences = defaultSections(vm);
    preferences = merge(preferences, extendedPreferences());
    preferences = sanitize(preferences);
    return reactive(preferences, vm);
}

function defaultSections(vm) {
    let preferences = merge([], account(vm.$i18n));
    preferences = merge(preferences, calendar(vm.$i18n));
    preferences = merge(preferences, mail(vm.$i18n));
    preferences = merge(preferences, contact(vm.$i18n));
    return merge(preferences, downloads(vm.$i18n));
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
