<template>
    <div id="scroll-area" class="pref-sections scroller-y" @scroll="({ target }) => SET_OFFSET(target.scrollTop)">
        <div
            v-for="(section, index) in sections"
            :id="section.id"
            :key="section.id"
            class="pref-section"
            :class="{ 'last-section': index === sections.length - 1 }"
        >
            <div :id="anchor(section)" class="section-heading">
                <bm-app-icon :icon-app="section.icon" />
                <h1>{{ section.name }}</h1>
            </div>
            <pref-category
                v-for="category in section.categories.filter(c => c.visible)"
                :key="category.id"
                :category="category"
            />
            <hr v-if="index < sections.length - 1" />
        </div>
    </div>
</template>

<script>
import { mapMutations } from "vuex";
import PrefCategory from "./PrefCategory";
import BmAppIcon from "../BmAppIcon";
import Navigation from "./mixins/Navigation";

export default {
    name: "PrefSections",
    components: { PrefCategory, BmAppIcon },
    mixins: [Navigation],
    props: {
        sections: {
            type: Array,
            required: true
        }
    },
    methods: {
        ...mapMutations("preferences", ["SET_OFFSET"])
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-sections {
    position: relative;
    background-color: $surface;
    z-index: 1;
    padding-top: 41px; // v-bm-scrollspy offset + 1px to make sure 1st section gets activated when navigated to

    .pref-section {
        padding-left: $sp-5 + $sp-3;
        padding-right: $sp-5 + $sp-3;
        @include from-lg {
            padding-left: $sp-6;
            padding-right: $sp-7;
        }

        &.last-section {
            // Ensure last group can scroll until the top of the screen for scrollspy to work correctly:
            $last-group-approx-min-height: base-px-to-rem(370);
            padding-bottom: calc(100vh - #{$last-group-approx-min-height});
            @include from-lg {
                padding-bottom: calc(#{map-get($modal-heights, "lg", "xl")} - #{$last-group-approx-min-height});
            }
        }

        .section-heading {
            display: flex;
            align-items: start;
            gap: $sp-5;
            padding-top: $sp-6;
            padding-bottom: $sp-3;

            .bm-app-icon {
                svg {
                    $size: base-px-to-rem(44);
                    width: $size;
                    height: $size;
                    @include from-lg {
                        $size: base-px-to-rem(50);
                        width: $size;
                        height: $size;
                    }
                }
            }

            h1 {
                color: $secondary-fg;
                margin: 0;
                padding-top: base-px-to-rem(8);
                @include from-lg {
                    padding-top: base-px-to-rem(6);
                    padding-bottom: base-px-to-rem(4);
                }
            }
        }

        > hr {
            margin: 0;
            border: none;
            padding-top: $sp-7 + $sp-6;
            border-bottom: 1px solid $secondary-fg;
            margin-bottom: $sp-7 + $sp-6;
            @include from-lg {
                margin-bottom: $sp-7;
                margin-left: $sp-7 + $sp-6;
                margin-right: $sp-7 + $sp-6;
            }
        }
    }
}
</style>
