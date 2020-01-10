import Folder from "../Folder";

export function folders(state) {
    return (state.itemKeys || []).map(key => {
        return state.items[key] && new Folder(key, state.items[key]);
    });
}
