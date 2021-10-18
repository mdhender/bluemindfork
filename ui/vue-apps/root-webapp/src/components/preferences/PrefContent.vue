<template>
    <div id="scroll-area" class="pref-content" @scroll="({ target }) => SET_OFFSET(target.scrollTop)">
        <div v-for="(section, index) in sections" :key="section.id" class="mb-5">
            <template v-if="index !== 0">
                <bm-list-group :id="'section-' + section.id" horizontal>
                    <pref-section-navbar :section="section" />
                </bm-list-group>
            </template>
            <div :id="anchor(section)" :v-show="false"></div>
            <pref-category v-for="category in section.categories" :key="category.id" :category="category" />
        </div>
    </div>
</template>

<script>
import { mapMutations } from "vuex";
import { BmListGroup } from "@bluemind/styleguide";
import PrefCategory from "./PrefCategory";
import PrefSectionNavbar from "./PrefSectionNavbar";
import Navigation from "./mixins/Navigation";

export default {
    name: "PrefContent",
    components: {
        BmListGroup,
        PrefCategory,
        PrefSectionNavbar
    },
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
@import "~@bluemind/styleguide/css/_variables";

.pref-content {
    position: relative;
    overflow: auto;
    background-color: $surface-bg;
    z-index: 1;
    .pref-section-navbar {
        border-bottom: 1px solid $secondary;
    }
}
</style>
