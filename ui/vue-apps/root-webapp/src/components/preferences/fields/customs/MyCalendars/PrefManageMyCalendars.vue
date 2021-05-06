<template>
    <div class="pref-manage-my-calendars">
        <div class="row mb-1 px-2 text-secondary">
            <bm-col cols="9">{{ $t("common.label") }}</bm-col>
            <bm-col cols="2">{{ $t("common.synchronization") }}</bm-col>
            <bm-col cols="1" />
        </div>
        <bm-list-group class="border-top border-bottom">
            <bm-list-group-item
                v-for="myCalendar in myCalendars"
                :key="myCalendar.uid"
                class="row d-flex align-items-center"
            >
                <bm-col cols="9">
                    <bm-color-badge v-if="myCalendar.settings.bm_color" :value="myCalendar.settings.bm_color" />
                    <div v-else class="empty d-inline-block" />
                    {{ myCalendar.name }}
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
                        @update="update(myCalendar)"
                        @manage-shares="manageShares"
                        @import-ics="importIcs"
                        @reset-data="resetData(myCalendar)"
                        @remove="remove(myCalendar)"
                    />
                </bm-col>
            </bm-list-group-item>
        </bm-list-group>
        <bm-button variant="outline-secondary" class="my-3" @click="openCreateModal()">
            {{ $t("preferences.calendar.my_calendars.create") }}
        </bm-button>
        <create-or-update-calendar-modal ref="create-or-update-calendar" />
    </div>
</template>

<script>
import PrefManageMyCalendarsMenu from "./PrefManageMyCalendarsMenu";
import CreateOrUpdateCalendarModal from "./CreateOrUpdateCalendarModal";
import calendarToSubscription from "./calendarToSubscription";
import { inject } from "@bluemind/inject";
import { BmButton, BmCol, BmColorBadge, BmFormCheckbox, BmListGroup, BmListGroupItem } from "@bluemind/styleguide";
import { mapActions, mapMutations, mapState } from "vuex";

export default {
    name: "PrefManageMyCalendars",
    components: {
        BmButton,
        BmCol,
        BmColorBadge,
        BmFormCheckbox,
        BmListGroup,
        BmListGroupItem,
        CreateOrUpdateCalendarModal,
        PrefManageMyCalendarsMenu
    },
    computed: {
        ...mapState("preferences", ["myCalendars", "subscriptions"])
    },
    methods: {
        ...mapActions("preferences", ["ADD_SUBSCRIPTIONS", "REMOVE_SUBSCRIPTIONS"]),
        ...mapMutations("preferences", ["REMOVE_PERSONAL_CALENDAR"]),
        onOfflineSyncChange(calendar) {
            if (!this.getOfflineSync(calendar)) {
                const subscription = calendarToSubscription(inject("UserSession"), calendar.uid, calendar.name);
                this.ADD_SUBSCRIPTIONS([subscription]);
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
        manageShares() {
            // TODO
        },
        importIcs() {
            // TODO
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
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-manage-my-calendars {
    .list-group-item {
        border-bottom: none !important;
        & div.empty {
            width: 20px;
            height: 20px;
        }
    }
}
</style>
