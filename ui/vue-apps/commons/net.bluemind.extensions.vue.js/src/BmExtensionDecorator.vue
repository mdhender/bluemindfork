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
                h("bm-extension-decorator", {
                    props: { extensions: this.extensions.slice(1) },
                    attrs: this.$attrs,
                    scopedSlots: { default: this.$scopedSlots.default }
                })
            ]);
        } else {
            return this.$scopedSlots.default();
        }
    }
};
</script>
