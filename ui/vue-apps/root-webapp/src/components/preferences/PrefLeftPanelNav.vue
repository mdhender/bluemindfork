<template>
    <nav class="pref-left-panel-nav mt-3" :aria-label="$t('preferences.menu.apps')">
        <bm-list-group v-bm-scrollspy:scroll-area>
            <bm-list-group-item
                v-for="section in sections"
                :ref="section.code"
                :key="section.href"
                class="text-primary app-item container"
                role="button"
                tabindex="0"
                :to="sectionPath(section.code, section.categories[0].code)"
                @click="
                    SET_SELECTED_SECTION(section.code);
                    scrollTo(sectionId(section.code, section.categories[0].code));
                "
            >
                <h2 class="row">
                    <bm-app-icon :icon-app="section.icon" class="col-3 text-center" />
                    <span class="col">{{ section.name }}</span>
                </h2>
            </bm-list-group-item>
        </bm-list-group>
    </nav>
</template>

<script>
import { BmListGroup, BmListGroupItem, BmScrollspy } from "@bluemind/styleguide";
import BmAppIcon from "../BmAppIcon";
import PrefMixin from "../../mixins/PrefMixin";
import { mapMutations } from "vuex";

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
    methods: {
        ...mapMutations("preferences", ["SET_SELECTED_SECTION"])
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
