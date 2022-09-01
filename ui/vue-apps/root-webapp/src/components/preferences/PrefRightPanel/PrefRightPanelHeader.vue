<template>
    <div class="pref-right-panel-header">
        <div class="d-flex d-lg-none py-4 px-3 align-items-center small-panel-header">
            <bm-icon-button
                variant="compact-on-fill-primary"
                size="lg"
                icon="arrow-back"
                @click="SET_SELECTED_SECTION(null)"
            />
            <template v-if="!openedInMobile">
                <div class="bold">{{ selectedSection.name }}</div>
                <bm-icon-button
                    class="ml-auto"
                    variant="compact-on-fill-primary"
                    size="lg"
                    icon="search"
                    @click="openedInMobile = true"
                />
            </template>
            <pref-search-input v-else class="flex-fill mx-3" />
        </div>
        <div class="d-none d-lg-flex align-items-center large-panel-header">
            <pref-search-input class="my-4 w-50" />
            <bm-button-close size="lg" class="ml-auto mr-3" @click="$emit('close')" />
        </div>
    </div>
</template>

<script>
import PrefSearchInput from "./PrefSearchInput";
import { BmIconButton, BmButtonClose } from "@bluemind/styleguide";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefRightPanelHeader",
    components: { BmIconButton, BmButtonClose, PrefSearchInput },
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
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";
@import "../_variables";

.pref-right-panel-header {
    .small-panel-header {
        background-color: $fill-primary-bg;
        color: $fill-primary-fg;
    }
    .large-panel-header {
        padding-left: $prefs-padding-left;
        @include from-lg {
            padding-left: $prefs-padding-left-lg;
        }
    }
}
</style>
