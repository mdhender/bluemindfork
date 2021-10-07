<template>
    <div id="scroll-area" class="pref-content" @scroll="({ target }) => SET_OFFSET(target.scrollTop)">
        <div v-for="(section, index) in sections" :key="section.code" class="mb-5">
            <bm-list-group v-if="index !== 0" :id="'section-' + section.code" horizontal>
                <pref-section-navbar :section="section" />
            </bm-list-group>
            <pref-category
                v-for="category in section.categories"
                :key="category.code"
                :category="category"
                :section="section"
                :local-user-settings="localUserSettings"
                @requestSave="$emit('requestSave')"
            />
        </div>
    </div>
</template>

<script>
import { mapMutations } from "vuex";
import { BmListGroup } from "@bluemind/styleguide";
import PrefCategory from "./PrefCategory";
import PrefSectionNavbar from "./PrefSectionNavbar";

export default {
    name: "PrefContent",
    components: {
        BmListGroup,
        PrefCategory,
        PrefSectionNavbar
    },
    props: {
        sections: {
            type: Array,
            required: true
        },
        localUserSettings: {
            type: Object,
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
