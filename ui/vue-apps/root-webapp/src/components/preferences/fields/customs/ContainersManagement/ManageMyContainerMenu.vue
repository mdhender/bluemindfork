<template>
    <bm-icon-dropdown variant="compact" icon="3dots-vertical" no-caret lazy>
        <bm-dropdown-item-button
            icon="pencil"
            :disabled="container.defaultContainer && !isCalendarType"
            @click="$emit('update')"
        >
            {{ $t("common.edit") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="share" @click="$emit('manage-shares')">
            {{ $t("common.share") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="box-arrow-up" @click="$emit('import')">
            {{ $t("common.import") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="broom" @click="$emit('reset-data')">
            {{ $t("common.action.empty") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="trash" :disabled="container.defaultContainer" @click="$emit('remove')">
            {{ $t("common.delete") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            v-if="displaySyncBtn"
            icon="arrow-round"
            :disabled="isSyncInProgress"
            @click="$emit('synchronize')"
        >
            {{ $t("common.start_synchronization") }}
        </bm-dropdown-item-button>
    </bm-icon-dropdown>
</template>

<script>
import { ContainerType } from "./container";
import { BmIconDropdown, BmDropdownItemButton } from "@bluemind/ui-components";

export default {
    name: "ManageMyContainerMenu",
    components: { BmIconDropdown, BmDropdownItemButton },
    props: {
        container: {
            type: Object,
            required: true
        },
        isSyncInProgress: {
            type: Boolean,
            required: true
        }
    },
    computed: {
        isCalendarType() {
            return this.container.type === ContainerType.CALENDAR;
        },
        displaySyncBtn() {
            return this.isCalendarType && this.container.settings.type === "externalIcs";
        }
    }
};
</script>
