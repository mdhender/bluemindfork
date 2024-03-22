import { onBeforeUnmount, onMounted, watch } from "vue";
import { AlertTypes } from "@bluemind/alert.store";
import { INFO, REMOVE } from "@bluemind/alert.store";
import TimezoneAlert from "../components/alerts/TimezoneAlert";
import store from "@bluemind/store";
import { useCookieSession } from "./useCookieSession";

const alertPayload = {
    alert: {
        name: "timezone",
        type: AlertTypes.INFO,
        uid: "timezoneuuid",
        payload: {
            kind: "Info",
            closeable: true
        }
    },
    options: { area: "system-alert", renderer: TimezoneAlert }
};
export function useTimezoneChecker() {
    const userAlertTimezonePreferencesCookie = useCookieSession("userAlertTimezonePreferences", {
        timezone: systemTimezone(),
        alertClosedByUser: false
    });
    let hasBeenAlertedOnce = false;

    function checkTimezone() {
        const userAlertTimezonePreferences = userAlertTimezonePreferencesCookie.getValue();
        if (
            isTimezoneDifferent() === true &&
            (userAlertTimezonePreferences.alertClosedByUser === false ||
                userAlertTimezonePreferences.timezone !== systemTimezone())
        ) {
            store.dispatch(`alert/${INFO}`, alertPayload);
            setUserAlertTimezonePreferences(systemTimezone(), false);
            hasBeenAlertedOnce = true;
        } else {
            if (
                store.state.alert.filter(({ area, type }) => area == "system-alert" && type == AlertTypes.INFO).length >
                0
            ) {
                store.dispatch(`alert/${REMOVE}`, alertPayload.alert);
            }
        }
    }

    let checkTimezoneIntervalId = null;

    function startIntervalTimezoneChecker() {
        return setInterval(checkTimezone, 1000 * 60);
    }

    function stopIntervalTimezoneChecker() {
        clearInterval(checkTimezoneIntervalId);
    }

    function isTimezoneDifferent() {
        const { timezone, timezone_difference_reminder } = store.state.settings || {};
        return timezone_difference_reminder === "true" && timezone !== systemTimezone();
    }
    function storeHasTimezoneAlert(storeAlerts) {
        return storeAlerts.filter(el => el.uid === alertPayload.alert.uid).length === 1;
    }

    function setUserAlertTimezonePreferences(timezone, preference) {
        userAlertTimezonePreferencesCookie.setValue({
            timezone: timezone,
            alertClosedByUser: preference
        });
    }

    onBeforeUnmount(() => {
        stopIntervalTimezoneChecker();
    });
    onMounted(() => {
        checkTimezoneIntervalId = startIntervalTimezoneChecker();
    });

    watch(store.state.alert, value => {
        if (storeHasTimezoneAlert(value) === false && isTimezoneDifferent() && hasBeenAlertedOnce) {
            setUserAlertTimezonePreferences(systemTimezone(), true);
        }
    });

    return { checkTimezone, storeHasTimezoneAlert };
}

function systemTimezone() {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
}
