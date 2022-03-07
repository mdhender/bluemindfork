<template>
    <div class="part-viewer-facade">
        <slot :name="slotName(part)" :message="message" :part="part">
            <component :is="componentName" :message="message" :part="part"></component>
        </slot>
    </div>
</template>

<script>
import PartViewerMixin from "./PartViewerMixin";

const components = {};
const viewerModuleLoader = require.context("./defaults", false, /\.vue$/);
viewerModuleLoader.keys().forEach(file => {
    const component = viewerModuleLoader(file).default;
    components[component.name] = component;
});

const registry = new Map();

Object.values(components).forEach(component => {
    component.$capabilities.forEach(capability => {
        if (!registry.has(capability)) {
            registry.set(capability, component.name);
        }
    });
});

export default {
    name: "PartViewerFacade",
    components,
    mixins: [PartViewerMixin],
    computed: {
        componentName() {
            const fallback = this.part.mime.replace(/\/.*$/, "/*");
            return registry.get(this.part.mime) || registry.get(fallback);
        }
    },

    methods: {
        slotName({ mime }) {
            return mime.replaceAll("/", "-");
        }
    }
};
</script>
