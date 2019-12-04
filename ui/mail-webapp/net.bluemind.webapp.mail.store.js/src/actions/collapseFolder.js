import ItemUri from "@bluemind/item-uri";

export function collapseFolder({ commit }, key) {
    commit("collapseFolder", ItemUri.item(key));
}
