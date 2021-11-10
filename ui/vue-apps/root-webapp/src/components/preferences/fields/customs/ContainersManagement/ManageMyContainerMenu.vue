<template>
    <bm-contextual-menu>
        <bm-dropdown-item-button icon="pencil" :disabled="isDefault && !isCalendarType" @click="$emit('update')">
            {{ $t("common.edit") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="share" @click="$emit('manage-shares')">
            {{ $t("common.share") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="upload" @click="$emit('import')">
            {{ $t("common.import") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="broom" @click="$emit('reset-data')">
            {{ $t("common.action.reset") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button icon="trash" :disabled="isDefault" @click="$emit('remove')">
            {{ $t("common.delete") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            v-if="displaySyncBtn"
            icon="loop"
            :disabled="isSyncInProgress"
            @click="$emit('synchronize')"
        >
            {{ $t("common.start_synchronization") }}
        </bm-dropdown-item-button>
    </bm-contextual-menu>
</template>

<script>
import { ContainerType, isDefault } from "./container";
import { BmContextualMenu, BmDropdownItemButton } from "@bluemind/styleguide";

export default {
    name: "ManageMyContainerMenu",
    components: { BmContextualMenu, BmDropdownItemButton },
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
        isDefault() {
            return isDefault(this.container.uid);
        },
        isCalendarType() {
            return this.container.type === ContainerType.CALENDAR;
        },
        displaySyncBtn() {
            return this.isCalendarType && this.container.settings.type === "externalIcs";
        }
    }
};
</script>
