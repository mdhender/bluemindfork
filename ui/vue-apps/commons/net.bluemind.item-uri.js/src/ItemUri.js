export const ItemUri = {
    encode(item, container) {
        return btoa(JSON.stringify([item, container]));
    },
    container(uri) {
        return this.decode(uri)[1];
    },
    item(uri) {
        return this.decode(uri)[0];
    },
    decode(uri) {
        return JSON.parse(atob(uri));
    }
};
