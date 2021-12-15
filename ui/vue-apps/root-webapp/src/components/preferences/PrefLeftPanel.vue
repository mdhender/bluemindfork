<template>
    <bm-col class="pref-left-panel text-white d-lg-flex flex-column" cols="12" lg="2">
        <div class="p-3">
            <h2 class="d-none d-lg-block">
                <bm-label-icon icon="preferences">{{ $t("common.preference") }}</bm-label-icon>
            </h2>
            <div class="d-lg-none">
                <bm-button variant="inline-light" class="mr-auto" @click="$emit('close')">
                    <bm-icon icon="arrow-back" size="2x" />
                </bm-button>
                <h2 class="d-inline align-middle">{{ $t("common.preference") }}</h2>
            </div>
        </div>
        <pref-left-panel-nav :sections="sections" class="flex-grow-1" />
        <div class="p-3">
            <bm-button class="text-white font-weight-bold text-left" variant="link" @click="goToOldPrefs">
                {{ $t("preferences.access_old_settings_app") }}
            </bm-button>
        </div>
    </bm-col>
</template>

<script>
import { BmButton, BmCol, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import PrefLeftPanelNav from "./PrefLeftPanelNav";

export default {
    name: "PrefLeftPanel",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon,
        BmCol,
        PrefLeftPanelNav
    },
    props: {
        sections: {
            required: true,
            type: Array
        }
    },
    methods: {
        async goToOldPrefs() {
            let confirm = true;
            if (this.$store.getters["session/SETTINGS_CHANGED"]) {
                confirm = await this.$bvModal.msgBoxConfirm(this.$t("preferences.access_old_settings_app.confirm"), {
                    title: this.$t("preferences.access_old_settings_app.confirm.title"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "cancel"
                });
            }

            if (confirm) {
                document.location.href = "/settings/";
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-left-panel {
    background-color: $info-dark;

    .bm-label-icon {
        color: $white;
    }
}
</style>
