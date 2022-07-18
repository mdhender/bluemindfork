<script>
import camelize from "lodash.camelcase";

export default {
    name: "BmExtensionRenderless",
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
            return h(this.extension.name, {
                props: this.$attrs,
                scopedSlots: {
                    default: attrs =>
                        h("bm-extension-renderless", {
                            attrs,
                            props: { extensions: this.extensions.slice(1) },
                            scopedSlots: { default: this.$scopedSlots.default }
                        })
                }
            });
        } else {
            const attrsCamelCase = Object.fromEntries(Object.entries(this.$attrs).map(([k, v]) => [camelize(k), v]));
            const content = this.$scopedSlots.default(attrsCamelCase);
            if (Array.isArray(content) && content.length > 1) {
                return h("div", content);
            }
            return content;
        }
    }
};
</script>
