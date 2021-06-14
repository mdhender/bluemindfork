<template>
    <div class="bm-extension" :class="className">
        <template v-if="$scopedSlots.default">
            <template v-for="extension in extensions">
                <slot v-bind="extension" />
            </template>
        </template>
        <template v-else-if="decorator">
            <component :is="decorator" v-for="extension in extensions" :key="extension.$id">
                <component :is="extension.component" :key="extension.$id" v-bind="$attrs" />
            </component>
        </template>
        <template v-else>
            <component :is="extension.component" v-for="extension in extensions" :key="extension.$id" v-bind="$attrs" />
        </template>
    </div>
</template>

<script>
import { mapExtensions } from "./";

export default {
    name: "BmExtension",
    props: {
        id: {
            type: String,
            required: true
        },
        property: {
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
        return { ...mapExtensions(this.id, { extensions: this.property }) };
    },
    computed: {
        className() {
            return "bm-extension-" + this.id.replace(/\./g, "-");
        }
    }
};
</script>
<style>
.bm-extensions:empty {
    display: none;
}
</style>
