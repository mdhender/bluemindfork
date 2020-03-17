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
    },
    itemsByContainer(uris) {
        const itemsByContainer = {};
        uris.forEach(uri => {
            const [item, container] = this.decode(uri);
            if (!itemsByContainer[container]) {
                itemsByContainer[container] = [];
            }
            itemsByContainer[container].push(item);
        });
        return itemsByContainer;
    },
    urisByContainer(uris) {
        const urisByContainer = {};
        uris.forEach(uri => {
            const [, container] = this.decode(uri);
            if (!urisByContainer[container]) {
                urisByContainer[container] = [];
            }
            urisByContainer[container].push(uri);
        });
        return urisByContainer;
    },
    isItemUri(uri) {
        try {
            return !!this.decode(uri);
        } catch {
            return false;
        }
    }
};
