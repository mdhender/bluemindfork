<script setup>
import { BmButton } from "@bluemind/ui-components";
import store from "@bluemind/store";
import AlertTypes from "@bluemind/store";
import { WARNING, REMOVE } from "@bluemind/alert.store";
import TimezoneChangedAlert from "./TimezoneChangedAlert.vue";

const props = defineProps({
    alert: {
        type: Object,
        required: true
    }
});
const systemTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone.trim();

function changeSettings() {
    store.dispatch("settings/SAVE_SETTING", { setting: "timezone", value: systemTimezone });
    store.dispatch("alert/" + REMOVE, { uid: "timezoneuuid" });

    let alert = {
        name: "timezone",
        type: AlertTypes.WARNING,
        uid: "timezoneuuid",
        payload: {
            kind: "Warn",
            closeable: true
        }
    };
    store.dispatch(`alert/${WARNING}`, {
        alert: alert,
        options: { area: "system-alert", renderer: TimezoneChangedAlert }
    });
}
</script>

<template>
    <div class="timezone-alert flex-fill">
        <i18n path="alert.preferences.system_timezone">
            <template #timezone>
                {{ systemTimezone }}
            </template>
            <template #preference>
                <router-link
                    to="#preferences-my_account-main"
                    @click.native="$store.commit('preferences/TOGGLE_PREFERENCES')"
                >
                    {{ $t("alert.preferences.change_preferences") }}
                </router-link>
            </template>
            <template #user-timezone>{{ $store.state.settings.timezone }}</template>
        </i18n>
        <bm-button size="sm" variant="outline" class="mw-100" @click="changeSettings()">
            {{ $t("alert.preferences.set_timezone", { timezone: systemTimezone }) }}
        </bm-button>
    </div>
</template>
