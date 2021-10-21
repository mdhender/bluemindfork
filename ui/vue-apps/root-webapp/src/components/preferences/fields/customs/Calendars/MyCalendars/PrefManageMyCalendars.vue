<template>
    <div class="pref-manage-my-calendars">
        <bm-table
            :items="myCalendars"
            :fields="fields"
            :per-page="perPage"
            :current-page="currentPage"
            sort-by="defaultContainer"
            sort-desc
        >
            <template #cell(defaultContainer)="row">
                <div :title="$t('preferences.calendar.my_calendars.default')" class="text-center">
                    <bm-icon v-if="row.value" icon="star-fill" size="lg" />
                </div>
            </template>
            <template #cell(name)="row"><bm-calendar-item :calendar="row.item" class="mx-2" /></template>
            <template #cell(offlineSync)="row">
                <bm-form-checkbox :checked="row.value" switch @change="onOfflineSyncChange(row.item)" />
            </template>
            <template #cell(menu)="row">
                <pref-manage-my-calendars-menu
                    class="float-right"
                    :calendar="row.item"
                    :is-sync-in-progress="!!beingSynced[row.item.uid]"
                    @update="update(row.item)"
                    @manage-shares="manageShares(row.item)"
                    @import-ics="importIcs(row.item)"
                    @reset-data="resetData(row.item)"
                    @remove="remove(row.item)"
                    @synchronize-external-ics="synchronizeExternalIcs(row.item)"
                />
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" class="d-inline-flex" />
        <bm-button variant="outline-secondary" class="float-right" @click="openCreateModal()">
            {{ $t("preferences.calendar.my_calendars.create") }}
        </bm-button>
        <create-or-update-calendar-modal ref="create-or-update-calendar" />
        <import-ics-modal ref="import-ics" />
        <manage-shares-modal ref="manage-shares" />
    </div>
</template>

<script>
import calendarToSubscription from "../calendarToSubscription";
import BmCalendarItem from "../BmCalendarItem";
import CreateOrUpdateCalendarModal from "./CreateOrUpdateCalendarModal";
import ImportIcsModal from "./ImportIcsModal";
import ManageSharesModal from "../ManageSharesModal/ManageSharesModal";
import PrefAlertsMixin from "../../../../mixins/PrefAlertsMixin";
import PrefManageMyCalendarsMenu from "./PrefManageMyCalendarsMenu";
import { inject } from "@bluemind/inject";
import { BmButton, BmFormCheckbox, BmIcon, BmPagination, BmTable } from "@bluemind/styleguide";
import { retrieveTaskResult } from "@bluemind/task";
import { mapActions, mapMutations, mapState } from "vuex";

export default {
    name: "PrefManageMyCalendars",
    components: {
        BmButton,
        BmCalendarItem,
        BmFormCheckbox,
        BmIcon,
        BmPagination,
        BmTable,
        CreateOrUpdateCalendarModal,
        ImportIcsModal,
        ManageSharesModal,
        PrefManageMyCalendarsMenu
    },
    mixins: [PrefAlertsMixin],
    data() {
        return {
            beingSynced: {},
            currentPage: 1,
            perPage: 5,
            fields: [
                {
                    key: "defaultContainer",
                    headerTitle: this.$t("preferences.calendar.my_calendars.default"),
                    label: ""
                },
                {
                    key: "name",
                    label: this.$t("common.label")
                },
                {
                    key: "offlineSync",
                    label: this.$t("common.synchronization")
                },
                {
                    key: "menu",
                    headerTitle: this.$t("common.action"),
                    label: ""
                }
            ]
        };
    },
    computed: {
        ...mapState("preferences", ["myCalendars", "subscriptions"]),
        totalRows() {
            return this.myCalendars.length;
        }
    },
    methods: {
        ...mapActions("preferences", ["SET_SUBSCRIPTIONS", "REMOVE_SUBSCRIPTIONS"]),
        ...mapMutations("preferences", ["REMOVE_PERSONAL_CALENDAR", "UPDATE_PERSONAL_CALENDAR"]),
        async onOfflineSyncChange(calendar) {
            const updatedCalendar = { ...calendar, offlineSync: !calendar.offlineSync };
            const subscription = calendarToSubscription(inject("UserSession"), updatedCalendar);
            await this.SET_SUBSCRIPTIONS([subscription]);
            this.UPDATE_PERSONAL_CALENDAR(updatedCalendar);
        },
        openCreateModal() {
            this.$refs["create-or-update-calendar"].open();
        },

        // menu actions
        update(calendar) {
            this.$refs["create-or-update-calendar"].open(calendar);
        },
        manageShares(calendar) {
            this.$refs["manage-shares"].open(calendar);
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
            this.showSyncCalendarInProgress();
            const taskRef = await inject("ContainerSyncPersistence", calendar.uid).sync();
            const taskService = inject("TaskService", taskRef.id);
            retrieveTaskResult(taskService)
                .then(() => {
                    this.showSyncCalendarSuccess();
                })
                .catch(() => {
                    this.showSyncCalendarError();
                })
                .finally(() => {
                    this.beingSynced[calendar.uid] = false;
                });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-manage-my-calendars {
    .b-table .fa-star-fill {
        color: $primary;
    }
}
</style>
