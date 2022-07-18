<template>
    <div class="file-viewer-facade">
        <slot :name="slotName(file)" :message="message" :file="file">
            <component :is="componentName" v-if="componentName" :message="message" :file="file"></component>
        </slot>
    </div>
</template>

<script>
import FileViewerMixin from "./FileViewerMixin";

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
    name: "FileViewerFacade",
    components,
    mixins: [FileViewerMixin],
    computed: {
        componentName() {
            const fallback = this.file.mime.replace(/\/.*$/, "/*");
            return registry.get(this.file.mime) || registry.get(fallback);
        }
    },
    methods: {
        slotName({ mime }) {
            return mime.replaceAll("/", "-");
        }
    }
};
</script>
