<script>
import { inject } from "@bluemind/inject";
import { mapExtensions } from "@bluemind/extensions";
import BmExtensionList from "./BmExtensionList";
import BmExtensionDecorator from "./BmExtensionDecorator";
import BmExtensionRenderless from "./BmExtensionRenderless";

const BmExtensionType = {
    LIST: "list",
    DECORATOR: "decorator",
    RENDERLESS: "renderless"
};

export default {
    name: "BmExtension",
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
    computed: {
        loaded() {
            return Cache.get(this.id, this.path).filter(({ $loaded }) => $loaded.status);
        }
    },
    render(h) {
        const extensions = this.loaded;
        const options = {
            class: ["bm-extension", "bm-extension-" + this.path.replace(/\./g, "-")],
            props: {
                extensions
            },
            attrs: this.$attrs,
            on: this.$listeners,
            scopedSlots: this.$scopedSlots
        };
        switch (this.type) {
            case BmExtensionType.DECORATOR:
                return h(BmExtensionDecorator, options);
            case BmExtensionType.RENDERLESS:
                return h(BmExtensionRenderless, options);
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
        const roles = inject("UserSession").roles.split(",");
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

<style>
.bm-extension:empty {
    display: none;
}
</style>
