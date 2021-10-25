<template>
    <div class="preferences position-absolute w-100 h-100 overlay d-flex z-index-500" @click="unlockOrClose">
        <global-events @keydown.esc="closePreferences" />
        <bm-spinner
            v-if="!isLoaded"
            class="flex-fill align-self-center text-center"
            :size="2.5"
            @click="lockClose = true"
        />
        <bm-container v-else fluid class="flex-fill bg-surface m-lg-5" @click="lockClose = true">
            <bm-row class="h-100">
                <pref-left-panel
                    :user="user"
                    :sections="SECTIONS"
                    :class="selectedSection ? 'd-none' : ''"
                    @close="closePreferences"
                />
                <pref-right-panel
                    :class="selectedSection ? 'd-flex' : 'd-none'"
                    :sections="SECTIONS"
                    @close="closePreferences"
                />
            </bm-row>
        </bm-container>
    </div>
</template>

<script>
import GlobalEvents from "vue-global-events";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { generateDateTimeFormats } from "@bluemind/i18n";
import { inject } from "@bluemind/inject";
import { BmContainer, BmRow, BmSpinner } from "@bluemind/styleguide";

import getPreferenceSections from "./sections";
import SettingsL10N from "../../../l10n/preferences/";
import PrefLeftPanel from "./PrefLeftPanel";
import PrefRightPanel from "./PrefRightPanel";
import PrefMixin from "./mixins/PrefMixin";

export default {
    name: "Preferences",
    components: {
        GlobalEvents,
        PrefRightPanel,
        BmContainer,
        BmRow,
        PrefLeftPanel,
        BmSpinner
    },
    mixins: [PrefMixin],
    props: {
        applications: {
            required: true,
            type: Array
        },
        user: {
            required: true,
            type: Object
        }
    },
    componentI18N: { messages: SettingsL10N },
    data() {
        return { isLoaded: false, lockClose: false };
    },
    computed: {
        ...mapState("preferences", { selectedSection: "selectedSectionCode" }),
        ...mapState("session", {
            lang: ({ settings }) => settings.remote && settings.remote.lang,
            timeformat: ({ settings }) => settings.remote && settings.remote.timeformat
        }),
        ...mapGetters("preferences", ["SECTIONS"])
    },
    watch: {
        timeformat(newValue) {
            const dateTimeFormats = generateDateTimeFormats(newValue);
            Object.entries(dateTimeFormats).forEach(entry => {
                this.$root.$i18n.setDateTimeFormat(entry[0], entry[1]);
            });
        }
    },
    async created() {
        const sections = getPreferenceSections(this.applications, inject("UserSession").roles, inject("i18n"));
        this.SET_SECTIONS(sections);

        await Promise.all([
            this.FETCH_USER_PASSWORD_LAST_CHANGE(),
            this.FETCH_ALL_SETTINGS().then(() => this.FETCH_MAILBOX_FILTER(this.lang)), // lang is set once all settings are loaded
            this.FETCH_SUBSCRIPTIONS().then(() => this.FETCH_CONTAINERS()) // FETCH_CONTAINERS action need subscriptions to be loaded
        ]);

        this.isLoaded = true;
    },
    async mounted() {
        if (this.$route.hash && this.$route.hash.startsWith("#preferences-")) {
            const { sectionCode, categoryCode } = parseSectionId(this.$route.hash);
            this.SET_SELECTED_SECTION(sectionCode);
            this.scrollTo(this.categoryId(sectionCode, categoryCode));
        } else {
            this.SET_SELECTED_SECTION("main");
        }

        // @see https://bootstrap-vue.org/docs/directives/scrollspy#events
        this.$root.$on("bv::scrollspy::activate", sectionId => {
            this.SET_SELECTED_SECTION(parseSectionId(sectionId).sectionCode);
            if (this.$route.hash !== sectionId) {
                this.$router.push({ hash: sectionId });
            }
        });
    },
    methods: {
        ...mapActions("preferences", [
            "FETCH_CONTAINERS",
            "FETCH_SUBSCRIPTIONS",
            "FETCH_USER_PASSWORD_LAST_CHANGE",
            "FETCH_MAILBOX_FILTER"
        ]),
        ...mapActions("session", ["FETCH_ALL_SETTINGS"]),
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES", "SET_SELECTED_SECTION", "SET_SECTIONS", "SET_OFFSET"]),
        ...mapMutations("session", ["ROLLBACK_LOCAL_SETTINGS"]),
        closePreferences() {
            this.ROLLBACK_LOCAL_SETTINGS();
            this.$router.push({ hash: "" });
            this.TOGGLE_PREFERENCES();
            this.SET_OFFSET(0);
        },
        unlockOrClose() {
            if (!this.lockClose) {
                this.closePreferences();
            }
            this.lockClose = false;
        }
    }
};

function parseSectionId(sectionId) {
    const splitSectionId = sectionId.split("-");
    return { sectionCode: splitSectionId[1], categoryCode: splitSectionId[2] };
}
</script>
