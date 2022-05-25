<template>
    <div class="bm-extension" :class="className">
        <template v-if="$scopedSlots.default">
            <template v-for="extension in loaded">
                <slot v-bind="extension" />
            </template>
        </template>
        <template v-else-if="decorator">
            <component :is="decorator" v-for="extension in loaded" :key="extension.$id">
                <component :is="extension.name" :key="extension.$id" v-bind="$attrs" />
            </component>
        </template>
        <template v-else>
            <component :is="extension.name" v-for="extension in loaded" :key="extension.$id" v-bind="$attrs" />
        </template>
    </div>
</template>

<script>
import { mapExtensions } from "@bluemind/extensions";

export default {
    name: "BmExtension",
    props: {
        id: {
            type: String,
            required: true
        },
        path: {
            type: String,
            required: true
        },
        decorator: {
            type: String,
            required: false,
            default: undefined
        }
    },
    data() {
        return { extensions: Cache.get(this.id, this.path) };
    },
    computed: {
        className() {
            return "bm-extension-" + this.path.replace(/\./g, "-");
        },
        loaded() {
            return this.extensions.filter(({ $loaded }) => $loaded.status);
        }
    }
};

/**Testing exposure */
export const Cache = {
    map: new Map(),
    get(id, path) {
        if (!this.map.has(id)) {
            this.load(id);
        }
        return this.map.get(id).has(path) ? this.map.get(id).get(path) : [];
    },
    load(id) {
        const extensions = new Map();
        this.map.set(id, extensions);
        mapExtensions(id, ["component"])?.component?.forEach(component => {
            const value = extensions.get(component.path) || [];
            value.push(component);
            extensions.set(component.path, value);
        });
    },
    clear() {
        this.map = new Map();
    }
};
</script>

<style>
.bm-extensions:empty {
    display: none;
}
</style>
