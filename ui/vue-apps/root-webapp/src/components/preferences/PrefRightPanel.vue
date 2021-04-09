<template>
    <bm-col lg="10" cols="12" class="pref-right-panel d-lg-flex flex-column h-100">
        <div class="d-lg-none d-block pref-right-panel-header py-2 px-3">
            <bm-button variant="inline-light" class="d-lg-none btn-sm mr-auto" @click="SET_SELECTED_SECTION(null)">
                <bm-icon icon="arrow-back" size="2x" />
            </bm-button>
            <h2 class="d-inline text-white align-middle">{{ sectionName }}</h2>
        </div>
        <bm-button-close class="align-self-end d-lg-block d-none mt-3 mx-3" @click="$emit('close')" />
        <pref-right-panel-nav :sections="sections" />
        <div class="border-bottom border-secondary" />
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <pref-content :sections="sections" :local-user-settings="settings.local" />
        <div class="d-flex mt-auto pl-5 py-3 border-top border-secondary">
            <bm-button type="submit" variant="primary" :disabled="!SETTINGS_CHANGED" @click.prevent="save">
                {{ $t("common.save") }}
            </bm-button>
            <bm-button
                type="reset"
                variant="simple-dark"
                class="ml-3"
                :disabled="!SETTINGS_CHANGED"
                @click.prevent="cancel"
            >
                {{ $t("common.cancel") }}
            </bm-button>
            <div v-if="status === 'error'" class="ml-5 text-danger d-flex align-items-center font-weight-bold">
                <bm-icon icon="exclamation-circle" class="mr-1" /> {{ $t("preferences.save.error") }}
            </div>
            <div v-if="status === 'saved'" class="ml-5 text-success d-flex align-items-center font-weight-bold">
                <bm-icon icon="exclamation-circle" class="mr-1" /> {{ $t("preferences.save.success") }}
            </div>
        </div>
    </bm-col>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { BmAlertArea, BmButton, BmButtonClose, BmCol, BmIcon } from "@bluemind/styleguide";

import PrefAlertsMixin from "./mixins/PrefAlertsMixin";
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
    mixins: [PrefAlertsMixin],
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
        ...mapState("session", ["settings"]),
        ...mapState("preferences", { selectedSection: "selectedSectionCode", sectionByCode: "sectionByCode" }),
        ...mapGetters("session", ["SETTINGS_CHANGED"]),
        sectionName() {
            const section = this.sectionByCode[this.selectedSection];
            return section ? section.name : "";
        }
    },
    methods: {
        ...mapActions("session", ["SAVE_SETTINGS"]),
        ...mapMutations("preferences", ["SET_SELECTED_SECTION"]),
        ...mapMutations("session", ["SET_SETTINGS"]),
        cancel() {
            this.SET_SETTINGS(this.settings.remote);
        },
        async save() {
            this.status = "saving";
            const oldSettings = JSON.parse(JSON.stringify(this.settings.remote));
            try {
                await this.SAVE_SETTINGS();
                this.manageAlertAfterSave(oldSettings, this.settings.local);
                this.status = "saved";
            } catch {
                this.SET_SETTINGS(oldSettings);
                this.status = "error";
            }
        },
        manageAlertAfterSave(oldSettings, newSettings) {
            const needAppReload = ["lang"];
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
    [disabled] {
        opacity: 0.5;
    }
}
</style>
