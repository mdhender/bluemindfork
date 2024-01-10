import { h } from "vue";
import BmExtension from "../BmExtension";

function useExtensions() {
    const normalizeSlot = slot => {
        return (Array.isArray(slot) ? slot : slot ? [slot] : []).filter(vnode => Boolean(vnode.tag));
    };
    const renderWebAppExtensions = extension => {
        if (!extension) {
            return [];
        }
        return normalizeSlot(h(BmExtension, { props: { id: "webapp", path: extension } }));
    };

    return { renderWebAppExtensions, normalizeSlot };
}

export default useExtensions;
