<script>
import { useExtensions } from "@bluemind/extensions.vue";
import { useToolbarContext } from "./toolbar";

export default {
    name: "BmToolbarElement",
    functional: true,
    inheritAttrs: false,
    props: {
        extension: {
            type: String,
            default: undefined
        },
        extensionId: {
            type: String,
            default: undefined
        }
    },
    render(h, { props, scopedSlots, data }) {
        const { renderWebAppExtensions } = useExtensions();
        const { isInToolbar } = useToolbarContext();
        const extensions = renderWebAppExtensions(props.extension, props.extensionId, data.attrs);

        if (isInToolbar.value) {
            return scopedSlots.toolbar();
        }
        if (props.extension && extensions.length) {
            return scopedSlots["menu-with-extensions"]({ extensions });
        }
        return scopedSlots.menu();
    }
};
</script>
