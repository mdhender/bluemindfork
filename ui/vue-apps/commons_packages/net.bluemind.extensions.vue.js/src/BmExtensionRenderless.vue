<script>
import camelize from "lodash.camelcase";

const BmExtensionRenderless = {
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
        const extension = props.extensions[0];
        if (extension) {
            return h(extension.name, {
                attrs: { ...data.attrs },
                class: data.class,
                scopedSlots: {
                    default: attrs =>
                        h(BmExtensionRenderless, {
                            attrs,
                            class: data.class,
                            props: { extensions: props.extensions.slice(1) },
                            scopedSlots: { ...scopedSlots }
                        })
                }
            });
        } else {
            const attrsCamelCase = Object.fromEntries(Object.entries(data.attrs).map(([k, v]) => [camelize(k), v]));
            const content = scopedSlots.default(attrsCamelCase);
            if (Array.isArray(content) && content.length > 1) {
                return h("div", { class: data.class }, content);
            }
            return content;
        }
    }
};

export default BmExtensionRenderless;
</script>
