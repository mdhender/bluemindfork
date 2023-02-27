<script>
import camelize from "lodash.camelcase";

export default {
    name: "BmExtensionRenderless",
    functional: true,
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
    render: function (h, { props, data, scopedSlots }) {
        if (props.extensions[0]) {
            return h(props.extensions[0].name, {
                attrs: { ...data.attrs },
                scopedSlots: {
                    default: attrs =>
                        h("bm-extension-renderless", {
                            attrs,
                            props: { extensions: props.extensions.slice(1) },
                            scopedSlots: { ...scopedSlots }
                        })
                }
            });
        } else {
            const attrsCamelCase = Object.fromEntries(Object.entries(data.attrs).map(([k, v]) => [camelize(k), v]));
            const content = scopedSlots.default(attrsCamelCase);
            if (Array.isArray(content) && content.length > 1) {
                return h("div", content);
            }
            return content;
        }
    }
};
</script>
