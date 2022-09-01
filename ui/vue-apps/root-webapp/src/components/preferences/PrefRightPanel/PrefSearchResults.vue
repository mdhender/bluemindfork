<template>
    <bm-spinner v-if="isLoading" :size="2" class="m-auto" />
    <div v-else-if="results.length === 0" class="pref-search-results pref-empty-search-results">
        <div
            class="ml-6"
            :style="'background: url(' + emptyResultsIllustration + ') no-repeat left top; height: 177px;'"
        />
        <p>
            <i18n :path="'preferences.search.no_result'" tag="span">
                <template #pattern>
                    <span class="search-pattern">{{ SEARCH_PATTERN }}</span>
                </template>
            </i18n>
            <br />{{ $t("common.search.try_otherwise") }}
        </p>
    </div>
    <div v-else class="pref-search-results overflow-auto">
        <div class="d-flex justify-content-end pr-6">
            <bm-button variant="text" @click="toggleAll">
                {{ areAllExpanded ? $t("common.collapse_all") : $t("common.expand_all") }}
            </bm-button>
        </div>
        <bm-alert-area
            v-if="alerts.length > 0"
            class="border-top border-neutral"
            :alerts="alerts"
            stackable
            @remove="REMOVE"
        >
            <template v-slot="context"><component :is="context.alert.renderer" :alert="context.alert" /></template>
        </bm-alert-area>
        <div class="border-bottom border-neutral" />
        <template v-for="(group, index) in results">
            <div :key="group.id" class="group-header">
                <pref-section-icon :section="GET_SECTION(group.id)" :set="(isCollapsed = isGroupCollapsed(group.id))" />
                <bm-button-expand size="lg" :expanded="!isCollapsed" @click="toggleGroup(group.id)" />
                <pref-group ref="group" :group="group" :collapsed="isCollapsed" class="flex-fill" />
            </div>
            <div v-if="index !== results.length - 1" :key="index" class="border-bottom border-neutral" />
        </template>
    </div>
</template>

<script>
import emptyResultsIllustration from "../../../../assets/setting-empty-search-results.png";
import PrefGroup from "../PrefGroup";
import PrefSectionIcon from "../PrefSectionIcon";
import RightPanelAlerts from "../mixins/RightPanelAlerts";
import { BmButton, BmButtonExpand, BmSpinner } from "@bluemind/styleguide";
import { mapGetters } from "vuex";

export default {
    name: "PrefSearchResults",
    components: { BmButton, BmButtonExpand, BmSpinner, PrefGroup, PrefSectionIcon },
    mixins: [RightPanelAlerts],
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
        return { emptyResultsIllustration, expandedGroups: [] };
    },
    computed: {
        ...mapGetters("preferences", ["GET_SECTION", "SEARCH_PATTERN"]),
        areAllExpanded() {
            return this.expandedGroups.length === this.results.length;
        }
    },
    watch: {
        results() {
            this.expandedGroups = [];
        },
        isLoading() {
            this.highLight();
        },
        expandedGroups() {
            this.highLight();
        }
    },
    methods: {
        async highLight() {
            if (!this.isLoading && this.results.length > 0) {
                await this.$nextTick();
                this.$refs.group.forEach(groupRef => {
                    parseNodeAndHighlight(groupRef.$el, this.SEARCH_PATTERN);
                });
            }
        },
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
        }
    }
};

function parseNodeAndHighlight(node, search) {
    if (node.nodeType === Node.TEXT_NODE) {
        const matchRegex = new RegExp(search, "gi");
        const patternsToMark = [...node.textContent.matchAll(matchRegex)];
        if (patternsToMark.length > 0) {
            const replaceRegex = new RegExp("(" + search + ")", "gi");
            const innerHTML = node.textContent.replaceAll(replaceRegex, "<mark>$1</mark>");
            const newNode = document.createElement("span");
            newNode.innerHTML = innerHTML;
            node.parentNode.appendChild(newNode);
            node.remove();
        }
    }

    let i = 0;
    while (i < node.childNodes.length) {
        parseNodeAndHighlight(node.childNodes[i], search);
        i++;
    }
}
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";
@import "../_variables";

.pref-search-results {
    &.pref-empty-search-results {
        padding-left: $prefs-padding-left;
        @include from-lg {
            padding-left: $prefs-padding-left-lg;
        }
    }

    .group-header {
        display: flex;
        padding-left: $sp-6;
    }

    .pref-section-icon {
        margin-top: $pref-entry-name-padding-top + math.div($h3-line-height - $section-icon-size, 2);
    }
    .bm-button-expand {
        margin-top: $pref-entry-name-padding-top + math.div($h3-line-height - $icon-btn-height-lg, 2);
    }

    .search-pattern {
        color: $primary-fg-hi1;
        font-weight: $font-weight-bold;
        word-break: break-all;
    }
}
</style>
