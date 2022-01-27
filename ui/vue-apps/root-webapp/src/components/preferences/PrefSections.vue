<template>
    <div id="scroll-area" class="pref-sections" @scroll="({ target }) => SET_OFFSET(target.scrollTop)">
        <bm-alert-area :alerts="alerts" stackable @remove="REMOVE">
            <template v-slot="context"><component :is="context.alert.renderer" :alert="context.alert" /></template>
        </bm-alert-area>
        <div v-for="(section, index) in sections" :id="section.id" :key="section.id" class="mb-5 pref-section">
            <bm-list-group v-if="index !== 0" :id="'section-' + section.id" horizontal>
                <pref-section-navbar :section="section" />
            </bm-list-group>
            <div :id="anchor(section)" :v-show="false"></div>
            <pref-category
                v-for="category in section.categories.filter(c => c.visible)"
                :key="category.id"
                :category="category"
            />
        </div>
    </div>
</template>

<script>
import { mapMutations } from "vuex";
import { BmListGroup } from "@bluemind/styleguide";
import PrefCategory from "./PrefCategory";
import PrefSectionNavbar from "./PrefSectionNavbar";
import Navigation from "./mixins/Navigation";
import RightPanelAlerts from "./mixins/RightPanelAlerts";

export default {
    name: "PrefSections",
    components: { BmListGroup, PrefCategory, PrefSectionNavbar },
    mixins: [Navigation, RightPanelAlerts],
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
@import "~@bluemind/styleguide/css/_variables";

.pref-sections {
    position: relative;
    overflow: auto;
    background-color: $surface-bg;
    z-index: 1;
    .pref-section-navbar {
        border-bottom: 1px solid $secondary;
    }
    .pref-group {
        padding-left: 4rem;
    }
}
</style>
