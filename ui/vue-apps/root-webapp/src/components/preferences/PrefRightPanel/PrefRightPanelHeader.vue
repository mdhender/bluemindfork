<template>
    <div class="pref-right-panel-header">
        <bm-navbar class="mobile-only">
            <template v-if="showMobileSearchInput">
                <bm-navbar-back @click="showMobileSearchInput = false" />
                <pref-search-input class="flex-fill ml-5 mr-3" variant="inline-on-fill-primary" />
            </template>
            <template v-else>
                <bm-navbar-back @click="$emit('close')" />
                <bm-dropdown
                    variant="text-on-fill-primary"
                    class="text-truncate"
                    size="lg"
                    @show.prevent="$emit('showMobileLeftPanel')"
                >
                    <template #button-content>
                        <pref-breadcrumb
                            :section="offset === 0 ? null : selectedSection"
                            :category="offset === 0 ? null : selectedCategory"
                            on-fill-primary
                            :interactive="false"
                        />
                    </template>
                </bm-dropdown>
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
            <pref-search-input :class="{ large: HAS_SEARCH }" resettable />
            <pref-breadcrumb
                v-if="!HAS_SEARCH"
                :section="offset === 0 ? null : selectedSection"
                :category="offset === 0 ? null : selectedCategory"
            />
            <bm-button-close size="lg" class="ml-auto mr-5" @click="$emit('close')" />
        </div>
    </div>
</template>

<script>
import PrefBreadcrumb from "./PrefBreadcrumb";
import PrefSearchInput from "./PrefSearchInput";
import { BmDropdown, BmIconButton, BmButtonClose, BmNavbar, BmNavbarBack } from "@bluemind/ui-components";
import { mapMutations, mapState, mapGetters } from "vuex";

export default {
    name: "PrefRightPanelHeader",
    components: {
        BmDropdown,
        BmIconButton,
        BmButtonClose,
        BmNavbar,
        BmNavbarBack,
        PrefBreadcrumb,
        PrefSearchInput
    },
    props: {
        selectedSection: {
            type: Object,
            default: null
        },
        selectedCategory: {
            type: Object,
            default: null
        }
    },
    data() {
        return { showMobileSearchInput: false };
    },
    computed: {
        ...mapGetters("preferences", ["HAS_SEARCH"]),
        ...mapState("preferences", ["selectedSectionId", "offset"])
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
        ...mapMutations("preferences", ["SET_SEARCH"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-right-panel-header {
    z-index: $zindex-sticky;

    .large-panel-header {
        height: base-px-to-rem(44);
        padding-left: $sp-6;
        background-color: $surface-hi1;
        box-shadow: $box-shadow;

        .pref-search-input {
            flex: none;
            width: base-px-to-rem(160);
            margin-right: $sp-5;

            &.large {
                width: base-px-to-rem(400);
            }
        }
    }

    > .navbar {
        .bm-dropdown {
            min-width: 0;
            flex: 1;
            align-self: stretch;

            .dropdown-toggle {
                min-width: 0;
                justify-content: start;
                padding-left: $sp-4;
                padding-right: $sp-4;
            }
        }
    }
}
</style>
