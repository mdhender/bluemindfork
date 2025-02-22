<template>
    <bm-extension id="webapp" type="decorator" path="mail.route.home">
        <section></section>
    </bm-extension>
</template>
<script>
import Vue from "vue";
import { BmExtension } from "@bluemind/extensions.vue";
import { extensions } from "@bluemind/extensions";

const loader = require.context("./screens", false, /\.vue$/);
loader.keys().forEach(file => {
    const component = loader(file).default;
    Vue.component(component.name, component);

    extensions.register("webapp", "net.bluemind.webapp.mail.js", {
        component: {
            name: component.name,
            path: "mail.route.home",
            priority: component.priority || 128
        }
    });
});

export default {
    name: "MailHomeScreen",
    components: { BmExtension }
};
</script>

<style lang="scss">
@use "sass:map";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-home-screen {
    display: flex;
    @include until-lg {
        display: none;
    }

    &:before,
    &:after {
        content: "";
        flex: 1;
    }

    flex-direction: column;
    align-items: center;
    height: 100%;
    width: 100%;
    padding: $sp-6 0 $sp-7;

    $text-and-actions-base-height: base-px-to-rem(230);
    $illustration-height: 360px;

    .starter-text-and-actions {
        flex: 0 1 $text-and-actions-base-height;

        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: space-between;

        .starter-main {
            flex: 3;
            min-height: max-content;

            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: space-between;
            text-align: center;

            @include regular-high;

            h1 {
                margin: 0;
            }

            .bm-button {
                margin-top: $sp-4;
                margin-bottom: $sp-6;
            }

            .description {
                flex: 1;

                width: 60%;
                min-width: base-px-to-rem(240);
                color: $neutral-fg;
                display: flex;
                align-items: center;
                justify-content: center;
            }
        }

        .starter-links {
            flex: 1;

            display: flex;
            flex-wrap: wrap;
            justify-content: center;
            align-items: center;
            gap: 0 $sp-7;
            & > .bm-button {
                margin-top: $sp-6;
                margin-bottom: $sp-5;
            }
        }
    }

    .bm-illustration.illustration-lg {
        flex: none;
        height: $illustration-height;
        position: relative;

        & > svg {
            position: absolute;
            top: -40px;
        }
    }

    .after-illustration {
        height: base-px-to-rem(46);
        flex: none;
    }

    &.minimalist {
        $space-saved-over-illustration: 50px;
        $space-saved-under-illustration: 100px;
        $space-saved: $space-saved-over-illustration + $space-saved-under-illustration;

        .starter-text-and-actions {
            flex: 0 1 calc(#{$text-and-actions-base-height} - #{$space-saved});
        }

        .bm-illustration {
            flex: 0 1 $illustration-height + $space-saved;
            min-height: $illustration-height;
            flex-direction: column;

            &:before {
                flex: 0 1 $space-saved-over-illustration;
                content: "";
            }
            &:after {
                flex: 0 1 $space-saved-under-illustration;
                content: "";
            }
        }
    }
}
</style>
