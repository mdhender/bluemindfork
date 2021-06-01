<template>
    <bm-popover
        ref="bm-banner-applications"
        :target="target"
        placement="bottomright"
        custom-class="bm-banner-applications"
        triggers="click blur"
        variant="info-dark"
        @shown="setFocus"
    >
        <div class="text-white mb-2 mx-3">{{ $t("banner.main.apps") }}</div>
        <bm-row class="bm-apps">
            <bm-col v-for="app in applications" :key="app.href" cols="6" class="text-white">
                <a v-if="app.external" :href="app.href">
                    <div class="pl-3 my-2 bm-app">
                        <bm-app-icon :icon-app="app.icon" />
                        <span class="pl-2 text-uppercase align-middle">{{ app.name }}</span>
                    </div>
                </a>
                <router-link v-else :to="app.href" tag="div" class="pl-3 my-2 bm-app" @click.native="closePopover">
                    <bm-app-icon :icon-app="app.icon" />
                    <span class="pl-2 text-uppercase align-middle">{{ app.name }}</span>
                </router-link>
            </bm-col>
        </bm-row>
    </bm-popover>
</template>

<script>
import { BmCol, BmPopover, BmRow } from "@bluemind/styleguide";
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
@import "~@bluemind/styleguide/css/_variables";

.bm-banner-applications {
    max-width: unset !important;
    left: -23px !important;
    .bm-app {
        cursor: pointer;

        &:hover,
        &.router-link-active {
            color: $primary;
            font-weight: $font-weight-bold;
        }
    }

    .arrow {
        left: 17px !important;
    }

    .bm-apps {
        width: 22rem;
        a:focus {
            outline: 1px dotted $light;
            color: $primary;
        }
    }

    a {
        all: unset;
        &:hover {
            all: unset;
        }
        &:visited {
            color: $white;
        }
    }

    .popover-body {
        padding-right: 0;
        padding-left: 0;
    }
}
</style>
