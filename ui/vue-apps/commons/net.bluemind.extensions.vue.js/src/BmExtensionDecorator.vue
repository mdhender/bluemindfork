<script>
export default {
    name: "BmExtensionDecorator",
    functional: true,
    props: {
        extensions: {
            type: Array,
            default: () => []
        }
    },
    render: function (h, { props, data, scopedSlots }) {
        const extension = props.extensions[0];
        if (extension) {
            return h(extension.name, { attrs: data.attrs }, [
                h("bm-extension-decorator", {
                    props: { extensions: props.extensions.slice(1) },
                    attrs: data.attrs,
                    scopedSlots: { ...scopedSlots }
                })
            ]);
        } else {
            return scopedSlots.default();
        }
    }
};
</script>
