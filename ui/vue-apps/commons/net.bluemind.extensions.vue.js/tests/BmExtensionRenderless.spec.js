import { mount } from "@vue/test-utils";
import BmExtensionRenderless from "../src/BmExtensionRenderless";

describe("BmExtensionRenderless without extension", () => {
    test("No modification of props", () => {
        const wrapper = mount(BmExtensionRenderless, {
            propsData: {
                color: "blue",
                extensions: []
            },
            scopedSlots: {
                default: `<span>Hello {{ props.color }}</span>`
            }
        });
        expect(wrapper.text()).toBe("Hello blue");
        expect(wrapper.find("span")).toBeTruthy();
    });
});

describe("BmExtensionRenderless with extensions", () => {
    test("Modification of prop", () => {
        const ExtensionOne = {
            props: ["color"],
            render: function () {
                return this.$scopedSlots.default({ color: this.color + " light" });
            }
        };

        const wrapper = mount(BmExtensionRenderless, {
            propsData: {
                color: "blue",
                extensions: [{ name: "extension-one", path: "dummy-path" }]
            },
            scopedSlots: {
                default: `<span>Hello {{ props.color }} </span>`
            },
            components: { ExtensionOne }
        });
        expect(wrapper.text()).toBe("Hello blue light");
    });

    test("Replacement of prop", () => {
        const ExtensionTwo = {
            props: ["color"],
            render: function () {
                return this.$scopedSlots.default({ color: "yellow" });
            }
        };

        const wrapper = mount(BmExtensionRenderless, {
            propsData: {
                color: "blue",
                extensions: [{ name: "extension-two", path: "dummy-path" }]
            },
            scopedSlots: {
                default: `<span>Hello {{ props.color }} </span>`
            },
            components: { ExtensionTwo }
        });
        expect(wrapper.text()).toBe("Hello yellow");
    });

    test("Several modification and replacement of prop", () => {
        const ExtensionOne = {
            props: ["color"],
            render: function () {
                return this.$scopedSlots.default({ color: "yellow" });
            }
        };
        const ExtensionTwo = {
            props: ["color"],
            render: function () {
                return this.$scopedSlots.default({ color: "light " + this.color });
            }
        };

        const wrapper = mount(BmExtensionRenderless, {
            propsData: {
                color: "blue",
                extensions: [
                    { name: "extension-one", path: "dummy-path" },
                    { name: "extension-two", path: "dummy-path" }
                ]
            },
            scopedSlots: {
                default: `<span>Hello {{ props.color }} </span>`
            },
            components: { ExtensionTwo, ExtensionOne }
        });
        expect(wrapper.text()).toBe("Hello light yellow");
    });

    test("Order of extensions changes the order of modification and replacement of prop", () => {
        const ExtensionOne = {
            props: ["color"],
            render: function () {
                return this.$scopedSlots.default({ color: "yellow" });
            }
        };
        const ExtensionTwo = {
            props: ["color"],
            render: function () {
                return this.$scopedSlots.default({ color: "light " + this.color });
            }
        };

        const wrapper = mount(BmExtensionRenderless, {
            propsData: {
                color: "blue",
                extensions: [
                    { name: "extension-two", path: "dummy-path" },
                    { name: "extension-one", path: "dummy-path" }
                ]
            },
            scopedSlots: {
                default: `<span>Hello {{ props.color }} </span>`
            },
            components: { ExtensionTwo, ExtensionOne }
        });
        expect(wrapper.text()).toBe("Hello yellow");
    });

    test("Addition of prop", () => {
        const ExtensionOne = {
            props: ["color"],
            render: function () {
                return this.$scopedSlots.default({ color: "yellow", shape: "circle" });
            }
        };
        const ExtensionTwo = {
            props: ["color"],
            render: function () {
                return this.$scopedSlots.default({ color: "light " + this.color });
            }
        };

        const wrapper = mount(BmExtensionRenderless, {
            propsData: {
                color: "blue",
                extensions: [
                    { name: "extension-two", path: "dummy-path" },
                    { name: "extension-one", path: "dummy-path" }
                ]
            },
            scopedSlots: {
                default: `<span>Hello {{ props.color }} {{ props.shape }}</span>`
            },
            components: { ExtensionTwo, ExtensionOne }
        });
        expect(wrapper.text()).toBe("Hello yellow circle");
    });
});
