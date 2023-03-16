<template functional>
    <div>
        <template v-if="scopedSlots.default">
            <template v-for="extension in props.extensions">
                <slot v-bind="extension" />
            </template>
        </template>
        <template v-else-if="props.decorator">
            <component :is="props.decorator" v-for="(extension, idx) in props.extensions" :key="idx">
                <component :is="extension.name" :key="extension.$id" v-bind="data.attrs" />
            </component>
        </template>
        <template v-else>
            <component
                :is="extension.name"
                v-for="(extension, idx) in props.extensions"
                :key="idx"
                v-bind="data.attrs"
            />
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
