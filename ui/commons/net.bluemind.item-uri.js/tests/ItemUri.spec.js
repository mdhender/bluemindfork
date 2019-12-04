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
    test("encode and decode item uid and container uid in an url compliant string", () => {
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
});
