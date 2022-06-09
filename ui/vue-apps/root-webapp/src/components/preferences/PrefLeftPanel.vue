<template>
    <bm-col class="pref-left-panel d-lg-flex flex-column" cols="12" lg="2">
        <div class="p-3">
            <h2 class="d-none d-lg-block">
                <bm-label-icon icon="preferences">{{ $t("common.preference") }}</bm-label-icon>
            </h2>
            <div class="d-lg-none">
                <bm-button variant="inline-on-fill-primary" class="mr-auto" @click="$emit('close')">
                    <bm-icon icon="arrow-back" size="2x" />
                </bm-button>
                <h2 class="d-inline align-middle">{{ $t("common.preference") }}</h2>
            </div>
        </div>
        <pref-left-panel-nav :sections="sections" class="flex-grow-1" />
    </bm-col>
</template>

<script>
import { BmButton, BmCol, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import PrefLeftPanelNav from "./PrefLeftPanelNav";

export default {
    name: "PrefLeftPanel",
    components: { BmButton, BmIcon, BmLabelIcon, BmCol, PrefLeftPanelNav },
    props: {
        sections: {
            required: true,
            type: Array
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-left-panel {
    background-color: $fill-primary-bg;
    color: $fill-primary-fg;
}
</style>
