<template>
    <nav class="pref-left-panel-nav mt-3" :aria-label="$t('preferences.menu.apps')">
        <bm-list-group v-bm-scrollspy:scroll-area>
            <bm-list-group-item
                v-for="section in sections"
                ref="section"
                :key="section.id"
                :active="isActive(section.id)"
                class="app-item container"
                :class="{ 'my-account-list-group-item': section.id === 'my_account' }"
                role="button"
                tabindex="0"
                :to="anchor(section, true)"
                @click="goToSection(section)"
            >
                <div class="d-flex flex-nowrap text-truncate w-100">
                    <div class="text-center mr-4"><pref-section-icon :section="section" /></div>
                    <div v-if="section.id === 'my_account'" class="display-name">
                        {{ userDisplayName }}
                    </div>
                    <div v-else class="section-name text-truncate">{{ section.name }}</div>
                </div>
                <div v-if="section.id === 'my_account'" class="section-name my-account-section-name">
                    {{ $t("preferences.general.manage_account") }}
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
@use "sass:math";
@import "~@bluemind/styleguide/css/_type";
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
        $arrow-half-height: base-px-to-rem(10);
        border-top: $arrow-half-height solid transparent;
        border-bottom: $arrow-half-height solid transparent;
        border-right: $arrow-half-height solid $surface;
        top: calc(50% - #{$arrow-half-height});
        right: 0;
    }

    .display-name {
        font-size: $font-size-lg;
        line-height: $line-height-base;
        color: $fill-primary-fg;
        white-space: initial;
        padding-top: math.div($avatar-height - $font-size-lg * $line-height-base, 2);
    }

    .section-name {
        color: $fill-primary-fg;
    }
    &.active .section-name {
        color: $fill-primary-fg-hi1;
    }

    &.list-group-item {
        padding: $sp-6 !important;
        height: initial;

        &.my-account-list-group-item {
            flex-direction: column;
            justify-content: flex-start;
        }
        &:not(.my-account-list-group-item) > div {
            align-items: center;
        }
    }

    .my-account-section-name {
        width: 100%;
        padding-left: $avatar-width + $sp-4;
        margin-top: $sp-2;
    }
}
</style>
