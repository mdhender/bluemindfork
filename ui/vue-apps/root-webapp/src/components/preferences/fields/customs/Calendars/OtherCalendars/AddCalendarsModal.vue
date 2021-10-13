<template>
    <bm-modal
        v-model="show"
        centered
        lazy
        :title="$t('preferences.calendar.other_calendars.add_calendars')"
        :cancel-title="$t('common.cancel')"
        :ok-title="okTitle"
        :ok-disabled="selected.length === 0"
        body-class=""
        @ok="subscribe"
    >
        <bm-spinner v-if="loadingStatus === 'LOADING'" :size="2" class="d-flex justify-content-center" />
        <template v-else>
            <div v-if="selected.length > 0" class="mb-3">
                <bm-calendar-badge
                    v-for="calendar in selected"
                    :key="calendar.uid"
                    :calendar="calendar"
                    closeable
                    @close="removeFromSelected(calendar)"
                />
            </div>
            <bm-form-input
                v-model="pattern"
                :placeholder="$t('preferences.calendar.other_calendars.search')"
                icon="search"
                resettable
                left-icon
                :aria-label="$t('preferences.calendar.other_calendars.search')"
                autocomplete="off"
                @reset="pattern = ''"
            />
            <template v-if="suggested.length > 0">
                <bm-table
                    :items="suggested"
                    :fields="fields"
                    :per-page="perPage"
                    :current-page="currentPage"
                    sort-by="name"
                    class="mt-2 mb-3"
                    @row-clicked="toggleSelected"
                >
                    <template #cell(selected)="row">
                        <bm-form-checkbox :checked="isSelected(row.item)" @change="toggleSelected(row.item)" />
                    </template>
                    <template #cell(name)="row"><bm-calendar-item :calendar="row.item" /></template>
                    <template #cell(ownerDisplayname)="row">
                        <span class="font-italic text-secondary">
                            {{ $t("common.shared_by", { name: row.value }) }}
                        </span>
                    </template>
                </bm-table>
                <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" />
            </template>
            <div v-else class="mt-2">{{ $t("preferences.calendar.other_calendars.nothing_to_add") }}</div>
        </template>
    </bm-modal>
</template>

<script>
import BmCalendarBadge from "../BmCalendarBadge";
import BmCalendarItem from "../BmCalendarItem";
import calendarToSubscription from "../calendarToSubscription";
import { inject } from "@bluemind/inject";
import { BmFormCheckbox, BmFormInput, BmModal, BmPagination, BmSpinner, BmTable } from "@bluemind/styleguide";
import { mapActions, mapMutations, mapState } from "vuex";

export default {
    name: "AddCalendarsModal",
    components: {
        BmCalendarBadge,
        BmCalendarItem,
        BmFormCheckbox,
        BmFormInput,
        BmModal,
        BmPagination,
        BmSpinner,
        BmTable
    },
    data() {
        return {
            show: false,
            loadingStatus: "IDLE",
            selected: [],
            pattern: "",
            allReadableCalendars: [],
            currentPage: 1,
            perPage: 10,
            fields: [
                {
                    key: "selected",
                    sortable: true,
                    headerTitle: this.$t("common.selection"),
                    label: ""
                },
                {
                    key: "name",
                    sortable: true,
                    headerTitle: this.$t("common.label"),
                    label: ""
                },
                {
                    key: "ownerDisplayname",
                    headerTitle: this.$t("common.shared_by"),
                    label: ""
                }
            ]
        };
    },
    computed: {
        ...mapState("preferences", ["otherCalendars"]),
        okTitle() {
            return this.$tc("preferences.calendar.other_calendars.add_n_calendars", this.selected.length, {
                count: this.selected.length
            });
        },
        suggested() {
            const realPattern = this.pattern.toLowerCase();
            return this.allReadableCalendars.filter(
                cal =>
                    cal.name.toLowerCase().includes(realPattern) ||
                    cal.ownerDisplayname.toLowerCase().includes(realPattern)
            );
        },
        totalRows() {
            return this.suggested.length;
        }
    },
    methods: {
        ...mapActions("preferences", ["SET_SUBSCRIPTIONS"]),
        ...mapMutations("preferences", ["ADD_OTHER_CALENDARS"]),
        async open() {
            this.loadingStatus = "LOADING";
            this.show = true;
            this.selected = [];
            this.currentPage = 1;
            this.pattern = "";
            this.allReadableCalendars = (await inject("ContainersPersistence").all({ type: "calendar" })).filter(
                calendar =>
                    calendar.owner !== inject("UserSession").userId &&
                    this.otherCalendars.findIndex(otherCal => otherCal.uid === calendar.uid) === -1
            );
            this.loadingStatus = "LOADED";
        },
        async subscribe() {
            const calendars = this.selected.map(selectedCal => ({ ...selectedCal, offlineSync: true }));
            const subscriptions = calendars.map(cal => calendarToSubscription(inject("UserSession"), cal));
            await this.SET_SUBSCRIPTIONS(subscriptions);
            this.ADD_OTHER_CALENDARS(this.selected);
        },

        removeFromSelected(calendar) {
            const index = this.selected.findIndex(selectedContact => selectedContact.uid === calendar.uid);
            if (index !== -1) {
                this.selected.splice(index, 1);
            }
        },
        toggleSelected(calendar) {
            const index = this.selected.findIndex(selectedCal => selectedCal.uid === calendar.uid);
            if (index === -1) {
                this.selected.push({ ...calendar });
            } else {
                this.selected.splice(index, 1);
            }
        },
        isSelected(calendar) {
            return this.selected.findIndex(selectedCal => selectedCal.uid === calendar.uid) !== -1;
        }
    }
};
</script>
