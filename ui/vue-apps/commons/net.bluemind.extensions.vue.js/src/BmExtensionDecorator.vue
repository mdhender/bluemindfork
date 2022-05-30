<script>
export default {
    name: "BmExtensionDecorator",
    props: {
        extensions: {
            type: Array,
            default: () => []
        }
    },

    computed: {
        extension() {
            return this.extensions[0];
        }
    },
    render: function (h) {
        if (this.extension) {
            return h(this.extension.name, { attrs: this.$attrs }, [
                h(
                    "bm-extension-decorator",
                    {
                        props: { extensions: this.extensions.slice(1) },
                        attrs: this.$attrs
                    },
                    this.$slots.default
                )
            ]);
        } else {
            return this.$slots.default;
        }
    }
};
</script>
