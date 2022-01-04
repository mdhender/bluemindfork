<template>
    <bm-spinner v-if="isLoading" :size="2" class="d-flex justify-content-center" />
    <div v-else class="availabilities-advanced-management ml-4 mb-5">
        {{ $t("preferences.calendar.my_calendars.add_calendar_to_my_availability") }}
        <bm-form-autocomplete-input
            v-model="searchedInput"
            :placeholder="$t('common.search')"
            icon="search"
            class="w-50 mt-2"
            left-icon
            resettable
            :items="suggestions"
            @input="findSuggestions"
            @selected="onSelect"
        >
            <template v-slot="{ item }"><bm-calendar-item :calendar="item" /></template>
        </bm-form-autocomplete-input>
        <h2 class="mt-4 mb-2">{{ $t("common.my_availabilities") }}</h2>
        <div class="mb-2">{{ $t("preferences.calendar.my_calendars.choose_calendar_for_my_availabilities") }}</div>
        <bm-calendar-badge
            v-for="calendarUid in calendarsForMyAvailabilities"
            :key="calendarUid"
            :calendar="getCalendar(calendarUid)"
            :closeable="!isDefaultCalendar(calendarUid)"
            @close="removeCalFromMyAvailabilities(calendarUid)"
        />
    </div>
</template>

<script>
import BmCalendarBadge from "../Calendars/BmCalendarBadge";
import BmCalendarItem from "../Calendars/BmCalendarItem";
import { isDefault } from "../container";
import { inject } from "@bluemind/inject";
import { BmFormAutocompleteInput, BmSpinner } from "@bluemind/styleguide";
import { mapState } from "vuex";

export default {
    name: "AvailabilitiesManagement",
    components: { BmCalendarBadge, BmCalendarItem, BmFormAutocompleteInput, BmSpinner },
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
        },

        removeCalFromMyAvailabilities(uidToRemove) {
            const index = this.calendarsForMyAvailabilities.findIndex(uid => uid === uidToRemove);
            if (index !== -1) {
                this.calendarsForMyAvailabilities.splice(index, 1);
            }
            this.saveMyAvailabilities();
        },

        isDefaultCalendar(uid) {
            return isDefault(uid);
        },
        getCalendar(uid) {
            return this.myCalendars.find(myCal => myCal.uid === uid);
        }
    }
};
</script>
