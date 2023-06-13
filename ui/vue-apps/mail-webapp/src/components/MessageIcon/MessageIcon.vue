<template>
    <bm-extension
        id="webapp.mail"
        class="message-icon"
        type="chain-of-responsibility"
        path="message.icon"
        :message="message"
    />
</template>

<script>
import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import { BmExtension } from "@bluemind/extensions.vue";

const loader = require.context("./extensions", false, /\.vue$/);
loader.keys().forEach(file => {
    const { default: component, priority } = loader(file);
    Vue.component(component.name, component);
    extensions.register("webapp.mail", "net.bluemind.webapp.mail.js", {
        component: {
            name: component.name,
            path: "message.icon",
            priority: priority
        }
    });
});

export default {
    name: "MessageIcon",
    components: { BmExtension },
    props: {
        message: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables.scss";

.message-icon {
    color: $neutral-fg;
}
</style>
