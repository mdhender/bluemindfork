<template>
    <div class="pref-right-panel-header">
        <bm-navbar class="small-panel-header d-lg-none">
            <bm-navbar-back @click="SET_SELECTED_SECTION(null)" />
            <template v-if="!openedInMobile">
                <bm-navbar-title :title="selectedSection.name" />
                <bm-icon-button
                    class="ml-auto"
                    variant="compact-on-fill-primary"
                    size="lg"
                    icon="search"
                    @click="openedInMobile = true"
                />
            </template>
            <pref-search-input v-else class="flex-fill mx-3" />
        </bm-navbar>
        <div class="d-none d-lg-flex align-items-center large-panel-header">
            <pref-search-input class="my-4 w-50" />
            <bm-button-close size="lg" class="ml-auto mr-5" @click="$emit('close')" />
        </div>
    </div>
</template>

<script>
import PrefSearchInput from "./PrefSearchInput";
import { BmIconButton, BmButtonClose, BmNavbar, BmNavbarBack, BmNavbarTitle } from "@bluemind/ui-components";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefRightPanelHeader",
    components: { BmIconButton, BmButtonClose, BmNavbar, BmNavbarBack, BmNavbarTitle, PrefSearchInput },
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
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "../variables";

.pref-right-panel-header {
    .small-panel-header {
        padding-right: $sp-3;
    }
    .large-panel-header {
        padding-left: $prefs-padding-left;
        @include from-lg {
            padding-left: $prefs-padding-left-lg;
        }
    }
}
</style>
