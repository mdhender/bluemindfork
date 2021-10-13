<template>
    <div class="pref-manage-my-calendars">
        <div class="row mb-1 px-2 text-secondary no-gutters">
            <bm-col cols="1" />
            <bm-col cols="8">{{ $t("common.label") }}</bm-col>
            <bm-col cols="2">{{ $t("common.synchronization") }}</bm-col>
            <bm-col cols="1" />
        </div>
        <bm-list-group class="border-top border-bottom">
            <bm-list-group-item
                v-for="(myCalendar, index) in myCalendars"
                :key="myCalendar.uid"
                class="row d-flex align-items-center no-gutters"
                :class="{ 'bg-extra-light': index % 2 === 0 }"
            >
                <bm-col cols="1">
                    <div :title="$t('preferences.calendar.my_calendars.default')" class="d-flex justify-content-center">
                        <bm-icon v-if="myCalendar.defaultContainer" icon="star-fill" size="lg" />
                    </div>
                </bm-col>
                <bm-col cols="8">
                    <bm-calendar-item :calendar="myCalendar" class="mx-2" />
                </bm-col>
                <bm-col cols="2">
                    <bm-form-checkbox
                        :checked="getOfflineSync(myCalendar)"
                        switch
                        @change="onOfflineSyncChange(myCalendar)"
                    />
                </bm-col>
                <bm-col cols="1">
                    <pref-manage-my-calendars-menu
                        :calendar="myCalendar"
                        :is-sync-in-progress="!!beingSynced[myCalendar.uid]"
                        @update="update(myCalendar)"
                        @manage-shares="manageShares(myCalendar)"
                        @import-ics="importIcs(myCalendar)"
                        @reset-data="resetData(myCalendar)"
                        @remove="remove(myCalendar)"
                        @synchronize-external-ics="synchronizeExternalIcs(myCalendar)"
                    />
                </bm-col>
            </bm-list-group-item>
        </bm-list-group>
        <bm-button variant="outline-secondary" class="my-3" @click="openCreateModal()">
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
import { BmButton, BmCol, BmFormCheckbox, BmIcon, BmListGroup, BmListGroupItem } from "@bluemind/styleguide";
import { retrieveTaskResult } from "@bluemind/task";
import { mapActions, mapMutations, mapState } from "vuex";

export default {
    name: "PrefManageMyCalendars",
    components: {
        BmButton,
        BmCalendarItem,
        BmCol,
        BmFormCheckbox,
        BmIcon,
        BmListGroup,
        BmListGroupItem,
        CreateOrUpdateCalendarModal,
        ImportIcsModal,
        ManageSharesModal,
        PrefManageMyCalendarsMenu
    },
    mixins: [PrefAlertsMixin],
    data() {
        return { beingSynced: {} };
    },
    computed: {
        ...mapState("preferences", ["myCalendars", "subscriptions"])
    },
    methods: {
        ...mapActions("preferences", ["SET_SUBSCRIPTIONS", "REMOVE_SUBSCRIPTIONS"]),
        ...mapMutations("preferences", ["REMOVE_PERSONAL_CALENDAR"]),
        onOfflineSyncChange(calendar) {
            const newOfflineSync = !this.getOfflineSync(calendar);
            if (newOfflineSync) {
                const subscription = calendarToSubscription(inject("UserSession"), {
                    ...calendar,
                    offlineSync: newOfflineSync
                });
                this.SET_SUBSCRIPTIONS([subscription]);
            } else {
                this.REMOVE_SUBSCRIPTIONS([calendar.uid]);
            }
        },
        getOfflineSync(calendar) {
            const isSubscribed = this.subscriptions.find(sub => sub.value.containerUid === calendar.uid);
            return isSubscribed ? isSubscribed.value.offlineSync : false;
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
    .list-group-item {
        border-bottom: none !important;
        .fa-star-fill {
            color: $primary;
        }
    }
}
</style>
