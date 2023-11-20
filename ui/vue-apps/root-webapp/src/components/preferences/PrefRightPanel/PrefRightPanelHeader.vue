<template>
    <div class="pref-right-panel-header">
        <bm-navbar class="mobile-only">
            <template v-if="showMobileSearchInput">
                <bm-navbar-back @click="showMobileSearchInput = false" />
                <pref-search-input class="flex-fill ml-5 mr-3" variant="inline-on-fill-primary" />
            </template>
            <template v-else>
                <bm-navbar-back @click="SET_SELECTED_SECTION(null)" />
                <bm-navbar-title :title="selectedSection.name" />
                <bm-icon-button
                    class="mx-3"
                    variant="compact-on-fill-primary"
                    size="lg"
                    icon="search"
                    @click="showMobileSearchInput = true"
                />
            </template>
        </bm-navbar>
        <div class="desktop-only d-flex align-items-center large-panel-header">
            <pref-search-input class="my-4 w-50" resettable />
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
    components: {
        BmIconButton,
        BmButtonClose,
        BmNavbar,
        BmNavbarBack,
        BmNavbarTitle,
        PrefSearchInput
    },
    props: {
        selectedSection: {
            type: Object,
            required: true
        }
    },
    data() {
        return { showMobileSearchInput: false };
    },
    computed: {
        ...mapState("preferences", ["selectedSectionId"])
    },
    watch: {
        selectedSectionId() {
            this.showMobileSearchInput = false;
        },
        showMobileSearchInput(value) {
            if (value === false) {
                this.SET_SEARCH("");
            }
        }
    },
    methods: {
        ...mapMutations("preferences", ["SET_SELECTED_SECTION", "SET_SEARCH"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "../variables";

.pref-right-panel-header {
    .large-panel-header {
        padding-left: $prefs-padding-left-lg;
    }
}
</style>
