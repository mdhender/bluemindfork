<template>
    <nav class="pref-left-panel-nav mt-3" :aria-label="$t('preferences.menu.apps')">
        <bm-list-group v-bm-scrollspy:scroll-area>
            <bm-list-group-item
                v-for="section in sections"
                ref="section"
                :key="section.id"
                :active="section.id === selectedSectionId"
                class="app-item container px-4 py-3"
                role="button"
                tabindex="0"
                :to="anchor(section, true)"
                @click="scrollTo(section)"
            >
                <div class="row align-items-center">
                    <div class="col-2 text-center">
                        <bm-avatar v-if="section.id === 'my_account'" :alt="user.displayname" />
                        <bm-app-icon v-else :icon-app="section.icon" class="text-primary" />
                    </div>
                    <div class="col">
                        <div v-if="section.id === 'my_account'" class="text-white display-name">
                            {{ user.displayname }}
                        </div>
                        <div v-else class="text-primary-or-white font-size-lg">{{ section.name }}</div>
                    </div>
                </div>
                <div v-if="section.id === 'my_account'" class="text-primary-or-white row">
                    <div class="col-2" />
                    <div class="col">{{ $t("preferences.general.manage_account") }}</div>
                </div>
                <div class="arrow position-absolute" />
            </bm-list-group-item>
        </bm-list-group>
    </nav>
</template>

<script>
import { mapState } from "vuex";

import { inject } from "@bluemind/inject";
import { BmAvatar, BmListGroup, BmListGroupItem, BmScrollspy } from "@bluemind/styleguide";

import BmAppIcon from "../BmAppIcon";
import Navigation from "./mixins/Navigation";

export default {
    name: "PrefLeftPanelNav",
    components: {
        BmAppIcon,
        BmAvatar,
        BmListGroup,
        BmListGroupItem
    },
    directives: { BmScrollspy },
    mixins: [Navigation],
    props: {
        sections: {
            required: true,
            type: Array
        },
        user: {
            required: true,
            type: Object
        }
    },
    computed: {
        ...mapState("preferences", ["selectedSectionId"]),
        userDisplayName() {
            return inject("UserSession").formatedName;
        }
    },
    mounted() {
        this.$refs.section[0].focus();
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-left-panel-nav .app-item {
    border-bottom: 0 !important;
    background-color: $info-dark;
    list-style: none;
    outline: none;

    &:hover,
    &.active {
        background-color: darken($info-dark, 10%);
    }

    &.active .arrow {
        width: 0;
        height: 0;
        border-top: $sp-2 solid transparent;
        border-bottom: $sp-2 solid transparent;
        border-right: $sp-2 solid $white;
        top: calc(50% - #{$sp-2});
        right: 0;
    }

    .text-primary-or-white {
        color: $white;
    }

    .font-size-lg {
        font-size: $font-size-lg;
    }

    &.active .text-primary-or-white {
        color: $primary;
    }

    .bm-avatar {
        font-size: 1rem;
    }

    .display-name {
        font-size: 18px;
    }
}
</style>
