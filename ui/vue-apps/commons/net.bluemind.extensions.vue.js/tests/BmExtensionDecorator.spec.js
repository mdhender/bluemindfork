import { mount } from "@vue/test-utils";
import BmExtensionDecorator from "../src/BmExtensionDecorator";

const html = `<span>Hello</span>`;

describe("BmExtensionDecorator without extension", () => {
    test("No modification of html", () => {
        const wrapper = mount(BmExtensionDecorator, {
            propsData: {
                extensions: []
            },
            slots: {
                default: html
            }
        });
        expect(wrapper.html()).toBe(html);
    });
});

describe("BmExtensionDecorator with extensions", () => {
    test("Modification of html with one extension", () => {
        const ExtensionOne = {
            render: function (h) {
                return h("div", {}, this.$slots.default);
            }
        };
        const html = `<span>Hello</span>`;
        const wrapper = mount(BmExtensionDecorator, {
            propsData: {
                extensions: [{ name: "extension-one", path: "dummy-path" }]
            },
            components: { ExtensionOne },
            slots: {
                default: html
            }
        });
        expect(wrapper.html()).toBe(`<div>${html}</div>`);
    });

    test("Modification of html with several extensions", () => {
        const ExtensionOne = {
            render: function (h) {
                return h("div", { class: ["three"] }, ["Greetings", this.$slots.default], this.$slots.default);
            }
        };
        const ExtensionTwo = {
            render: function (h) {
                return h("div", { class: ["two"] }, [this.$slots.default], this.$slots.default);
            }
        };
        const ExtensionThree = {
            render: function (h) {
                return h("div", {}, this.$slots.default);
            }
        };
        const html = `<span>Hello</span>`;
        const wrapper = mount(BmExtensionDecorator, {
            propsData: {
                extensions: [
                    { name: "extension-one", path: "dummy-path" },
                    { name: "extension-two", path: "dummy-path" },
                    { name: "extension-three", path: "dummy-path" }
                ]
            },
            components: { ExtensionOne, ExtensionTwo, ExtensionThree },
            slots: {
                default: html
            }
        });
        expect(wrapper.html()).toMatchInlineSnapshot(`
            "<div class=\\"three\\">Greetings<div class=\\"two\\">
                <div><span>Hello</span></div>
              </div>
            </div>"
        `);
    });

    test("Replacement of html", () => {
        const ExtensionOne = {
            render: function (h) {
                return h("div", {}, ["Hi"]);
            }
        };
        const html = `<span>Hello</span>`;
        const wrapper = mount(BmExtensionDecorator, {
            propsData: {
                extensions: [{ name: "extension-one", path: "dummy-path" }]
            },
            components: { ExtensionOne },
            slots: {
                default: html
            }
        });
        expect(wrapper.html()).toBe(`<div>Hi</div>`);
    });
});
