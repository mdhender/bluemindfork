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
    functional: true,
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
    },
    render(h, { props, data: { attrs }, scopedSlots }) {
        if (scopedSlots.default) {
            return props.extensions.map(extension => scopedSlots.default({ extension }));
        } else if (props.decorator) {
            return props.extensions.map(extension =>
                h(
                    props.decorator,
                    { props: { key: extension.$id, ...extension.props }, attrs },
                    h(extension.name, { props: { key: extension.$id }, attrs })
                )
            );
        } else {
            return props.extensions.map(extension => h(extension.name, { props: { key: extension.$id }, attrs }));
        }
    }
};
</script>
