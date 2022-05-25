import { mount } from "@vue/test-utils";
import { default as BmExtension, Cache } from "../src/BmExtension";
import { mapExtensions } from "@bluemind/extensions";

jest.mock("@bluemind/extension");

self.bundleResolve = jest.fn().mockImplementation((id, callback) => callback());

describe("BmExtension", () => {
    const DummyComponent = { render: h => h("div", { class: ["dummy"] }) };
    const AnotherComponent = { render: h => h("div", { class: ["another"] }) };
    const DecoratorComponent = {
        render(h) {
            return h("div", this.$slots.default);
        }
    };
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
    test("To be empty if there is no extensions", () => {
        let wrapper = mount(BmExtension, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element"
            }
        });
        expect(wrapper.element).toBeEmptyDOMElement();
        wrapper = mount(BmExtension, {
            stubs: { DummyComponent },
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                decorator: "DummyComponent"
            }
        });
        expect(wrapper.element).toBeEmptyDOMElement();
        wrapper = mount(BmExtension, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                decorator: "DummyComponent"
            },
            slots: {
                default: "Default"
            }
        });
        expect(wrapper.element).toBeEmptyDOMElement();
    });

    test("To insert component defined within extension", () => {
        mapExtensions.mockReturnValue({
            component: [
                { name: "DummyComponent", path: "dummy-element", $loaded: { status: true } },
                { name: "DummyComponent", path: "dummy-element", $loaded: { status: true } },
                { name: "AnotherComponent", path: "dummy-element", $loaded: { status: true } }
            ]
        });
        let wrapper = mount(BmExtension, {
            stubs: { DummyComponent, AnotherComponent },
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element"
            }
        });
        expect(wrapper.findAllComponents(AnotherComponent).length).toBe(1);
        expect(wrapper.findAllComponents(DummyComponent).length).toBe(2);
    });
    test("To wrap component inside Decorator if a decorator is used", () => {
        mapExtensions.mockReturnValue({
            component: [
                { name: "DummyComponent", path: "dummy-element", $loaded: { status: true } },
                { name: "DummyComponent", path: "dummy-element", $loaded: { status: true } }
            ]
        });
        let wrapper = mount(BmExtension, {
            stubs: { DummyComponent, DecoratorComponent },
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                decorator: "DecoratorComponent"
            }
        });
        expect(wrapper.findAllComponents(DecoratorComponent).length).toBe(2);
        expect(wrapper.findComponent(DecoratorComponent).find(".dummy").exists).toBeTruthy();
    });
    test("To use default slot to decorate component", () => {
        mapExtensions.mockReturnValue({
            component: [
                { name: "DummyComponent", path: "dummy-element", $loaded: { status: true } },
                { name: "DummyComponent", path: "dummy-element", $loaded: { status: true } }
            ]
        });
        let wrapper = mount(BmExtension, {
            stubs: { DummyComponent },
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                decorator: "DecoratorComponent"
            },
            scopedSlots: {
                default: '<div class="slot"><component :is="props.name" /></div>'
            }
        });
        expect(wrapper.findAll(".slot").length).toBe(2);
        expect(wrapper.find(".slot").find(".dummy").exists()).toBeTruthy();
    });
});
