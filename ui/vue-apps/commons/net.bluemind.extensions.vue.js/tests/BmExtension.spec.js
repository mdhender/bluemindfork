import inject from "@bluemind/inject";
import { mount } from "@vue/test-utils";
import { mapExtensions } from "@bluemind/extensions";
import { default as BmExtension, Cache } from "../src/BmExtension";
import BmExtensionList from "../src/BmExtensionList";
import BmExtensionDecorator from "../src/BmExtensionDecorator";
import BmExtensionRenderless from "../src/BmExtensionRenderless";

jest.mock("@bluemind/extensions");
inject.register({ provide: "UserSession", factory: () => ({ roles: "" }) });

self.bundleResolve = jest.fn().mockImplementation((id, callback) => callback());

describe("BmExtension", () => {
    beforeEach(() => {
        mapExtensions.mockReset();
        mapExtensions.mockReturnValue({ extensions: [] });
        Cache.clear();
    }),
        test("set classes depending on extension point", () => {
            const wrapper = mount(BmExtension, {
                propsData: {
                    id: "test.dummy.id",
                    path: "dummy-element"
                }
            });
            expect(wrapper.find(".bm-extension-dummy-element").exists()).toBeTruthy();
        });
    test("to call mapExtensions", () => {
        mount(BmExtension, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element"
            }
        });
        expect(mapExtensions).toHaveBeenCalledWith("test.dummy.id", ["component"]);
    });
});

describe("BmExtension switch to right extension type", () => {
    beforeEach(() => {
        mapExtensions.mockReset();

        mapExtensions.mockReturnValue({ extensions: [{ name: "extension-one", path: "dummy-path" }] });
        Cache.clear();
    });
    test("to create a BmExtensionDecorator", () => {
        const wrapper = mount(BmExtension, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                type: "decorator"
            },
            scopedSlots: {
                default: `inner slot`
            }
        });
        expect(wrapper.findAllComponents(BmExtensionDecorator).length).toBe(1);
    });
    test("to create a BmExtensionList", () => {
        const wrapper = mount(BmExtension, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                type: "list"
            }
        });
        expect(wrapper.findAllComponents(BmExtensionList).length).toBe(1);
    });
    test("to create a BmExtensionList by default", () => {
        const wrapper = mount(BmExtension, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element"
            }
        });
        expect(wrapper.findAllComponents(BmExtensionList).length).toBe(1);
    });
    test("to create a BmExtensionRenderless", () => {
        const wrapper = mount(BmExtension, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                type: "renderless"
            },
            scopedSlots: {
                default: `<span> Hello </span>`
            }
        });
        expect(wrapper.findAllComponents(BmExtensionRenderless).length).toBe(1);
    });
});
