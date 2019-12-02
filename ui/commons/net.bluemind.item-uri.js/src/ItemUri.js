export const ItemUri = {
    encode(item, container) {
        return btoa(item + "/" + container);
    },
    container(uri) {
        return this.decode(uri)[1];
    },
    item(uri) {
        return this.decode(uri)[0];
    },
    decode(uri) {
        return atob(uri).split("/");
    }
};
