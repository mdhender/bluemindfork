<template>
    <bm-spinner v-if="isLoading" class="d-flex justify-content-center" />
    <div v-else class="availabilities-advanced-management">
        <bm-form-group>
            <label for="availabilities-management-search-input" class="mb-1">
                {{ $t("preferences.calendar.my_calendars.add_calendar_to_my_availability") }}
            </label>
            <bm-form-autocomplete-input
                id="availabilities-management-search-input"
                v-model="searchedInput"
                :placeholder="$t('common.search')"
                icon="magnifier"
                left-icon
                :items="suggestions"
                @input="findSuggestions"
                @selected="onSelect"
            >
                <template #default="{ item }"><bm-calendar-item :calendar="item" /></template>
            </bm-form-autocomplete-input>
        </bm-form-group>
        <h3 class="mt-6 mb-4">{{ $t("common.my_availabilities") }}</h3>
        <div class="mb-4">{{ $t("preferences.calendar.my_calendars.choose_calendar_for_my_availabilities") }}</div>
        <div class="calendar-badges">
            <bm-calendar-badge
                v-for="calendarUid in calendarsForMyAvailabilities"
                :key="calendarUid"
                :calendar="getCalendar(calendarUid)"
                :closeable="!isDefaultCalendar(calendarUid)"
                @close="removeCalFromMyAvailabilities(calendarUid)"
            />
        </div>
    </div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { SUCCESS } from "@bluemind/alert.store";
import { BmFormGroup, BmFormAutocompleteInput, BmSpinner } from "@bluemind/ui-components";
import BmCalendarBadge from "../Calendars/BmCalendarBadge";
import BmCalendarItem from "../Calendars/BmCalendarItem";
import { SAVE_ALERT_MODAL } from "../../../../Alerts/defaultAlerts";

export default {
    name: "AvailabilitiesManagement",
    components: { BmCalendarBadge, BmCalendarItem, BmFormGroup, BmFormAutocompleteInput, BmSpinner },
    data() {
        return {
            isLoading: true,

            calendarsForMyAvailabilities: [],
            myDefaultCalContainerUid: "freebusy:" + inject("UserSession").userId,

            // for search autocomplete
            searchedInput: "",
            suggestions: []
        };
    },
    computed: {
        ...mapState("preferences", { myCalendars: state => state.containers.myCalendars })
    },
    async mounted() {
        this.isLoading = true;
        this.calendarsForMyAvailabilities = (
            await inject("FreebusyMgmtPersistence", this.myDefaultCalContainerUid).get()
        ).sort(uid => !this.isDefaultCalendar(uid));
        this.isLoading = false;
    },
    methods: {
        ...mapActions("alert", { SUCCESS }),
        findSuggestions() {
            if (this.searchedInput) {
                this.suggestions = this.myCalendars.filter(
                    cal =>
                        cal.name.toLowerCase().includes(this.searchedInput.toLowerCase()) &&
                        !this.calendarsForMyAvailabilities.includes(cal.uid)
                );
            } else {
                this.suggestions = this.myCalendars.filter(cal => !this.calendarsForMyAvailabilities.includes(cal.uid));
            }
        },
        onSelect(selected) {
            this.calendarsForMyAvailabilities.push(selected.uid);
            this.suggestions = [];
            this.searchedInput = "";
            this.saveMyAvailabilities();
        },

        saveMyAvailabilities() {
            inject("FreebusyMgmtPersistence", this.myDefaultCalContainerUid).set(this.calendarsForMyAvailabilities);
            this.SUCCESS(SAVE_ALERT_MODAL);
        },

        removeCalFromMyAvailabilities(uidToRemove) {
            const index = this.calendarsForMyAvailabilities.findIndex(uid => uid === uidToRemove);
            if (index !== -1) {
                this.calendarsForMyAvailabilities.splice(index, 1);
            }
            this.saveMyAvailabilities();
        },

        isDefaultCalendar(uid) {
            return this.getCalendar(uid).defaultContainer;
        },
        getCalendar(uid) {
            return this.myCalendars.find(myCal => myCal.uid === uid);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.availabilities-advanced-management {
    margin: 0 $sp-5;
    @include from-lg {
        margin: 0 $sp-6;
    }

    #availabilities-management-search-input .bm-form-input {
        max-width: base-px-to-rem(300);
    }

    .calendar-badges {
        display: flex;
        flex-wrap: wrap;
        gap: $sp-4 $sp-5;
    }
}
</style>
