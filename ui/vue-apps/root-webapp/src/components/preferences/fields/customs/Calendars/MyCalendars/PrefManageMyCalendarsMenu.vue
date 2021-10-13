<template>
    <!-- FIXME: click outside of menu dont close it.. -->
    <bm-contextual-menu class="float-right">
        <bm-dropdown-item-button icon="pencil" @click="$emit('update')">
            {{ $t("common.edit") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="share" @click="$emit('manage-shares')">
            {{ $t("common.share") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="upload" @click="$emit('import-ics')">
            {{ $t("common.import") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="broom" @click="$emit('reset-data')">
            {{ $t("common.action.reset") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="trash" :disabled="isDefaultCalendar" @click="$emit('remove')">
            {{ $t("common.delete") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            v-if="calendar.settings.type === 'externalIcs'"
            icon="loop"
            :disabled="isSyncInProgress"
            @click="$emit('synchronize-external-ics')"
        >
            {{ $t("common.start_synchronization") }}
        </bm-dropdown-item-button>
    </bm-contextual-menu>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmContextualMenu, BmDropdownItemButton } from "@bluemind/styleguide";

export default {
    name: "PrefManageMyCalendarsMenu",
    components: { BmContextualMenu, BmDropdownItemButton },
    props: {
        calendar: {
            type: Object,
            required: true
        },
        isSyncInProgress: {
            type: Boolean,
            required: true
        }
    },
    computed: {
        isDefaultCalendar() {
            return this.calendar.uid && this.calendar.uid === "calendar:Default:" + inject("UserSession").userId;
        }
    }
};
</script>
