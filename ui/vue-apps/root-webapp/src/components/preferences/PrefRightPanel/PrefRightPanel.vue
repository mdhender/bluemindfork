<template>
    <div class="pref-right-panel h-100" tabindex="-1">
        <pref-right-panel-header
            :selected-section="section"
            :selected-category="category"
            @close="$emit('close')"
            @showMobileLeftPanel="$emit('showMobileLeftPanel')"
        />
        <pref-sections v-show="!HAS_SEARCH" ref="sections" :sections="sections" />
        <pref-search-results v-if="HAS_SEARCH" :results="searchResults" :is-loading="isSearchLoading" />
        <div class="bottom-area">
            <pref-alert-area />
            <transition name="slide-fade"><pref-right-panel-footer @saved="$el.focus()" /></transition>
        </div>
    </div>
</template>

<script>
import debounce from "lodash.debounce";
import { mapActions, mapGetters, mapState } from "vuex";

import { ERROR, REMOVE, WARNING } from "@bluemind/alert.store";
import { matchPattern } from "@bluemind/string";

import PrefRightPanelFooter from "./PrefRightPanelFooter";
import PrefRightPanelHeader from "./PrefRightPanelHeader";
import PrefAlertArea from "../PrefAlertArea";
import PrefSections from "../PrefSections";
import PrefSearchResults from "./PrefSearchResults";

export default {
    name: "PrefRightPanel",
    components: {
        PrefRightPanelFooter,
        PrefRightPanelHeader,
        PrefAlertArea,
        PrefSections,
        PrefSearchResults
    },
    props: {
        sections: {
            type: Array,
            default: null
        }
    },
    data() {
        return {
            searchResults: [],
            isSearchLoading: false,
            searchGroups: debounce(() => {
                const groupsFromTextNodes = getGroupsFromTextNodes(this.$refs["sections"], this.SEARCH_PATTERN).map(
                    this.GET_GROUP
                );
                const groupsFromKeywords = getGroupsFromKeywords(this.SEARCH_PATTERN, this.sections);
                const groupMap = new Map();
                groupsFromTextNodes.concat(groupsFromKeywords).forEach(group => {
                    groupMap.set(group.id, group);
                });
                this.searchResults = Array.from(groupMap.values());
                this.isSearchLoading = false;
            }, 500),
            warnings: []
        };
    },
    computed: {
        ...mapState("preferences", ["selectedSectionId", "selectedCategoryId", "sectionById"]),
        ...mapGetters("preferences/fields", [
            "ERRORS",
            "IS_LOGOUT_NEEDED",
            "IS_RELOAD_NEEDED",
            "NOT_VALID_PREFERENCES"
        ]),
        ...mapGetters("preferences", ["GET_GROUP", "HAS_SEARCH", "SEARCH_PATTERN", "GROUP_BY_FIELD_ID"]),
        isReloadNeeded() {
            return this.IS_LOGOUT_NEEDED || this.IS_RELOAD_NEEDED;
        },
        section() {
            return this.sectionById[this.selectedSectionId];
        },
        category() {
            return this.section?.categories?.find(c => c.id === `${this.selectedSectionId}.${this.selectedCategoryId}`);
        }
    },
    watch: {
        IS_LOGOUT_NEEDED() {
            if (this.IS_LOGOUT_NEEDED) {
                const logoutAlert = { ...alert };
                logoutAlert.name = "preferences.NEED_RECONNECTION";
                logoutAlert.options.renderer = "NeedReconnectionAlert";
                this.WARNING(logoutAlert);
            }
        },
        IS_RELOAD_NEEDED() {
            if (this.IS_RELOAD_NEEDED && !this.IS_LOGOUT_NEEDED) {
                const reloadAlert = { ...alert };
                reloadAlert.name = "preferences.NEED_APP_RELOAD";
                reloadAlert.options.renderer = "ReloadAppAlert";
                this.WARNING(reloadAlert);
            }
        },
        SEARCH_PATTERN() {
            if (this.HAS_SEARCH) {
                this.isSearchLoading = true;
                this.searchGroups();
            } else {
                this.searchResults = [];
            }
        },
        ERRORS(fieldIds) {
            fieldIds.forEach(fieldId => {
                const group = this.GROUP_BY_FIELD_ID(fieldId);
                const alert = {
                    alert: { uid: group.id, payload: { group } },
                    options: { area: "pref-right-panel", renderer: "SaveErrorAlert", dismissible: true }
                };
                this.ERROR(alert);
            });
        },
        NOT_VALID_PREFERENCES(fieldIds) {
            const warnings = [];
            fieldIds.forEach(fieldId => {
                const group = this.GROUP_BY_FIELD_ID(fieldId);
                if (!warnings.includes(group.id)) {
                    warnings.push(group.id);
                    const alert = {
                        alert: { uid: group.id, payload: { group } },
                        options: { area: "pref-right-panel", renderer: "NotValidAlert", dismissible: true }
                    };
                    this.WARNING(alert);
                }
            });
            const toRemoveWarnings = this.warnings.filter(groupId => !warnings.includes(groupId));
            toRemoveWarnings.forEach(groupId => this.REMOVE({ uid: groupId }));
            this.warnings = warnings;
        }
    },
    methods: {
        ...mapActions("alert", { ERROR, REMOVE, WARNING })
    }
};

const alert = {
    alert: { uid: "IS_RELOAD_OR_LOGOUT_NEEDED" },
    options: { area: "pref-right-panel", dismissible: false, keepAfterClose: true }
};

function getGroupsFromTextNodes(ref, pattern) {
    const matchingGroupIds = [];
    const groups = ref.$el.getElementsByClassName("pref-group");
    for (let group of groups) {
        if (doesNodeMatch(group, pattern)) {
            matchingGroupIds.push(group.id);
        }
    }
    return matchingGroupIds;
}

function getGroupsFromKeywords(pattern, sections) {
    return sections
        .flatMap(section => section.categories)
        .flatMap(category => category.groups)
        .filter(group => {
            const groupKeywords = group.fields.flatMap(field => field.keywords);
            return groupKeywords.find(keyword => keyword.toLowerCase().includes(pattern));
        });
}

function doesNodeMatch(node, pattern) {
    if (node.nodeType === Node.TEXT_NODE && matchPattern(pattern, node.textContent)) {
        return true;
    }
    let i = 0;
    while (i < node.childNodes.length) {
        if (doesNodeMatch(node.childNodes[i], pattern)) {
            return true;
        }
        i++;
    }
    return false;
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-right-panel {
    display: flex;
    flex-direction: column;
    overflow: hidden;

    padding: 0 !important;

    width: 100%;
    @include from-lg {
        width: 80%;
    }

    position: relative;

    .bottom-area {
        position: absolute;
        bottom: 0;
        z-index: $zindex-fixed;
        width: 100%;

        .bm-alert-area {
            width: 100%;
            background-color: $surface;
        }
    }

    .slide-fade-enter-active,
    .slide-fade-leave-active {
        transition: all 0.1s ease-out;
    }

    .slide-fade-enter,
    .slide-fade-leave-to {
        transform: translateY(20px);
    }
}
</style>
