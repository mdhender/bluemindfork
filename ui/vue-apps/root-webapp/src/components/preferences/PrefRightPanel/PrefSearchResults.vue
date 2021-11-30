<template>
    <bm-spinner v-if="isLoading" :size="2" class="m-auto" />
    <div v-else-if="results.length === 0" class="pref-search-results ml-5">
        <div
            class="ml-4"
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
        <div class="d-flex justify-content-end">
            <bm-button variant="simple-dark" @click="toggleAll">
                {{ areAllExpanded ? $t("common.collapse_all") : $t("common.expand_all") }}
            </bm-button>
        </div>
        <div class="border-bottom border-secondary" />
        <template v-for="(group, index) in results">
            <div :key="group.id" class="d-flex">
                <pref-section-icon
                    :section="GET_SECTION(group.id)"
                    class="mt-4"
                    :set="(isCollapsed = isGroupCollapsed(group.id))"
                />
                <pref-group ref="group" :group="group" :collapsed="isCollapsed" class="flex-fill" />
                <bm-button variant="inline-dark" class="align-self-start mt-4 mr-3" @click="toggleGroup(group.id)">
                    <bm-icon :icon="isCollapsed ? 'chevron' : 'chevron-up'" size="2x" />
                </bm-button>
            </div>
            <div v-if="index !== results.length - 1" :key="index" class="border-bottom border-secondary" />
        </template>
    </div>
</template>

<script>
import emptyResultsIllustration from "../../../../assets/setting-empty-search-results.png";
import PrefGroup from "../PrefGroup";
import PrefSectionIcon from "../PrefSectionIcon";
import { BmButton, BmIcon, BmSpinner } from "@bluemind/styleguide";
import { mapGetters } from "vuex";

export default {
    name: "PrefSearchResults",
    components: { BmButton, BmIcon, BmSpinner, PrefGroup, PrefSectionIcon },
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
    if (node.nodeType === 3) {
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
@import "~@bluemind/styleguide/css/variables";

.pref-search-results .search-pattern {
    color: $info-dark;
    font-weight: $font-weight-bold;
    word-break: break-all;
}
</style>
