<template>
    <div class="preferences position-absolute w-100 h-100 overlay d-flex z-index-500" @click="unlockOrClose">
        <bm-spinner v-if="!loaded" class="flex-fill align-self-center text-center" @click="lockClose = true" />
        <bm-container
            v-else
            fluid
            class="bg-surface visible-container"
            tabindex="0"
            @click="lockClose = true"
            @keydown.esc="closePreferences"
        >
            <section
                v-show="showMobileLeftPanel"
                class="mobile-left-panel-section mobile-only"
                @click="showMobileLeftPanel = false"
            >
                <pref-left-panel
                    :sections="SECTIONS"
                    @click.native.stop
                    @categoryClicked="showMobileLeftPanel = false"
                />
            </section>
            <bm-row class="h-100 flex-nowrap">
                <pref-left-panel :sections="SECTIONS" class="desktop-only" />
                <pref-right-panel
                    :sections="SECTIONS"
                    @close="closePreferences"
                    @showMobileLeftPanel="showMobileLeftPanel = true"
                />
            </bm-row>
        </bm-container>
    </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { REMOVE } from "@bluemind/alert.store";
import { generateDateTimeFormats } from "@bluemind/i18n";
import { inject } from "@bluemind/inject";
import Roles from "@bluemind/roles";
import { BmContainer, BmRow, BmSpinner } from "@bluemind/ui-components";

import getPreferenceSections from "./sections";
import PrefLeftPanel from "./PrefLeftPanel";
import PrefRightPanel from "./PrefRightPanel/PrefRightPanel";
import Navigation from "./mixins/Navigation";

export default {
    name: "Preferences",
    components: {
        PrefRightPanel,
        BmContainer,
        BmRow,
        PrefLeftPanel,
        BmSpinner
    },
    mixins: [Navigation],
    props: {
        applications: {
            required: true,
            type: Array
        }
    },
    data() {
        return { loaded: false, lockClose: false, showMobileLeftPanel: false };
    },
    computed: {
        ...mapState("preferences", ["selectedSectionId", "selectedCategoryId"]),
        ...mapState("settings", ["lang", "timeformat"]),
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
        const sections = getPreferenceSections(this);
        this.SET_SECTIONS(sections);

        const promises = [
            this.FETCH_USER_PASSWORD_LAST_CHANGE(),
            this.FETCH_SUBSCRIPTIONS().then(subscriptions => this.FETCH_CONTAINERS(subscriptions)) // FETCH_CONTAINERS action need subscriptions to be loaded
        ];
        let fetchSettingsPromise = this.FETCH_ALL_SETTINGS(this);
        if (inject("UserSession").roles.includes(Roles.SELF_CHANGE_MAILBOX_FILTER)) {
            fetchSettingsPromise = fetchSettingsPromise.then(() => this.FETCH_MAILBOX_FILTER(this.lang));
        }
        promises.push(fetchSettingsPromise);
        await Promise.all(promises);

        this.loaded = true;
        this.scrollOnLoad();
    },
    methods: {
        ...mapActions("alert", { REMOVE }),
        ...mapActions("preferences", [
            "FETCH_CONTAINERS",
            "FETCH_SUBSCRIPTIONS",
            "FETCH_USER_PASSWORD_LAST_CHANGE",
            "FETCH_MAILBOX_FILTER"
        ]),
        ...mapActions("settings", ["FETCH_ALL_SETTINGS"]),
        ...mapMutations("preferences", [
            "TOGGLE_PREFERENCES",
            "SET_SECTIONS",
            "SET_CURRENT_PATH",
            "SET_OFFSET",
            "SET_SEARCH"
        ]),
        async closePreferences() {
            const confirm = await this.checkUnsavedChanges();
            if (confirm) {
                this.$router.push({ hash: "" });
                this.TOGGLE_PREFERENCES();
                this.SET_OFFSET(0);
                this.SET_SEARCH("");
                this.cleanAlertsOnClose();
            }
        },
        async checkUnsavedChanges() {
            if (this.$store.getters["preferences/fields/HAS_CHANGED"]) {
                return await this.$bvModal.msgBoxConfirm(this.$t("preferences.leave_app.confirm"), {
                    title: this.$t("preferences.leave_app.confirm.title"),
                    cancelTitle: this.$t("common.cancel"),
                    okTitle: this.$t("preferences.leave_app.confirm.button")
                });
            }
            return true;
        },
        cleanAlertsOnClose() {
            this.$store.state.alert.forEach(alert => {
                if (alert.area.startsWith("pref-") && !alert.keepAfterClose) {
                    this.REMOVE(alert);
                }
            });
        },
        unlockOrClose() {
            if (!this.lockClose) {
                this.closePreferences();
            }
            this.lockClose = false;
        },
        async scrollOnLoad() {
            // @see https://bootstrap-vue.org/docs/directives/scrollspy#events
            this.$root.$on("bv::scrollspy::activate", hash => {
                const path = hash.replace("#preferences-", "");
                if (`${this.selectedSectionId}-${this.selectedCategoryId}` !== path) {
                    this.SET_CURRENT_PATH(path);
                }
                if (this.$route.hash !== hash) {
                    this.$router.push({ hash });
                }
            });
            await this.$nextTick();
            if (this.$route.hash && this.$route.hash.startsWith("#preferences-")) {
                const path = this.$route.hash.replace("#preferences-", "");
                this.SET_CURRENT_PATH(path);
                this.scrollTo(path);
            } else {
                this.SET_CURRENT_PATH("");
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

@media only screen {
    @include from-lg {
        .preferences .visible-container {
            width: $modal-xl;
            max-width: $modal-max-width;
            height: map-get($modal-heights, "lg", "xl");
            margin: auto;
            overflow: hidden;
            box-shadow: $box-shadow-lg;
        }
    }
}

.preferences {
    .mobile-left-panel-section {
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        right: 0;
        z-index: $zindex-modal;
        background-color: $modal-backdrop;

        display: flex;

        .pref-left-panel {
            width: 85%;
            max-width: base-px-to-rem(320);
            box-shadow: $box-shadow-lg;
        }
    }

    @include until-lg {
        .pref-right-panel {
            width: 100%;
        }
    }
}
</style>
