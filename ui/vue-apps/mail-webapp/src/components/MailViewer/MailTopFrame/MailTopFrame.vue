<template>
    <bm-extension id="webapp" class="mail-top-frame" type="decorator" path="mail.topframe" :message="message">
        <div />
    </bm-extension>
</template>

<script>
import Vue from "vue";
import { BmExtension } from "@bluemind/extensions.vue";
import { extensions } from "@bluemind/extensions";

const loader = require.context("./frames", false, /\.vue$/);
loader.keys().forEach(file => {
    const component = loader(file).default;
    Vue.component(component.name, component);
    extensions.register("webapp", "mail-app", {
        component: { name: component.name, path: "mail.topframe", priority: component.priority }
    });
});

export default {
    name: "MailTopFrame",
    components: { BmExtension },
    props: { message: { type: Object, required: true } },
    render() {
        return this.$scopedSlots.default({ message: this.message });
    }
};
</script>
