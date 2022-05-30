<template>
    <div class="bm-extension">
        <template v-if="$scopedSlots.default">
            <template v-for="extension in extensions">
                <slot v-bind="extension" />
            </template>
        </template>
        <template v-else-if="decorator">
            <component :is="decorator" v-for="extension in extensions" :key="extension.$id">
                <component :is="extension.name" :key="extension.$id" v-bind="$attrs" />
            </component>
        </template>
        <template v-else>
            <component :is="extension.name" v-for="extension in extensions" :key="extension.$id" v-bind="$attrs" />
        </template>
    </div>
</template>

<script>
export default {
    name: "BmExtensionList",
    props: {
        decorator: {
            type: String,
            required: false,
            default: undefined
        },
        extensions: {
            type: Array,
            required: true
        }
    }
};
</script>
