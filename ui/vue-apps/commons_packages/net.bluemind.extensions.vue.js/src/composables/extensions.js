import { h } from "vue";
import BmExtension from "../BmExtension";

function useExtensions() {
    const normalizeSlot = slot => {
        return (Array.isArray(slot) ? slot : slot ? [slot] : []).filter(vnode => Boolean(vnode.tag));
    };
    const renderWebAppExtensions = (extension, id = "webapp", attrs = {}) => {
        if (!extension) {
            return [];
        }
        return normalizeSlot(h(BmExtension, { props: { id, path: extension }, attrs }));
    };

    return { renderWebAppExtensions, normalizeSlot };
}

export default useExtensions;
