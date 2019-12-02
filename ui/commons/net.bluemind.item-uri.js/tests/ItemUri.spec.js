import ItemUri from "../src";

describe("ItemUri", () => {
    test("encode and decode item uid/id and container uid", () => {
        let item = "ITEM:/?#[]@!$&'()*+,;=<>{}|`\\^a0-_";
        const container = "CONTAINER:/?#[]@!$&'()*+,;=<>{}|`\\^a0-_";
        let uri = ItemUri.encode(item, container);
        expect(ItemUri.decode(uri)).toBe([item, container]);
        expect(ItemUri.item(uri)).toBe(item);
        expect(ItemUri.container(uri)).toBe(container);
        item = 2839264;
        uri = ItemUri.encode(item, container);
        expect(ItemUri.decode(uri)).toBe([item, container]);
        expect(ItemUri.item(uri)).toBe(item);
        expect(ItemUri.container(uri)).toBe(container);
    });
    test("encode and decode item uid and container uid in an url compliant string", () => {
        const item = ":/?#[]@!$&'()*+,;=";
        const container = "<>{}|`\\^";
        const uri = ItemUri.encode(item, container);
        expect(encodeURIComponent(uri)).toBe(uri);
    });
    test("encoding is consistant", () => {
        const item = "ITEM:/?#[]@!$&'()*+,;=<>{}|`\\^a0-_";
        const container = "CONTAINER:/?#[]@!$&'()*+,;=<>{}|`\\^a0-_";
        expect(ItemUri.encode(item, container)).toBe(ItemUri.encode(item, container));
    });
    test("support empty item / container", () => {
        let uri = ItemUri.encode("item", undefined);
        expect(ItemUri.decode(uri)).toBe(["item", undefined]);
        uri = ItemUri.encode(undefined, "container");
        expect(ItemUri.decode(uri)).toBe([undefined, "container"]);
        uri = ItemUri.encode(undefined, undefined);
        expect(ItemUri.decode(uri)).toBe([undefined, undefined]);
    });
    test("encoding result can help to match items from the same container", () => {
        expect(false).toBeTruthy();
    });
});
