<template>
    <containers-management
        class="pref-manage-my-calendars"
        :containers="myCalendars"
        :container-type="containerType"
        :can-filter="false"
        :default-container-field="defaultContainerField"
        :has-share-column="false"
        sort-by="defaultContainer"
        sort-desc
        :default-action-modal="false"
        @action-btn-clicked="openCreateModal"
        @update="UPDATE_PERSONAL_CALENDAR"
        @remove="REMOVE_PERSONAL_CALENDAR"
    >
        <template v-slot:default-container-column="{ isDefault }">
            <div :title="$t('preferences.calendar.my_calendars.default')" class="text-center">
                <bm-icon v-if="isDefault" icon="star-fill" size="lg" />
            </div>
        </template>
        <template v-slot:item="{ container }"><bm-calendar-item :calendar="container" /></template>
        <template v-slot:action="{ container, openShareModal }">
            <pref-manage-my-calendars-menu
                class="float-right"
                :calendar="container"
                :is-sync-in-progress="!!beingSynced[container.uid]"
                @update="update(container)"
                @manage-shares="openShareModal(container)"
                @import-ics="importIcs(container)"
                @reset-data="resetData(container)"
                @remove="remove(container)"
                @synchronize-external-ics="synchronizeExternalIcs(container)"
            />
        </template>
        <template v-slot:action-btn-content>
            <bm-icon icon="plus" /> {{ $t("preferences.calendar.my_calendars.create") }}
        </template>
        <template v-slot:additionnal-modals>
            <create-or-update-calendar-modal ref="create-or-update-calendar" />
            <import-ics-modal ref="import-ics" />
        </template>
    </containers-management>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";

import { inject } from "@bluemind/inject";
import { BmIcon } from "@bluemind/styleguide";
import { retrieveTaskResult } from "@bluemind/task";
import { ERROR, LOADING, REMOVE, SUCCESS } from "@bluemind/alert.store";

import { ContainerType } from "../../container";
import BaseField from "../../../../../mixins/BaseField";
import BmCalendarItem from "../BmCalendarItem";
import ContainersManagement from "../../ContainersManagement";
import CreateOrUpdateCalendarModal from "./CreateOrUpdateCalendarModal";
import ImportIcsModal from "./ImportIcsModal";
import PrefManageMyCalendarsMenu from "./PrefManageMyCalendarsMenu";

const ALERT = {
    alert: {
        name: "preferences.sync_calendar",
        uid: "SYNC_CALENDAR_UID"
    },
    options: { area: "pref-right-panel", renderer: "DefaultAlert" }
};

export default {
    name: "PrefManageOtherCalendars",
    components: {
        BmCalendarItem,
        BmIcon,
        ContainersManagement,
        CreateOrUpdateCalendarModal,
        ImportIcsModal,
        PrefManageMyCalendarsMenu
    },
    mixins: [BaseField],
    data() {
        return {
            beingSynced: {},
            containerType: ContainerType.CALENDAR,
            defaultContainerField: {
                key: "defaultContainer",
                headerTitle: this.$t("preferences.calendar.my_calendars.default"),
                label: ""
            }
        };
    },
    computed: {
        ...mapState("preferences", ["myCalendars"])
    },
    methods: {
        ...mapActions("alert", { ERROR, LOADING, REMOVE, SUCCESS }),
        ...mapActions("preferences", ["REMOVE_SUBSCRIPTIONS"]),
        ...mapMutations("preferences", ["REMOVE_PERSONAL_CALENDAR", "UPDATE_PERSONAL_CALENDAR"]),
        openCreateModal() {
            this.$refs["create-or-update-calendar"].open();
        },
        update(calendar) {
            this.$refs["create-or-update-calendar"].open(calendar);
        },
        importIcs(calendar) {
            this.$refs["import-ics"].open(calendar);
        },
        async resetData(calendar) {
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t("preferences.calendar.my_calendars.reset_data"), {
                title: this.$t("common.action.reset"),
                okTitle: this.$t("common.action.reset"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                await inject("CalendarPersistence", calendar.uid).reset();
            }
        },
        async remove(calendar) {
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t("preferences.calendar.my_calendars.remove"), {
                title: this.$t("common.delete"),
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                await inject("CalendarsMgmtPersistence").remove(calendar.uid);
                this.REMOVE_PERSONAL_CALENDAR(calendar.uid);
                this.REMOVE_SUBSCRIPTIONS([calendar.uid]);
            }
        },
        async synchronizeExternalIcs(calendar) {
            this.beingSynced[calendar.uid] = true;
            this.LOADING(ALERT);
            const taskRef = await inject("ContainerSyncPersistence", calendar.uid).sync();
            const taskService = inject("TaskService", taskRef.id);
            retrieveTaskResult(taskService)
                .then(() => {
                    this.SUCCESS(ALERT);
                })
                .catch(() => {
                    this.ERROR(ALERT);
                })
                .finally(() => {
                    this.beingSynced[calendar.uid] = false;
                });
        }
    }
};
</script>
