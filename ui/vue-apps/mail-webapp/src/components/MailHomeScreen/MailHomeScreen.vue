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
