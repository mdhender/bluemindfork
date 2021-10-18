<template>
    <bm-col lg="10" cols="12" class="pref-right-panel d-lg-flex flex-column h-100">
        <div class="d-lg-none d-block pref-right-panel-header py-2 px-3">
            <bm-button variant="inline-light" class="d-lg-none btn-sm mr-auto" @click="SET_SELECTED_SECTION(null)">
                <bm-icon icon="arrow-back" size="2x" />
            </bm-button>
            <h2 class="d-inline text-white align-middle">{{ section.name }}</h2>
        </div>
        <bm-button-close class="align-self-end d-lg-block d-none mt-3 mx-3" @click="$emit('close')" />
        <pref-right-panel-nav :sections="sections" />
        <div class="border-bottom border-secondary" />
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <pref-content :sections="sections" />
        <div class="d-flex mt-auto pl-5 py-3 border-top border-secondary">
            <bm-button type="submit" variant="primary" :disabled="!HAS_CHANGED || HAS_ERROR" @click.prevent="SAVE">
                {{ $t("common.save") }}
            </bm-button>
            <bm-button type="reset" variant="simple-dark" class="ml-3" :disabled="!HAS_CHANGED" @click.prevent="CANCEL">
                {{ $t("common.cancel") }}
            </bm-button>
            <div v-if="STATUS === 'error'" class="ml-5 text-danger d-flex align-items-center font-weight-bold">
                <bm-icon icon="exclamation-circle" class="mr-1" /> {{ $t("preferences.save.error") }}
            </div>
            <div v-if="STATUS === 'saved'" class="ml-5 text-success d-flex align-items-center font-weight-bold">
                <bm-icon icon="exclamation-circle" class="mr-1" /> {{ $t("preferences.save.success") }}
            </div>
        </div>
    </bm-col>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { BmAlertArea, BmButton, BmButtonClose, BmCol, BmIcon } from "@bluemind/styleguide";
import { ERROR, REMOVE, WARNING } from "@bluemind/alert.store";

import PrefContent from "./PrefContent";
import PrefRightPanelNav from "./PrefRightPanelNav";
import NeedReconnectionAlert from "./Alerts/NeedReconnectionAlert";
import ReloadAppAlert from "./Alerts/ReloadAppAlert";

export default {
    name: "PrefRightPanel",
    components: {
        BmAlertArea,
        BmButton,
        BmButtonClose,
        BmCol,
        BmIcon,
        PrefContent,
        PrefRightPanelNav,
        NeedReconnectionAlert,
        ReloadAppAlert
    },
    props: {
        sections: {
            type: Array,
            default: null
        }
    },
    data() {
        return { status: "idle" };
    },

    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "pref-right-panel") }),
        ...mapState("session", ["settings"]),
        ...mapState("preferences", ["selectedSectionId", "sectionById", "mailboxFilter"]),
        ...mapGetters("preferences/fields", ["HAS_CHANGED", "HAS_ERROR", "IS_LOGOUT_NEEDED", "IS_RELOAD_NEEDED"]),
        ...mapGetters("preferences", ["STATUS"]),
        isReloadNeeded() {
            return this.IS_LOGOUT_NEEDED || this.IS_RELOAD_NEEDED;
        },
        section() {
            return this.sectionById[this.selectedSectionId] || {};
        }
    },
    watch: {
        isReloadNeeded() {
            const alert = {
                alert: { uid: "IS_RELOAD_NEEDED" },
                options: { area: "pref-right-panel", dismissible: false }
            };
            if (this.isReloadNeeded && this.IS_LOGOUT_NEEDED) {
                alert.name = "preferences.NEED_RECONNECTION";
                alert.options.renderer = "NeedReconnectionAlert";
                this.WARNING(alert);
            } else if (this.isReloadNeeded) {
                alert.name = "preferences.NEED_APP_RELOAD";
                alert.options.renderer = "ReloadAppAlert";
                this.WARNING(alert);
            } else {
                this.REMOVE(alert.alert);
            }
        }
    },
    methods: {
        ...mapMutations("preferences", ["SET_SELECTED_SECTION"]),
        ...mapActions("preferences", ["CANCEL", "SAVE"]),
        ...mapActions("alert", { ERROR, REMOVE, WARNING }),
        async saveSettings() {
            const oldSettings = JSON.parse(JSON.stringify(this.settings.remote));
            try {
                await this.SAVE_SETTINGS();
                this.manageAlertAfterSave(oldSettings, this.settings.local);
            } catch {
                this.SET_SETTINGS(oldSettings);
                throw new Error();
            }
        },
        async saveMailboxFilter() {
            const oldMailboxFilter = JSON.parse(JSON.stringify(this.mailboxFilter.remote));
            try {
                await this.SAVE_MAILBOX_FILTER();
            } catch {
                this.SET_MAILBOX_FILTER(oldMailboxFilter);
                throw new Error();
            }
        },
        manageAlertAfterSave(oldSettings, newSettings) {
            const needAppReload = ["lang", "mail_thread", "mail-application"];
            const needReconnection = ["default_app"];

            const showReloadAppAlert = needAppReload.some(setting => oldSettings[setting] !== newSettings[setting]);
            const showReconnectionAlert = needReconnection.some(
                setting => oldSettings[setting] !== newSettings[setting]
            );
            if (showReconnectionAlert) {
                this.showReconnectionAlert();
            } else if (showReloadAppAlert) {
                this.showReloadAppAlert();
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-right-panel {
    .pref-content-header {
        background-color: $info-dark;
    }
}
</style>
