<template>
    <div class="preferences position-absolute w-100 h-100 overlay d-flex z-index-500" @click="closePreferences">
        <global-events @keydown.esc="closePreferences" />
        <div
            v-if="!areSettingsLoaded"
            class="position-absolute h-100 w-100 d-flex align-items-center z-index-200 text-center overlay"
            @click.stop
        >
            <bm-spinner class="flex-fill" :size="2.5" />
        </div>
        <bm-container v-else fluid class="flex-fill bg-surface m-lg-5" @click.stop>
            <bm-row class="h-100">
                <pref-left-panel :user="user" :sections="SECTIONS" :class="selectedSection ? 'd-none' : ''" />
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

import getPreferenceSections from "./PreferenceSections";
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
    computed: {
        ...mapState("preferences", { selectedSection: "selectedSectionCode" }),
        ...mapState("session", { areSettingsLoaded: ({ settings }) => settings.loaded }),
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
    created() {
        const sections = getPreferenceSections(this.applications, inject("UserSession").roles, inject("i18n"));
        this.SET_SECTIONS(sections);
        this.FETCH_USER_PASSWORD_LAST_CHANGE();
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
        ...mapActions("preferences", ["FETCH_USER_PASSWORD_LAST_CHANGE"]),
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES", "SET_SELECTED_SECTION", "SET_SECTIONS", "SET_OFFSET"]),
        ...mapMutations("session", ["ROLLBACK_LOCAL_SETTINGS"]),
        closePreferences() {
            this.ROLLBACK_LOCAL_SETTINGS();
            this.$router.push({ hash: "" });
            this.TOGGLE_PREFERENCES();
            this.SET_OFFSET(0);
        }
    }
};

function parseSectionId(sectionId) {
    const splitSectionId = sectionId.split("-");
    return { sectionCode: splitSectionId[1], categoryCode: splitSectionId[2] };
}
</script>
