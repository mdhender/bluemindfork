<template>
    <nav class="pref-left-panel-nav mt-3" :class="{ searching: HAS_SEARCH }" :aria-label="$t('preferences.menu.apps')">
        <bm-list-group v-bm-scrollspy:scroll-area="40">
            <template v-for="section in sections">
                <bm-list-group-item
                    ref="section"
                    :key="section.id"
                    class="section-title"
                    :class="{ 'section-active': isSectionActive(section) }"
                    role="button"
                    :to="anchor(section, true)"
                    @click="goTo(section)"
                >
                    <bm-app-icon :icon-app="section.icon" class="text-secondary mr-4" />
                    <div class="section-name text-truncate">{{ section.name }}</div>
                </bm-list-group-item>
                <div v-show="isSectionActive(section)" :key="`${section.id}-categories`" class="mb-6">
                    <bm-list-group-item
                        v-for="category in section.categories.filter(c => c.visible)"
                        :key="category.id"
                        class="category-title"
                        role="button"
                        :to="anchor(category, true)"
                        @click="
                            goTo(category);
                            $emit('categoryClicked');
                        "
                    >
                        <bm-icon :icon="category.icon" />
                        <div>{{ category.name }}</div>
                    </bm-list-group-item>
                </div>
            </template>
        </bm-list-group>
    </nav>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import { BmIcon, BmListGroup, BmListGroupItem, BmScrollspy } from "@bluemind/ui-components";
import Navigation from "./mixins/Navigation";
import BmAppIcon from "../BmAppIcon";

export default {
    name: "PrefLeftPanelNav",
    components: { BmIcon, BmListGroup, BmListGroupItem, BmAppIcon },
    directives: { BmScrollspy },
    mixins: [Navigation],
    props: {
        sections: {
            required: true,
            type: Array
        }
    },
    computed: {
        ...mapGetters("preferences", ["HAS_SEARCH"]),
        ...mapState("preferences", ["selectedSectionId", "offset"])
    },
    methods: {
        ...mapMutations("preferences", ["SET_SEARCH", "SET_CURRENT_PATH"]),
        async goTo(sectionOrCategory) {
            this.SET_SEARCH("");
            await this.$nextTick();
            this.scrollTo(sectionOrCategory.id);
        },
        isSectionActive(section) {
            return !this.HAS_SEARCH && this.offset !== 0 && section.id === this.selectedSectionId;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/focus";
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-left-panel-nav {
    $arrow-width: base-px-to-rem(12);
    $arrow-half-height: base-px-to-rem(10);

    .list-group-item {
        color: $fill-primary-fg;
        border: none;
        height: unset;
        &,
        &:focus,
        &:hover,
        &.active {
            background: none;
        }

        &:focus-visible {
            @include default-focus($fill-primary-fg);
            &:hover {
                @include default-focus($fill-primary-fg-hi1);
            }
        }
        &:hover {
            color: $fill-primary-fg-hi1;
        }

        &.section-title {
            padding: $sp-6;
            padding-right: $sp-4 + $arrow-width;
            @include large;
        }

        &.category-title {
            padding-top: $sp-4;
            padding-bottom: $sp-4;
            @include from-lg {
                padding-top: $sp-5;
                padding-bottom: $sp-5;
                padding-right: $sp-4 + $arrow-width;
            }
            $section-icon-width: 22px;
            $category-icon-width: map-get($icon-sizes, "md");
            padding-left: calc(#{$sp-6} + (#{$section-icon-width} - #{$category-icon-width}) / 2);
            gap: $sp-4;
            align-items: start;
        }
    }

    &:not(.searching) .list-group-item {
        &.section-title.section-active.router-link-active,
        &:not(.section-title).router-link-active {
            background-color: $fill-primary-bg-hi1;
            @include from-lg {
                &:after {
                    content: "";
                    position: absolute;
                    right: 0;
                    top: calc(50% - #{$arrow-half-height});
                    border-right: $arrow-width solid $fill-primary-fg;
                    border-top: $arrow-half-height solid transparent;
                    border-bottom: $arrow-half-height solid transparent;
                }
            }
        }

        &.section-title.section-active {
            @include large-bold;
        }
    }
}
</style>
