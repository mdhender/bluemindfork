<template>
    <bm-breadcrumb class="pref-breadcrumb" :on-fill-primary="onFillPrimary">
        <bm-breadcrumb-item
            v-if="!searchResult"
            class="root-item"
            icon="preferences"
            text="Préférences"
            :active="!section"
            interactive
            @click="scrollToRoot"
        />
        <bm-breadcrumb-item
            v-if="section"
            class="section-item"
            :text="section.name"
            :active="!category"
            :interactive="!searchResult"
            @click="scrollTo(section)"
        >
            <template #icon>
                <bm-app-icon :icon-app="section.icon" />
            </template>
        </bm-breadcrumb-item>
        <bm-breadcrumb-item
            v-if="category"
            v-highlight="group?.name ? null : SEARCH_PATTERN"
            :icon="category.icon"
            :text="category.name"
            :active="!group?.name"
            :interactive="!searchResult"
        />
        <bm-breadcrumb-item v-if="group?.name" v-highlight="SEARCH_PATTERN" :text="group.name" active />
    </bm-breadcrumb>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";

import { BmBreadcrumb, BmBreadcrumbItem, Highlight } from "@bluemind/ui-components";

import BmAppIcon from "../../BmAppIcon";
import Navigation from "../mixins/Navigation";

export default {
    name: "PrefBreadcrumbs",
    components: { BmBreadcrumb, BmBreadcrumbItem, BmAppIcon },
    directives: { Highlight },
    mixins: [Navigation],
    props: {
        section: { type: Object, default: null },
        category: { type: Object, default: null },
        group: { type: Object, default: null },
        onFillPrimary: { type: Boolean, default: false },
        searchResult: { type: Boolean, default: false }
    },
    computed: {
        ...mapGetters("preferences", ["SEARCH_PATTERN"])
    },
    methods: {
        ...mapMutations("preferences", ["SET_CURRENT_PATH"]),

        scrollToRoot() {
            this.SET_CURRENT_PATH("");
            document.getElementById("scroll-area").scrollTop = 0;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-breadcrumb {
    .bm-breadcrumb-item {
        .bm-app-icon {
            position: relative;
            top: base-px-to-rem(2);
        }

        @include until-lg {
            // hide root item unless active
            &.root-item:not(.item-active) {
                &,
                & + .bm-breadcrumb-item::before {
                    display: none;
                }
            }

            // hide item label unless active
            &:not(.item-active) {
                .bm-icon,
                .bm-app-icon {
                    + div {
                        display: none;
                    }
                }

                .bm-icon {
                    color: $neutral-fg-lo1;
                }
            }
        }

        &.section-item {
            $app-icon-width: base-px-to-rem(22);
            min-width: 2 * $sp-2 + $app-icon-width;
            @include from-lg {
                min-width: 2 * $sp-3 + $app-icon-width + $breadcrumb-separator-width;
            }
        }
    }
}
</style>
