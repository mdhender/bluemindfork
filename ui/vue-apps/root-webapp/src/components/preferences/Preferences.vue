<template>
    <div class="preferences position-absolute w-100 h-100 overlay d-flex z-index-500" @click="unlockOrClose">
        <bm-spinner
            v-if="!loaded"
            class="flex-fill align-self-center text-center"
            :size="2.5"
            @click="lockClose = true"
        />
        <bm-container
            v-else
            fluid
            class="flex-fill bg-surface visible-container"
            tabindex="0"
            @click="lockClose = true"
            @keydown.esc="closePreferences"
        >
            <bm-row class="h-100">
                <pref-left-panel
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
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { REMOVE } from "@bluemind/alert.store";
import { generateDateTimeFormats } from "@bluemind/i18n";
import { inject } from "@bluemind/inject";
import Roles from "@bluemind/roles";
import { BmContainer, BmRow, BmSpinner } from "@bluemind/styleguide";

import getPreferenceSections from "./sections";
import SettingsL10N from "../../../l10n/preferences/";
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
    componentI18N: { messages: SettingsL10N },
    data() {
        return { loaded: false, lockClose: false };
    },
    computed: {
        ...mapState("preferences", { selectedSection: "selectedSectionId" }),
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

        await Promise.all([
            this.FETCH_USER_PASSWORD_LAST_CHANGE(),
            this.FETCH_ALL_SETTINGS().then(() => {
                if (inject("UserSession").roles.includes(Roles.SELF_CHANGE_MAILBOX_FILTER)) {
                    return this.FETCH_MAILBOX_FILTER(this.lang); // lang is set once all settings are loaded
                }
            }),
            this.FETCH_SUBSCRIPTIONS().then(subscriptions => this.FETCH_CONTAINERS(subscriptions)) // FETCH_CONTAINERS action need subscriptions to be loaded
        ]);

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
            "SET_SELECTED_SECTION",
            "SET_SECTIONS",
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
                    okTitle: this.$t("preferences.leave_app.confirm.button"),
                    okVariant: "contained-accent",
                    cancelVariant: "text",
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "cancel"
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
            this.$root.$on("bv::scrollspy::activate", path => {
                const newSectionId = path.split("-")[1];
                if (this.selectedSection !== newSectionId) {
                    this.SET_SELECTED_SECTION(newSectionId);
                }
                if (this.$route.hash !== path) {
                    this.$router.push({ hash: path });
                }
            });
            await this.$nextTick();
            if (this.$route.hash && this.$route.hash.startsWith("#preferences-")) {
                const path = this.$route.hash.replace("#preferences-", "");
                this.SET_SELECTED_SECTION(path.split("-").shift());
                this.scrollTo(path);
            } else {
                this.SET_SELECTED_SECTION("my_account");
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";

@media only screen {
    @include from-lg {
        .preferences .visible-container {
            max-width: 80%;
            margin-top: $sp-5;
            margin-bottom: $sp-5;
        }
    }
}
</style>
