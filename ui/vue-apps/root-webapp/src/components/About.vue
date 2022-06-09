<template>
    <div
        class="about position-absolute w-100 h-100 d-flex overlay z-index-500 justify-content-around align-items-stretch align-items-lg-center"
        @click="close"
    >
        <global-events @keydown.esc="close" />
        <div
            class="d-flex align-items-center flex-column flex-fill flex-lg-grow-0 flex-lg-shrink-0 m-4 p-4"
            :style="{ background: 'url(' + aboutBackgroundImg + ')  center/cover' }"
        >
            <div class="flex-fill"></div>
            <div class="d-flex align-items-end">
                <!-- eslint-disable-next-line vue/no-v-html -->
                <svg height="68" width="271" v-html="BmLogo" />
            </div>
            <img :src="aboutVersionImg" alt="" class="mt-2" />
            <i18n path="banner.about.version" class="mt-2 flex-fill">
                <template v-slot:brand>
                    <strong class="font-weight-bold">
                        {{ $t("banner.about.version.brand", [version.brand]) }}
                    </strong>
                </template>
                <template v-slot:technical>{{ version.technical }}</template>
            </i18n>
            <a
                target="_blank"
                href="https://www.bluemind.net/"
                class="text-decoration-none text-uppercase h1 mb-4"
                @click.stop
            >
                www.bluemind.net
            </a>
            <span>Copyright 2012 - {{ year }} BlueMind</span>
        </div>
    </div>
</template>
<script>
import GlobalEvents from "vue-global-events";

import { BmLogo } from "@bluemind/styleguide";

import aboutBackgroundImg from "../../assets/about-background.png";
import aboutVersionImg from "../../assets/about-version.png";

export default {
    name: "About",
    components: {
        GlobalEvents
    },
    props: {
        version: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            aboutBackgroundImg,
            aboutVersionImg,
            BmLogo,
            year: new Date().getFullYear()
        };
    },
    methods: {
        close() {
            this.$router.navigate({ hash: "" });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.about {
    > div {
        background-color: $blue-800;
        color: $white !important;
        flex-basis: 717px !important;
        min-height: 500px;
        min-width: 320px;
    }
    a {
        color: $cyan;
        letter-spacing: 6px;
        font-weight: $font-weight-normal;
    }
}
</style>
