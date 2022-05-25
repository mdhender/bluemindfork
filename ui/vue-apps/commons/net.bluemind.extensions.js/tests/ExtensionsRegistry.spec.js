let extensions;
jest.mock("@bluemind/global");

import BmExtensions from "../src/ExtensionsRegistry";
import global from "@bluemind/global";

describe("mapExtensions function", () => {
    beforeAll(() => {
        extensions = {
            "dummy.extension": [
                {
                    bundle: "one",
                    "my-key": {
                        attribute: "AttributeValue-1",
                        children: {
                            child: {
                                subattribute: "SubAttributeValue-1"
                            }
                        }
                    }
                },
                {
                    bundle: "two",
                    "my-key": {
                        attribute: "AttributeValue-2",
                        child: new String("BodyValue-2")
                    }
                },
                {
                    bundle: "three",
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
        };
    });
    beforeEach(() => {
        BmExtensions.clear();
        self.bundleResolve = undefined;
    });
    test("Global extionsion is defined", () => {
        expect(global.$extensions).toBeDefined();
    });
    test("Extract property data from extension point", () => {
        BmExtensions.load(extensions);
        expect(BmExtensions.extensions.has("dummy.extension")).toBeTruthy();
        const dummy = BmExtensions.extensions.get("dummy.extension");
        expect(dummy.length).toBe(3);
        expect(dummy[0].$id).toEqual("one");
        expect(dummy[0]["my-key"]).toMatchInlineSnapshot(`
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
                "subattribute": "SubAttributeValue-1",
              },
            }
        `);
        expect(dummy[1]["my-key"].child.toString()).toEqual("BodyValue-2");
        expect(dummy[2]["my-other-key"]).toBeDefined();
        expect(dummy[2]["my-other-key"].$id).toEqual("three");
        expect(BmExtensions.extensions.has("dummy.extension2")).toBeFalsy();
    });
    test("to add new extension when using register ", () => {
        BmExtensions.load(extensions);
        BmExtensions.register("dummy.extension", "local.id", {
            "my-key": {
                attribute: "AttributeValue-4",
                child: new Date()
            }
        });
        const dummy = BmExtensions.extensions.get("dummy.extension");
        expect(dummy.length).toBe(4);
        expect(dummy[3]["my-key"]).toBeDefined();
        expect(dummy[3]["my-key"].attribute).toEqual("AttributeValue-4");
        expect(dummy[3]["my-key"].child).toBeInstanceOf(Date);
    });
    test("Use get with one parameter to get all extension of an extension point", () => {
        BmExtensions.load(extensions);
        const values = BmExtensions.get("dummy.extension");
        expect(values.length).toBe(3);
    });
    test("Use get with to get extensions fields sorted by priorities", () => {
        extensions = {
            "dummy.extension": [
                {
                    bundle: "one",
                    "my-key": {
                        attribute: "AttributeValue-1",
                        priority: 1
                    }
                },

                {
                    bundle: "two",
                    "my-key": {
                        attribute: "AttributeValue-2",
                        priority: 2
                    }
                },

                {
                    bundle: "three",
                    "my-key": {
                        attribute: "AttributeValue-2",
                        priority: 0
                    }
                }
            ]
        };
        BmExtensions.load(extensions);
        const values = BmExtensions.get("dummy.extension", "my-key");
        expect(values.length).toBe(3);
        expect(values[0].$id).toEqual("two");
        expect(values[2].$id).toEqual("three");
    });
    test("Synced extension are always loaded", () => {
        BmExtensions.load(extensions);
        const values = BmExtensions.get("dummy.extension", "my-key");
        expect(values[0].$loaded).toBeDefined();
        expect(BmExtensions.get("dummy.extension")[0].$loaded).toBeDefined();
        expect(values[1].$loaded).toBeDefined();
        expect(BmExtensions.get("dummy.extension")[1].$loaded).toBeDefined();
        expect(BmExtensions.isLoaded(values[0])).toBeTruthy();
        expect(BmExtensions.isLoaded(values[1])).toBeTruthy();
    });
    test("Asynced extension are always not loaded at startup", () => {
        const resolved = {};
        self.bundleResolve = (id, callback) => (resolved[id] = callback);
        BmExtensions.load(extensions);
        const values = BmExtensions.get("dummy.extension", "my-key");
        expect(BmExtensions.isLoaded(values[0])).toBeFalsy();
        expect(BmExtensions.isLoaded(values[1])).toBeFalsy();
        resolved["one"]();
        expect(BmExtensions.isLoaded(values.find(({ $id }) => $id === "one"))).toBeTruthy();
        expect(BmExtensions.isLoaded(values.find(({ $id }) => $id !== "one"))).toBeFalsy();
        expect(
            BmExtensions.isLoaded(BmExtensions.get("dummy.extension").find(({ $id }) => $id === "one"))
        ).toBeTruthy();
        expect(BmExtensions.isLoaded(BmExtensions.get("dummy.extension").find(({ $id }) => $id !== "one"))).toBeFalsy();
    });
});
