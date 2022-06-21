<template>
    <div class="part-viewer-facade">
        <slot :name="slotName(part)" :message="message" :part="part">
            <component :is="componentName" v-if="componentName" :message="message" :part="part"></component>
            <div v-else class="no-preview d-flex flex-fill align-items-center text-center p-2 justify-content-around">
                <div>
                    <div>{{ part.fileName }}</div>
                    <div class="font-weight-bold">{{ $t("mail.viewer.no.preview") }}</div>
                </div>
            </div>
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

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.part-viewer-facade {
    .no-preview {
        color: $neutral-fg;
        height: 100%;
    }
}
</style>
