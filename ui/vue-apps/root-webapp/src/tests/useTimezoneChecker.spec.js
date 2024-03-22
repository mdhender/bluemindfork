import { mount } from "@vue/test-utils";
import { useTimezoneChecker } from "../composables/useTimezoneChecker";
import alertStore from "@bluemind/alert.store";
import store from "@bluemind/store";
import settingsStore from "../settingsStore";
const timezoneMock = function (zone) {
    const DateTimeFormat = Intl.DateTimeFormat;
    jest.spyOn(global.Intl, "DateTimeFormat").mockImplementation(
        (locale, options) => new DateTimeFormat(locale, { ...options, timeZone: zone })
    );
};

describe("useTimezoneChecker", () => {
    beforeEach(() => {
        store.registerModule("alert", alertStore);
        store.registerModule("settings", settingsStore);
    });
    afterEach(() => {
        store.unregisterModule("alert");
        store.unregisterModule("settings");
        jest.restoreAllMocks();
    });
    it("user should be notified", () => {
        timezoneMock("Europe/Paris");
        settingsStore.mutations.SET_SETTING(store.state, { setting: "settings", value: {} });
        settingsStore.mutations.SET_SETTING(store.state.settings, { setting: "timezone", value: "America/Chicago" });
        settingsStore.mutations.SET_SETTING(store.state.settings, {
            setting: "timezone_difference_reminder",
            value: "true"
        });

        const wrapper = mount({
            store,
            template: `<div></div>`,
            setup() {
                const { checkTimezone, storeHasTimezoneAlert } = useTimezoneChecker();
                return { checkTimezone, storeHasTimezoneAlert };
            }
        });
        wrapper.vm.checkTimezone();
        expect(wrapper.vm.storeHasTimezoneAlert(store.state.alert)).toBe(true);
    });
    it("user shouldn't be notified", () => {
        let storeHasAlerts = false;
        timezoneMock("Europe/Paris");
        settingsStore.mutations.SET_SETTING(store.state, { setting: "settings", value: {} });
        settingsStore.mutations.SET_SETTING(store.state.settings, {
            setting: "timezone",
            value: Intl.DateTimeFormat().resolvedOptions().timeZone
        });

        const wrapper = mount({
            store,
            storeHasAlerts,
            template: `<div></div>`,
            setup() {
                const { checkTimezone, storeHasTimezoneAlert } = useTimezoneChecker();
                return { checkTimezone, storeHasTimezoneAlert };
            }
        });
        wrapper.vm.checkTimezone();
        expect(wrapper.vm.storeHasTimezoneAlert(store.state.alert)).toBe(false);
    });
});
