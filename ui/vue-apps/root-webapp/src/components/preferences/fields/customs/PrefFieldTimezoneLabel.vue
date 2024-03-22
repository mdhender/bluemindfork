<script setup>
import store from "@bluemind/store";
import { WARNING } from "@bluemind/alert.store";
import { BmButton } from "@bluemind/ui-components";
const systemTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

function changeSettings() {
    store.commit("preferences/fields/PUSH_STATE", {
        id: "my_account.main.localisation.timezone",
        value: systemTimezone
    });
}
const props = defineProps({
    label: {
        type: String,
        required: true
    }
});
</script>
<template>
    <div class="pref-field-timezone-label">
        <div class="regular timezone-label">
            {{ label }}
        </div>
        <bm-button
            v-show="$store.state.settings.timezone !== systemTimezone"
            variant="link"
            class="timezone-label-link"
            @click.stop="changeSettings"
        >
            {{ $t("preferences.calendar.main.set_timezone", { timezone: systemTimezone }) }}
        </bm-button>
    </div>
</template>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-field-timezone-label {
    display: flex;
    flex-direction: column;
    flex-wrap: wrap;
    gap: $sp-2 $sp-4;
}
.timezone-label {
    padding-bottom: $sp-2;
}
.timezone-label-link {
    justify-content: start;
    padding-bottom: $sp-4;
}
</style>
