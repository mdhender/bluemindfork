<template>
    <nav class="pref-left-panel-nav mt-3" :aria-label="$t('preferences.menu.apps')">
        <bm-list-group v-bm-scrollspy:scroll-area>
            <bm-list-group-item
                v-for="section in sections"
                ref="section"
                :key="section.id"
                :active="isActive(section.id)"
                class="app-item container px-4 py-3"
                role="button"
                tabindex="0"
                :to="anchor(section, true)"
                @click="goToSection(section)"
            >
                <div class="row align-items-center">
                    <div class="col-2 text-center mr-1"><pref-section-icon :section="section" /></div>
                    <div class="col">
                        <div v-if="section.id === 'my_account'" class="display-name">
                            {{ userDisplayName }}
                        </div>
                        <div v-else class="section-name font-size-lg">{{ section.name }}</div>
                    </div>
                </div>
                <div v-if="section.id === 'my_account'" class="section-name row">
                    <div class="col-2" />
                    <div class="col">{{ $t("preferences.general.manage_account") }}</div>
                </div>
                <div class="arrow position-absolute" />
            </bm-list-group-item>
        </bm-list-group>
    </nav>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { BmListGroup, BmListGroupItem, BmScrollspy } from "@bluemind/styleguide";
import Navigation from "./mixins/Navigation";
import PrefSectionIcon from "./PrefSectionIcon";

export default {
    name: "PrefLeftPanelNav",
    components: { BmListGroup, BmListGroupItem, PrefSectionIcon },
    directives: { BmScrollspy },
    mixins: [Navigation],
    props: {
        sections: {
            required: true,
            type: Array
        }
    },
    data() {
        return { userDisplayName: inject("UserSession").formatedName };
    },
    computed: {
        ...mapGetters("preferences", ["HAS_SEARCH"]),
        ...mapState("preferences", ["selectedSectionId"])
    },
    mounted() {
        this.$refs.section[0].focus();
    },
    methods: {
        ...mapMutations("preferences", ["SET_SEARCH", "SET_SELECTED_SECTION"]),
        async goToSection(section) {
            this.SET_SEARCH("");
            this.SET_SELECTED_SECTION(section.id);
            await this.$nextTick();
            this.scrollTo(section);
        },
        isActive(sectionId) {
            return !this.HAS_SEARCH && sectionId === this.selectedSectionId;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-left-panel-nav .app-item {
    border-bottom: 0 !important;
    background-color: $fill-primary-bg;
    list-style: none;
    outline: none;

    &:hover,
    &.active {
        background-color: $fill-primary-bg-hi1;
    }

    &.active .arrow {
        width: 0;
        height: 0;
        border-top: $sp-2 solid transparent;
        border-bottom: $sp-2 solid transparent;
        border-right: $sp-2 solid $surface;
        top: calc(50% - #{$sp-2});
        right: 0;
    }

    .font-size-lg {
        font-size: $font-size-lg;
    }

    .display-name {
        font-size: 18px;
        color: $fill-primary-fg;
    }

    .section-name {
        color: $fill-primary-fg;
    }
    &.active .section-name {
        color: $fill-primary-fg-hi1;
    }
}
</style>
