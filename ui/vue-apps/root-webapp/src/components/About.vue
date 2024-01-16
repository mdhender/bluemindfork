<template>
    <div
        class="about position-absolute w-100 h-100 d-flex overlay z-index-500 justify-content-around align-items-stretch align-items-lg-center"
        @click="close"
    >
        <global-events @keydown.esc="close" />
        <div :style="{ background: 'url(' + aboutBackgroundImg + ')  center/cover' }">
            <div class="about-brand">
                <!-- eslint-disable-next-line vue/no-v-html -->
                <svg height="51" width="200" v-html="BmLogo" />
            </div>
            <div class="about-logo">
                <img :src="aboutVersionImg" alt="" />
            </div>
            <i18n path="banner.about.version" class="about-version">
                <template #brand>
                    <span class="about-version-commercial">
                        {{ $t("banner.about.version.brand", [version.brand]) }}
                    </span>
                </template>
                <template #technical>
                    <span class="about-version-technical">{{ version.technical }}</span>
                </template>
            </i18n>
            <a target="_blank" href="https://www.bluemind.net/" class="about-link" @click.stop> www.bluemind.net </a>
            <span class="about-copyright">Copyright 2012 - {{ year }} BlueMind</span>
        </div>
    </div>
</template>
<script>
import GlobalEvents from "vue-global-events";

import { BmLogo } from "@bluemind/ui-components";

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
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
.about {
    > div {
        background-color: $blue-800;
        color: $white !important;

        min-height: 500px;
        min-width: 320px;
        padding: $sp-4;
        margin: $sp-4;

        flex: 717px 1 1;
        @include from-lg {
            flex-shrink: 0;
            flex-grow: 0;
        }

        display: flex;
        flex-direction: column;
        align-items: center;
    }
    .about-brand {
        margin-top: 145px;
    }
    .about-logo {
        margin: 16px 0;
    }
    .about-version {
        @include caption;
        text-transform: lowercase;
        margin-bottom: 125px;
        color: rgba(255, 255, 255, 0.4);
        .about-version-commercial {
            color: rgba(255, 255, 255, 1);
        }
    }
    .about-link {
        color: $blue-500;
        text-decoration: none;
        margin-bottom: 10px;
        @include regular;
    }
    .about-copyright {
        @include caption;
        opacity: 0.67;
    }
}
</style>
