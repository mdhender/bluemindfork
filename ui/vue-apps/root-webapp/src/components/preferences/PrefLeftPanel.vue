<template>
    <bm-col class="pref-left-panel d-lg-flex flex-column" cols="12" lg="2">
        <div class="p-6">
            <div class="d-none d-lg-block bold">
                <bm-label-icon icon="preferences">{{ $t("common.preference") }}</bm-label-icon>
            </div>
            <div class="d-lg-none">
                <bm-icon-button
                    variant="compact-on-fill-primary"
                    size="lg"
                    class="mr-auto"
                    icon="arrow-back"
                    @click="$emit('close')"
                />
                <div class="d-inline align-middle bold">{{ $t("common.preference") }}</div>
            </div>
        </div>
        <pref-left-panel-nav :sections="sections" class="flex-grow-1" />
    </bm-col>
</template>

<script>
import { BmIconButton, BmCol, BmLabelIcon } from "@bluemind/styleguide";
import PrefLeftPanelNav from "./PrefLeftPanelNav";

export default {
    name: "PrefLeftPanel",
    components: { BmIconButton, BmLabelIcon, BmCol, PrefLeftPanelNav },
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
    padding: 0 !important;
}
</style>
