import ItemUri from "../src";

describe("ItemUri", () => {
    test("encode and decode item uid/id and container uid", () => {
        let item = "ITEM:/?#[]@!$&'()*+,;=<>{}|`\\^a0-_";
        const container = "CONTAINER:/?#[]@!$&'()*+,;=<>{}|`\\^a0-_";
        let uri = ItemUri.encode(item, container);
        expect(ItemUri.decode(uri)).toEqual([item, container]);
        expect(ItemUri.item(uri)).toEqual(item);
        expect(ItemUri.container(uri)).toEqual(container);
        item = 2839264;
        uri = ItemUri.encode(item, container);
        expect(ItemUri.decode(uri)).toEqual([item, container]);
        expect(ItemUri.item(uri)).toEqual(item);
        expect(ItemUri.container(uri)).toEqual(container);
    });
    test.skip("encode and decode item uid and container uid in an url compliant string", () => {
        const item = ":/?#[]@!$&'()*+,;=";
        const container = "<>{}|`\\^";
        const uri = ItemUri.encode(item, container);
        expect(encodeURIComponent(uri)).toBe(uri);
    });
    test("encoding is consistent", () => {
        const item = "ITEM:/?#[]@!$&'()*+,;=<>{}|`\\^a0-_";
        const container = "CONTAINER:/?#[]@!$&'()*+,;=<>{}|`\\^a0-_";
        expect(ItemUri.encode(item, container)).toEqual(ItemUri.encode(item, container));
    });
    test("support empty item / container", () => {
        let uri = ItemUri.encode("item", null);
        expect(ItemUri.decode(uri)).toEqual(["item", null]);
        uri = ItemUri.encode(null, "container");
        expect(ItemUri.decode(uri)).toEqual([null, "container"]);
        uri = ItemUri.encode(null, null);
        expect(ItemUri.decode(uri)).toEqual([null, null]);
        uri = ItemUri.encode(undefined, undefined);
        expect(ItemUri.decode(uri)).toEqual([null, null]);
    });
    test("sort items by container", () => {
        const container = "c1";
        const container2 = "c2";

        const uris = [
            ItemUri.encode("item1", container),
            ItemUri.encode("item2", container),
            ItemUri.encode("item3", container2),
            ItemUri.encode("item4", container),
            ItemUri.encode("item5", container2)
        ];

        const result = ItemUri.itemsByContainer(uris);
        expect(result[container]).toEqual(["item1", "item2", "item4"]);
        expect(result[container2]).toEqual(["item3", "item5"]);
    });
    test("sort uris by container", () => {
        const container = "c1";
        const container2 = "c2";

        const uris = [
            ItemUri.encode("item1", container),
            ItemUri.encode("item2", container),
            ItemUri.encode("item3", container2),
            ItemUri.encode("item4", container),
            ItemUri.encode("item5", container2)
        ];

        const result = ItemUri.urisByContainer(uris);
        expect(result[container]).toEqual([
            ItemUri.encode("item1", container),
            ItemUri.encode("item2", container),
            ItemUri.encode("item4", container)
        ]);
        expect(result[container2]).toEqual([ItemUri.encode("item3", container2), ItemUri.encode("item5", container2)]);
    });
    test("test if a string is a ItemUri or not", () => {
        let uri = ItemUri.encode("item", "container");
        expect(ItemUri.isItemUri(uri)).toBeTruthy();
        uri = ItemUri.encode("item", null);
        expect(ItemUri.isItemUri(uri)).toBeTruthy();
        uri = ItemUri.encode(null, "container");
        expect(ItemUri.isItemUri(uri)).toBeTruthy();
        uri = "dummy string";
        expect(ItemUri.isItemUri(uri)).not.toBeTruthy();
    });
});
