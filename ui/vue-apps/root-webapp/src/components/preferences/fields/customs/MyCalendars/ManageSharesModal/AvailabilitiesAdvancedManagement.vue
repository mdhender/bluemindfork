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
            <template v-slot="{ item }">
                <bm-color-badge v-if="item.settings.bm_color" :value="item.settings.bm_color" class="mr-1" />
                <div v-else class="empty d-inline-block" />
                {{ item.name }}
            </template>
        </bm-form-autocomplete-input>
        <h2 class="mt-4 mb-2">{{ $t("common.my_availabilities") }}</h2>
        <div>{{ $t("preferences.calendar.my_calendars.choose_calendar_for_my_availabilities") }}</div>
        <h1 v-for="cal in calendarsForMyAvailabilities" :key="cal" class="d-inline">
            <bm-badge
                pill
                :closeable="!isDefaultCalendar(cal)"
                class="mt-2 mr-2 align-items-center"
                @close="removeCalFromMyAvailabilities(cal)"
            >
                <bm-color-badge v-if="getColor(cal)" :value="getColor(cal)" class="mr-1" />
                <div v-else class="empty d-inline-block" />
                {{ getName(cal) }}
            </bm-badge>
        </h1>
    </div>
</template>

<script>
import { mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { BmBadge, BmColorBadge, BmFormAutocompleteInput, BmSpinner } from "@bluemind/styleguide";

export default {
    name: "AvailabilitiesAdvancedManagement",
    components: { BmBadge, BmColorBadge, BmFormAutocompleteInput, BmSpinner },
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
        ...mapState("preferences", ["myCalendars"])
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
            return uid === "calendar:Default:" + inject("UserSession").userId;
        },
        getName(uid) {
            return this.myCalendars.find(myCal => myCal.uid === uid).name;
        },
        getColor(uid) {
            return this.myCalendars.find(myCal => myCal.uid === uid).settings?.bm_color;
        }
    }
};
</script>

<style lang="scss">
.availabilities-advanced-management {
    div.empty {
        width: 20px;
        height: 20px;
    }
}
</style>
