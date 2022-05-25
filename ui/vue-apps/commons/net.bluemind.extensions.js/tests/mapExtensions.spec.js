import { mapExtensions } from "../src/mapExtensions";
import ExtensionsRegistry from "../src/ExtensionsRegistry";

describe("mapExtensions function", () => {
    beforeAll(() => {
        ExtensionsRegistry.load({
            "dummy.extension": [
                {
                    bundle: "one",
                    "my-key": {
                        attribute: "AttributeValue-1",
                        children: {
                            child: {
                                attribute2: "AttributeValue-2"
                            }
                        }
                    }
                },
                {
                    bundle: "two",
                    "my-key": {
                        attribute: "AttributeValue-2",
                        children: {
                            child: {
                                body: "BodyValue-2"
                            }
                        }
                    }
                },
                {
                    bundle: "tree",
                    "my-key": {
                        attribute: "AttributeValue-2",
                        children: {
                            child: {
                                body: "BodyValue-2"
                            }
                        }
                    },
                    "my-other-key": {
                        attribute: "AttributeValue-2",
                        children: {
                            child: {
                                body: "BodyValue-2"
                            }
                        }
                    }
                }
            ],
            "dummy.extension2": []
        });
    });
    test("Extract property data from extension point", () => {
        let data = mapExtensions("dummy.extension", ["my-key"]);
        expect(typeof data).toEqual("object");
        expect(data["my-key"]).toBeDefined();
        expect(Array.isArray(data["my-key"])).toBeTruthy();
        expect(data["my-key"].length).toBe(3);
        expect(data["my-key"][0]).toMatchInlineSnapshot(`
            Object {
              "$id": "one",
              "$loaded": Object {
                "status": true,
              },
              "attribute": "AttributeValue-1",
              "child": Object {
                "$id": "one",
                "$loaded": Object {
                  "status": true,
                },
                "attribute2": "AttributeValue-2",
              },
            }
        `);
    });
    test("Map property to another name", () => {
        let data = mapExtensions("dummy.extension", { key: "my-key" });
        expect(typeof data).toEqual("object");
        expect(data["my-key"]).toBeUndefined();
        expect(data.key).toBeDefined();
        expect(data.key.length).toBe(3);
    });
    test("Use a function to map extension", () => {
        let data = mapExtensions("dummy.extension", { key: extension => extension["my-key"] });
        expect(typeof data).toEqual("object");
        expect(data["my-key"]).toBeUndefined();
        expect(data.key).toBeDefined();
        expect(data.key.length).toBe(3);
        expect(data.key[0]).toMatchInlineSnapshot(`
            Object {
              "$id": "one",
              "$loaded": Object {
                "status": true,
              },
              "attribute": "AttributeValue-1",
              "child": Object {
                "$id": "one",
                "$loaded": Object {
                  "status": true,
                },
                "attribute2": "AttributeValue-2",
              },
            }
        `);
    });
    test("Filter undefined extension", () => {
        let data = mapExtensions("dummy.extension", ["my-other-key"]);
        expect(data["my-other-key"]).toBeDefined();
        expect(Array.isArray(data["my-other-key"])).toBeTruthy();
        expect(data["my-other-key"].length).toBe(1);
    });

    test("To map all givent prperties", () => {
        let data = mapExtensions("dummy.extension", ["my-key", "my-other-key"]);
        expect(data["my-other-key"]).toBeDefined();
        expect(data["my-key"]).toBeDefined();
        data = mapExtensions("dummy.extension", { key: "my-key", yek: "my-other-key" });
        expect(data.key).toBeDefined();
        expect(data.yek).toBeDefined();
    });
});
