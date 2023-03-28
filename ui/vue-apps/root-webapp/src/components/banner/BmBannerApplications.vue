<template>
    <bm-popover
        ref="bm-banner-applications"
        :target="target"
        placement="bottomright"
        custom-class="bm-banner-applications"
        triggers="click blur"
        variant="primary"
        @shown="setFocus"
    >
        <div class="banner-apps-title mb-4 mx-5">{{ $t("banner.main.apps") }}</div>
        <bm-row class="bm-apps">
            <bm-col v-for="app in applications.filter(({ hidden }) => !hidden)" :key="app.$id" cols="6">
                <a v-if="app.external" :href="app.path">
                    <div class="bm-app">
                        <bm-app-icon :icon-app="app.icon" />
                        <span class="pl-4 text-uppercase align-middle">{{ app.name }}</span>
                    </div>
                </a>
                <router-link v-else :to="app.path" tag="div" class="bm-app" @click.native="closePopover">
                    <bm-app-icon :icon-app="app.icon" />
                    <span class="pl-4 text-uppercase align-middle">{{ app.name }}</span>
                </router-link>
            </bm-col>
        </bm-row>
    </bm-popover>
</template>

<script>
import { BmCol, BmPopover, BmRow } from "@bluemind/ui-components";
import BmAppIcon from "../BmAppIcon";

export default {
    name: "BmBannerApplications",
    components: { BmAppIcon, BmCol, BmPopover, BmRow },
    props: {
        applications: {
            required: true,
            type: Array
        },
        target: {
            required: true,
            type: String
        }
    },
    methods: {
        closePopover() {
            this.$root.$emit("bv::hide::popover", this.target);
        },
        setFocus() {
            this.$nextTick(() => document.getElementsByClassName("bm-banner-applications")[0].focus());
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.bm-banner-applications {
    max-width: unset !important;
    left: -23px !important;

    .banner-apps-title {
        color: $fill-primary-fg;
    }

    .arrow {
        left: 17px !important;
    }

    .bm-apps {
        width: 22rem;
        .bm-app {
            padding-left: $sp-5;
            margin-top: $sp-4;
            margin-bottom: $sp-4;
            cursor: pointer;
            color: $fill-primary-fg;

            &:hover,
            &.router-link-active {
                color: $fill-primary-fg-hi1;
                font-weight: $font-weight-bold;
            }
        }

        a {
            color: $fill-primary-fg;
            all: unset;
            &:hover {
                all: unset;
            }
            &:visited {
                color: $fill-primary-fg;
            }
            &:focus {
                outline: 1px dotted $fill-primary-fg-hi1;
                color: $fill-primary-fg-hi1;
            }
        }
    }

    .popover-body {
        padding-right: 0;
        padding-left: 0;
    }
}
</style>
