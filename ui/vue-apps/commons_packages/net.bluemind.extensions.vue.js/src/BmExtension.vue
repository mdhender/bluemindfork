<script>
import { inject } from "@bluemind/inject";
import { mapExtensions } from "@bluemind/extensions";
import BmExtensionList from "./BmExtensionList";
import BmExtensionDecorator from "./BmExtensionDecorator";
import BmExtensionRenderless from "./BmExtensionRenderless";
import BmExtensionChainOfResponsibility from "./BmExtensionChainOfResponsibility";

const BmExtensionType = {
    LIST: "list",
    DECORATOR: "decorator",
    RENDERLESS: "renderless",
    CHAIN_OF_RESPONSIBILITY: "chain-of-responsibility"
};

export default {
    name: "BmExtension",
    functional: true,
    props: {
        type: {
            type: String,
            default: BmExtensionType.LIST
        },
        id: {
            type: String,
            required: true
        },
        path: {
            type: String,
            required: true
        }
    },
    render(h, { data, listeners, props, scopedSlots }) {
        const extensions = Cache.get(props.id, props.path).filter(({ $loaded }) => $loaded.status);
        const options = {
            props: {
                extensions
            },
            attrs: data.attrs,
            on: listeners,
            scopedSlots: scopedSlots
        };
        switch (props.type) {
            case BmExtensionType.DECORATOR:
                return h(BmExtensionDecorator, options);
            case BmExtensionType.RENDERLESS:
                return h(BmExtensionRenderless, options);
            case BmExtensionType.CHAIN_OF_RESPONSIBILITY:
                return h(BmExtensionChainOfResponsibility, options);
            default:
                return h(BmExtensionList, options);
        }
    }
};

/**Testing exposure */
export const Cache = {
    map: new Map(),
    get(id, path) {
        if (!this.map.has(id)) {
            this.load(id);
        }
        return this.map.get(id).has(path) ? this.map.get(id).get(path) : [];
    },
    load(id) {
        const extensions = new Map();
        const roles = inject("UserSession")?.roles.split(",") || [];
        this.map.set(id, extensions);
        mapExtensions(id, ["component"])?.component?.forEach(component => {
            if (!component.role || roles.includes(component.role)) {
                const value = extensions.get(component.path) || [];
                value.push(component);
                extensions.set(component.path, value);
            }
        });
    },
    clear() {
        this.map = new Map();
    }
};
</script>
