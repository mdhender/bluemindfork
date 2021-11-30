<template>
    <bm-col lg="10" cols="12" class="pref-right-panel d-lg-flex flex-column h-100">
        <pref-right-panel-header :selected-section="section" @close="$emit('close')" />
        <pref-right-panel-nav v-if="!HAS_SEARCH" :sections="sections" />
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template v-slot="context"><component :is="context.alert.renderer" :alert="context.alert" /></template>
        </bm-alert-area>
        <pref-sections v-show="!HAS_SEARCH" ref="sections" :sections="sections" />
        <pref-search-results v-if="HAS_SEARCH" :results="searchResults" :is-loading="isSearchLoading" />
        <pref-right-panel-footer />
    </bm-col>
</template>

<script>
import debounce from "lodash.debounce";
import { mapActions, mapGetters, mapState } from "vuex";

import { BmAlertArea, BmCol } from "@bluemind/styleguide";
import { REMOVE, WARNING } from "@bluemind/alert.store";

import PrefRightPanelFooter from "./PrefRightPanelFooter";
import PrefRightPanelHeader from "./PrefRightPanelHeader";
import PrefRightPanelNav from "./PrefRightPanelNav";
import PrefSections from "../PrefSections";
import PrefSearchResults from "./PrefSearchResults";
import NeedReconnectionAlert from "../Alerts/NeedReconnectionAlert";
import ReloadAppAlert from "../Alerts/ReloadAppAlert";

export default {
    name: "PrefRightPanel",
    components: {
        BmAlertArea,
        BmCol,
        PrefRightPanelFooter,
        PrefRightPanelHeader,
        PrefRightPanelNav,
        PrefSections,
        PrefSearchResults,
        NeedReconnectionAlert,
        ReloadAppAlert
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
            }, 500)
        };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "pref-right-panel") }),
        ...mapState("preferences", ["selectedSectionId", "sectionById"]),
        ...mapGetters("preferences/fields", ["IS_LOGOUT_NEEDED", "IS_RELOAD_NEEDED"]),
        ...mapGetters("preferences", ["GET_GROUP", "HAS_SEARCH", "SEARCH_PATTERN"]),
        isReloadNeeded() {
            return this.IS_LOGOUT_NEEDED || this.IS_RELOAD_NEEDED;
        },
        section() {
            return this.sectionById[this.selectedSectionId] || {};
        }
    },
    watch: {
        isReloadNeeded() {
            const alert = {
                alert: { uid: "IS_RELOAD_NEEDED" },
                options: { area: "pref-right-panel", dismissible: false }
            };
            if (this.isReloadNeeded && this.IS_LOGOUT_NEEDED) {
                alert.name = "preferences.NEED_RECONNECTION";
                alert.options.renderer = "NeedReconnectionAlert";
                this.WARNING(alert);
            } else if (this.isReloadNeeded) {
                alert.name = "preferences.NEED_APP_RELOAD";
                alert.options.renderer = "ReloadAppAlert";
                this.WARNING(alert);
            } else {
                this.REMOVE(alert.alert);
            }
        },
        SEARCH_PATTERN() {
            if (this.HAS_SEARCH) {
                this.isSearchLoading = true;
                this.searchGroups();
            } else {
                this.searchResults = [];
            }
        }
    },
    methods: {
        ...mapActions("alert", { REMOVE, WARNING })
    }
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
    if (node.nodeType === 3 && node.textContent.toLowerCase().includes(pattern)) {
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
@import "~@bluemind/styleguide/css/_variables";

.pref-right-panel {
    .pref-section-icon {
        // a total width of 4rem, should be used as for the content's left margin
        &.bm-app-icon svg {
            width: 2rem;
            height: 2rem;
            margin: 0 1rem 0 1rem;
        }
        &.bm-avatar {
            margin-right: $sp-3;
            margin-left: $sp-3;
        }
    }
}
</style>
