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
                    { props: { ...extension.props }, attrs },
                    h(extension.name, { attrs })
                )
            );
        } else {
            return props.extensions.map(extension => h(extension.name, { attrs }));
        }
    }
};
</script>
