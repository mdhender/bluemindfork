<template>
    <div class="pref-right-panel-header">
        <div class="d-flex d-lg-none py-2 px-3 align-items-center small-panel-header">
            <bm-button variant="inline-on-fill-primary" @click="SET_SELECTED_SECTION(null)">
                <bm-icon icon="arrow-back" size="2x" />
            </bm-button>
            <template v-if="!openedInMobile">
                <h2>{{ selectedSection.name }}</h2>
                <bm-button class="ml-auto" variant="inline-on-fill-primary" @click="openedInMobile = true">
                    <bm-icon icon="search" size="2x" />
                </bm-button>
            </template>
            <pref-search-input v-else class="flex-fill mx-3" />
        </div>
        <div class="d-none d-lg-flex align-items-center large-panel-header">
            <pref-search-input class="my-3 w-25" style="margin-left: 4rem;" />
            <bm-button-close size="lg" class="ml-auto mr-3" @click="$emit('close')" />
        </div>
    </div>
</template>

<script>
import PrefSearchInput from "./PrefSearchInput";
import { BmButton, BmButtonClose, BmIcon } from "@bluemind/styleguide";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefRightPanelHeader",
    components: { BmButton, BmButtonClose, BmIcon, PrefSearchInput },
    props: {
        selectedSection: {
            type: Object,
            required: true
        }
    },
    data() {
        return { openedInMobile: false };
    },
    computed: {
        ...mapState("preferences", ["selectedSectionId"])
    },
    watch: {
        selectedSectionId() {
            this.openedInMobile = false;
        }
    },
    methods: {
        ...mapMutations("preferences", ["SET_SELECTED_SECTION"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-right-panel-header {
    .small-panel-header {
        background-color: $fill-primary-bg;

        h2 {
            color: $fill-primary-fg;
        }
    }
}
</style>
