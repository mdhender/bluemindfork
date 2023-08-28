<template>
    <bm-extension
        id="webapp"
        class="composer-top-frame"
        type="decorator"
        path="composer.topframe"
        :message="message"
        :attachments="attachments"
    >
        <div />
    </bm-extension>
</template>

<script>
import Vue from "vue";
import { BmExtension } from "@bluemind/extensions.vue";
import { extensions } from "@bluemind/extensions";

const loader = require.context("./frames", false, /TopFrame\.vue$/);
loader.keys().forEach(file => {
    const component = loader(file).default;
    Vue.component(component.name, component);
    extensions.register("webapp", "net.bluemind.webapp.mail.js", {
        component: { name: component.name, path: "composer.topframe", priority: component.priority }
    });
});

export default {
    name: "ComposerTopFrame",
    components: { BmExtension },
    props: {
        message: { type: Object, required: true },
        attachments: { type: Array, required: true }
    }
};
</script>
