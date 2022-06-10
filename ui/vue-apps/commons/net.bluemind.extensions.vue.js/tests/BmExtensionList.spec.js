import { mount } from "@vue/test-utils";
import inject from "@bluemind/inject";
import BmExtensionList from "../src/BmExtensionList";

jest.mock("@bluemind/extensions");
inject.register({ provide: "UserSession", factory: () => ({ roles: "" }) });

self.bundleResolve = jest.fn().mockImplementation((id, callback) => callback());

describe("BmExtensionList", () => {
    const DummyComponent = { render: h => h("div", { class: ["dummy"] }) };
    const AnotherComponent = { render: h => h("div", { class: ["another"] }) };
    const DecoratorComponent = {
        render(h) {
            return h("div", this.$slots.default);
        }
    };
    test("To be empty if there is no extensions", () => {
        let wrapper = mount(BmExtensionList, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                extensions: []
            }
        });
        expect(wrapper.element).toBeEmptyDOMElement();
        wrapper = mount(BmExtensionList, {
            stubs: { DummyComponent },
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                decorator: "DummyComponent",
                extensions: []
            }
        });
        expect(wrapper.element).toBeEmptyDOMElement();
        wrapper = mount(BmExtensionList, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                decorator: "DummyComponent",
                extensions: []
            },
            slots: {
                default: "Default"
            }
        });
        expect(wrapper.element).toBeEmptyDOMElement();
    });

    test("To insert component defined within extension", () => {
        let wrapper = mount(BmExtensionList, {
            stubs: { DummyComponent, AnotherComponent },
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                extensions: [
                    { name: "dummy-component", path: "dummy-element" },
                    { name: "dummy-component", path: "dummy-element" },
                    { name: "another-component", path: "dummy-element" }
                ]
            }
        });

        expect(wrapper.findAllComponents(AnotherComponent).length).toBe(1);
        expect(wrapper.findAllComponents(DummyComponent).length).toBe(2);
    });
    test("To wrap component inside Decorator if a decorator is used", () => {
        let wrapper = mount(BmExtensionList, {
            stubs: { DummyComponent, DecoratorComponent },
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                decorator: "DecoratorComponent",
                extensions: [
                    { name: "dummy-component", path: "dummy-element" },
                    { name: "dummy-component", path: "dummy-element" }
                ]
            }
        });
        expect(wrapper.findAllComponents(DecoratorComponent).length).toBe(2);
        expect(wrapper.findComponent(DecoratorComponent).find(".dummy").exists).toBeTruthy();
    });
    test("To use default slot to decorate component", () => {
        let wrapper = mount(BmExtensionList, {
            stubs: { DummyComponent },
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element",
                decorator: "DecoratorComponent",
                extensions: [
                    { name: "dummy-component", path: "dummy-element" },
                    { name: "dummy-component", path: "dummy-element" }
                ]
            },
            scopedSlots: {
                default: '<div class="slot"><component :is="props.name" /></div>'
            }
        });
        expect(wrapper.findAll(".slot").length).toBe(2);
        expect(wrapper.find(".slot").find(".dummy").exists()).toBeTruthy();
    });
});
