<template>
    <nav class="pref-left-panel-nav mt-3" :aria-label="$t('preferences.menu.apps')">
        <bm-list-group v-bm-scrollspy:scroll-area>
            <bm-list-group-item
                v-for="section in sections"
                :ref="section.code"
                :key="section.href"
                :active="section.code === selectedSectionCode"
                class="app-item container"
                role="button"
                tabindex="0"
                :to="sectionPath(section)"
                @click="scrollTo(sectionId(section))"
            >
                <h2 class="row">
                    <bm-app-icon :icon-app="section.icon" class="col-3 text-center text-primary" />
                    <span class="col text-white">{{ section.name }}</span>
                </h2>
            </bm-list-group-item>
        </bm-list-group>
    </nav>
</template>

<script>
import { mapState } from "vuex";

import { BmListGroup, BmListGroupItem, BmScrollspy } from "@bluemind/styleguide";

import BmAppIcon from "../BmAppIcon";
import PrefMixin from "./mixins/PrefMixin";

export default {
    name: "PrefLeftPanelNav",
    components: {
        BmAppIcon,
        BmListGroup,
        BmListGroupItem
    },
    directives: { BmScrollspy },
    mixins: [PrefMixin],
    props: {
        sections: {
            required: true,
            type: Array
        }
    },
    computed: {
        ...mapState("preferences", ["selectedSectionCode"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-left-panel-nav {
    .app-item {
        border-bottom: 0 !important;
        background-color: $info-dark;
        list-style: none;

        &:hover,
        &.active {
            background-color: darken($info-dark, 10%);
        }

        .row span {
            line-height: 1.571em;
        }
    }
}
</style>
