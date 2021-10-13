<template>
    <div class="pref-manage-other-calendars">
        <bm-form-input
            v-model="pattern"
            class="mt-1 mb-3"
            :placeholder="$t('common.filter')"
            icon="filter"
            resettable
            left-icon
            :aria-label="$t('common.filter')"
            autocomplete="off"
            @reset="pattern = ''"
        />
        <bm-table :items="filtered" :fields="fields" :per-page="perPage" :current-page="currentPage" sort-by="name">
            <template #cell(name)="row"><bm-calendar-item :calendar="row.item" class="mx-2" /></template>
            <template #cell(ownerDisplayname)="row">
                <span class="font-italic text-secondary">{{ $t("common.shared_by", { name: row.value }) }}</span>
            </template>
            <template #cell(offlineSync)="row">
                <bm-form-checkbox :checked="row.value" switch @change="onOfflineSyncChange(row.item)" />
            </template>
            <template #cell(action)="row">
                <bm-button v-if="isManaged(row.item)" variant="inline" @click="openShareModal(row.item)">
                    <bm-icon icon="share" size="lg" />
                </bm-button>
                <bm-button v-else variant="inline" @click="removeSubscription(row.item)">
                    <bm-icon icon="trash" size="lg" />
                </bm-button>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" class="d-inline-flex" />
        <bm-button variant="outline-secondary" class="float-right" @click="openAddCalendarsModal()">
            {{ $t("preferences.calendar.other_calendars.add_calendars") }}
        </bm-button>
        <manage-shares-modal ref="manage-shares" />
        <add-calendars-modal ref="add-calendars" />
    </div>
</template>

<script>
import calendarToSubscription from "../calendarToSubscription";
import AddCalendarsModal from "./AddCalendarsModal";
import BmCalendarItem from "../BmCalendarItem";
import ManageSharesModal from "../ManageSharesModal/ManageSharesModal";
import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import { BmButton, BmFormCheckbox, BmFormInput, BmIcon, BmPagination, BmTable } from "@bluemind/styleguide";
import { mapActions, mapMutations, mapState } from "vuex";

export default {
    name: "PrefManageOtherCalendars",
    components: {
        AddCalendarsModal,
        BmButton,
        BmCalendarItem,
        BmFormCheckbox,
        BmFormInput,
        BmIcon,
        BmPagination,
        BmTable,
        ManageSharesModal
    },
    data() {
        return {
            currentPage: 1,
            perPage: 5,
            pattern: "",
            fields: [
                {
                    key: "name",
                    sortable: true,
                    label: this.$t("common.label")
                },
                {
                    key: "ownerDisplayname",
                    headerTitle: this.$t("common.shared_by"),
                    label: ""
                },
                {
                    key: "offlineSync",
                    label: this.$t("common.synchronization"),
                    sortable: true
                },
                {
                    key: "action",
                    headerTitle: this.$t("preferences.calendar.other_calendars.action"),
                    label: "",
                    class: "action"
                }
            ]
        };
    },
    computed: {
        ...mapState("preferences", ["otherCalendars"]),
        filtered() {
            const realPattern = this.pattern.toLowerCase();
            return this.otherCalendars.filter(
                cal =>
                    cal.name.toLowerCase().includes(realPattern) ||
                    cal.ownerDisplayname.toLowerCase().includes(realPattern)
            );
        },
        totalRows() {
            return this.filtered.length;
        }
    },
    methods: {
        ...mapActions("preferences", ["SET_SUBSCRIPTIONS", "REMOVE_SUBSCRIPTIONS"]),
        ...mapMutations("preferences", ["REMOVE_OTHER_CALENDAR", "SET_CALENDAR_OFFLINE_SYNC"]),
        isManaged(calendar) {
            return calendar.verbs.some(verb => verb === Verb.All || verb === Verb.Manage);
        },

        // actions on calendar
        async onOfflineSyncChange(calendar) {
            const newOfflineSync = !calendar.offlineSync;
            const subscription = calendarToSubscription(inject("UserSession"), {
                ...calendar,
                offlineSync: !calendar.offlineSync
            });
            await this.SET_SUBSCRIPTIONS([subscription]);
            this.SET_CALENDAR_OFFLINE_SYNC({ uid: calendar.uid, offlineSync: newOfflineSync });
        },
        openShareModal(calendar) {
            this.$refs["manage-shares"].open(calendar);
        },
        removeSubscription(calendar) {
            this.REMOVE_SUBSCRIPTIONS([calendar.uid]);
            this.REMOVE_OTHER_CALENDAR(calendar.uid);
        },

        openAddCalendarsModal() {
            this.$refs["add-calendars"].open();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-manage-other-calendars {
    .b-table {
        .action {
            float: right;
        }
        td {
            vertical-align: middle;
        }
    }
}
</style>
