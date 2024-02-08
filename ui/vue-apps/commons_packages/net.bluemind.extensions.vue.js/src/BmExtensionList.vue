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
    render(h, { props, data, scopedSlots }) {
        if (scopedSlots.default) {
            return props.extensions.map(extension => scopedSlots.default({ extension }));
        } else if (props.decorator) {
            return props.extensions.map(extension =>
                h(
                    props.decorator,
                    { props: { ...extension.props }, attrs: { ...data.attrs }, class: data.class },
                    h(extension.name, { attrs: { ...data.attrs } })
                )
            );
        } else {
            return props.extensions.map(extension =>
                h(extension.name, { attrs: { ...data.attrs }, class: data.class })
            );
        }
    }
};
</script>
