import ItemUri from "@bluemind/item-uri";

export function expandFolder({ commit }, key) {
    commit("expandFolder", ItemUri.item(key));
}
