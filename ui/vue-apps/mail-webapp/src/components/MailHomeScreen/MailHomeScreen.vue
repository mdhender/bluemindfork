<template>
    <bm-extension id="webapp" type="decorator" path="mail.route.home">
        <section>
            <h1>Fallback</h1>
        </section>
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

    extensions.register("webapp", "mail-app", {
        component: {
            name: component.name,
            path: "mail.route.home",
            priority: component.priority || 128
        }
    });
});

extensions.register();
export default {
    name: "MailHomeScreen",
    components: { BmExtension }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins/responsiveness";
@import "~@bluemind/ui-components/src/css/type";
@import "~@bluemind/ui-components/src/css/variables";

.mail-home-screen {
    display: flex;
    @include until-lg {
        display: none;
    }

    flex: 1;
    &:before,
    &:after {
        content: "";
        flex: 1;
    }

    flex-direction: column;
    align-items: center;
    height: 100%;
    width: 100%;
    padding: $sp-6 0 $sp-5 $scroll-width;

    .starter-text-and-actions {
        flex: 0 1 base-px-to-rem(312);

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

            @extend %regular-high;

            h1 {
                margin: 0;
            }

            .bm-button {
                margin-top: $sp-4;
            }

            .description {
                flex: 1;

                width: 60%;
                min-width: base-px-to-rem(240);
                color: $neutral-fg;
                display: flex;
                align-items: center;
            }
        }

        .starter-links {
            flex: 1;

            display: flex;
            flex-wrap: wrap;
            justify-content: center;
            gap: $sp-3 $sp-7;
            margin: $sp-6 0 $sp-5 0;
            align-items: flex-end;

            .starter-link {
                display: flex;
                gap: $sp-4;
                .bm-icon {
                    color: $neutral-fg;
                }
            }
        }
    }

    .bm-illustration {
        flex: none;
    }
}
</style>
