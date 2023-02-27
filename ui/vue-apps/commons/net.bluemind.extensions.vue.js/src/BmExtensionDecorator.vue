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
        if (props.extensions[0]) {
            return h(props.extensions[0].name, { attrs: data.attrs }, [
                h("bm-extension-decorator", {
                    props: { extensions: props.extensions[0].slice(1) },
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
