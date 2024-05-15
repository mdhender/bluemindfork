<template>
    <bm-spinner v-if="isLoading" class="m-auto" />
    <div v-else-if="results.length === 0" class="pref-search-results pref-empty-search-results">
        <bm-illustration value="spider" size="md" />
        <p>
            <i18n :path="'preferences.search.no_result'" tag="span">
                <template #pattern>
                    <span class="search-pattern">{{ SEARCH_PATTERN }}</span>
                </template>
            </i18n>
            <br />{{ $t("common.search.try_otherwise") }}
        </p>
    </div>
    <div v-else class="pref-search-results scroller-y">
        <bm-button
            class="toggle-all-button"
            variant="text"
            :icon="areAllExpanded ? 'collapse-vertical' : 'expand'"
            @click="toggleAll"
        >
            {{ areAllExpanded ? $t("common.collapse_all") : $t("common.expand_all") }}
        </bm-button>
        <hr />
        <template v-for="(group, index) in results">
            <div :key="`header-${index}`" class="group-header">
                <bm-button-expand
                    :set="(isCollapsed = isGroupCollapsed(group.id))"
                    :expanded="!isCollapsed"
                    size="sm"
                    @click="toggleGroup(group.id)"
                />
                <pref-breadcrumb
                    v-bind="GET_SECTION_AND_CATEGORY(group.id)"
                    :group="group"
                    search-result
                    @click.native="toggleGroup(group.id)"
                />
            </div>
            <pref-group
                :key="`body-${index}`"
                ref="group"
                v-highlight="SEARCH_PATTERN"
                :group="group"
                :collapsed="isCollapsed"
                no-heading
                class="flex-fill"
                :class="{ 'last-group': index === results.length - 1 }"
            />
            <hr v-if="separatorNeededAfterIndex(index)" :key="`hr-${index}`" />
        </template>
    </div>
</template>

<script>
import PrefGroup from "../PrefGroup";
import PrefBreadcrumb from "./PrefBreadcrumb";
import { BmButton, BmButtonExpand, BmIllustration, BmSpinner, Highlight } from "@bluemind/ui-components";
import { mapGetters } from "vuex";

export default {
    name: "PrefSearchResults",
    components: { BmButton, BmButtonExpand, BmIllustration, BmSpinner, PrefGroup, PrefBreadcrumb },
    directives: { Highlight },
    props: {
        isLoading: {
            type: Boolean,
            required: true
        },
        results: {
            type: Array,
            required: true
        }
    },
    data() {
        return { expandedGroups: [] };
    },
    computed: {
        ...mapGetters("preferences", ["GET_SECTION_AND_CATEGORY", "SEARCH_PATTERN"]),
        areAllExpanded() {
            return this.expandedGroups.length === this.results.length;
        }
    },
    watch: {
        results() {
            this.expandedGroups = [];
        }
    },
    methods: {
        isGroupCollapsed(groupId) {
            return this.expandedGroups.findIndex(id => groupId === id) === -1;
        },
        toggleGroup(groupId) {
            const index = this.expandedGroups.findIndex(id => groupId === id);
            if (index !== -1) {
                this.expandedGroups.splice(index, 1);
            } else {
                this.expandedGroups.push(groupId);
            }
        },
        toggleAll() {
            if (this.areAllExpanded) {
                this.expandedGroups = [];
            } else {
                this.expandedGroups = this.results.map(group => group.id);
            }
        },
        separatorNeededAfterIndex(index) {
            const getCategory = result => result.id.substring(0, result.id.indexOf("."));
            return (
                index < this.results.length - 1 &&
                getCategory(this.results[index]) !== getCategory(this.results[index + 1])
            );
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-search-results {
    height: 100%;
    padding-top: $sp-6;

    &.pref-empty-search-results {
        padding: $sp-7 $sp-6;

        display: flex;
        flex-direction: column;
        gap: $sp-6;

        align-items: center;
        @include from-lg {
            align-items: start;
            padding-left: $sp-7 + $sp-6;
        }
    }

    $expand-btn-width-sm: $icon-btn-width-compact-sm;
    $expand-btn-width: base-px-to-rem(54);

    .bm-button.toggle-all-button {
        .bm-icon {
            $icon-width: map-get($icon-sizes, "sm");
            $padding-x: math.div($expand-btn-width-sm - $icon-width, 2);
            padding-left: $padding-x;
            padding-right: $padding-x;
            @include from-lg {
                $padding-x: math.div($expand-btn-width - $icon-width, 2);
                padding-left: $padding-x;
                padding-right: $padding-x;
            }
        }
        margin-bottom: $sp-5;

        @include until-lg {
            gap: $sp-3;
        }
    }

    .group-header {
        display: flex;
        align-items: center;

        @include from-lg {
            .bm-button-expand.btn-sm {
                width: $expand-btn-width;
            }
        }
        .pref-breadcrumb {
            cursor: pointer;
            user-select: none;
            @include until-lg {
                padding-right: $sp-2;
            }
        }

        padding-bottom: $sp-5;
    }

    .pref-group {
        padding-top: $sp-5;
        padding-left: $sp-5 + $sp-3;
        padding-right: $sp-5 + $sp-3;
        @include from-lg {
            padding-left: $sp-7 + $sp-6;
            padding-right: $sp-7;
        }

        &.last-group {
            padding-bottom: base-px-to-rem(240);
        }
    }

    > hr {
        margin: 0;
        border: none;
        padding-top: $sp-6;
        border-bottom: 1px solid $secondary-fg;
        margin-bottom: $sp-7;
        margin-left: $expand-btn-width-sm + $sp-3;
        margin-right: $expand-btn-width-sm + $sp-3;
        @include from-lg {
            margin-left: $expand-btn-width + $sp-3;
            margin-right: $expand-btn-width + $sp-3;
        }
    }

    .search-pattern {
        color: $primary-fg-hi1;
        @include bold;
        word-break: break-all;
    }
}
</style>
