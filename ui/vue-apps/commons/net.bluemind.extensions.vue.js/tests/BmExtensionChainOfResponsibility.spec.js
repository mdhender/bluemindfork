import Vue from "vue";
import { mount } from "@vue/test-utils";
import BmExtensionChainOfResponsibility from "../src/BmExtensionChainOfResponsibility";

const textOne = "Blue";
const textTwo = "Green";
const textThree = "Red";
const defaultText = "Hello";

const ExtensionPriorityOne = {
    props: {
        color: { type: String, required: true },
        next: { type: Function, required: true }
    },
    render: function (h) {
        if (this.color === "blue") {
            return h("h1", textOne);
        } else {
            return this.next();
        }
    }
};
const ExtensionPriorityTwo = {
    props: {
        color: { type: String, required: true },
        next: { type: Function, required: true }
    },
    render: function (h) {
        if (this.color === "green") {
            return h("h1", textTwo);
        } else {
            return this.next();
        }
    }
};
const ExtensionPriorityThree = {
    props: {
        color: { type: String, required: true },
        next: { type: Function, required: true }
    },
    render: function (h) {
        if (this.color === "red") {
            return h("h1", textThree);
        } else {
            return this.next();
        }
    }
};

Vue.component("extension-priority-one", ExtensionPriorityOne);
Vue.component("extension-priority-two", ExtensionPriorityTwo);
Vue.component("extension-priority-three", ExtensionPriorityThree);

describe("BmExtensionChainOfResponsibility with extensions", () => {
    test("Render the extension component if it does not call the 'next' prop function", () => {
        const wrapper = mount(BmExtensionChainOfResponsibility, {
            propsData: {
                color: "blue",
                extensions: [{ name: "extension-priority-one", path: "dummy-path" }]
            },
            slots: {
                default: `<span>${defaultText}</span>`
            }
        });
        expect(wrapper.text()).toBe(textOne);
    });
    test("Within a list of extensions it renders the first extension component which does not call the 'next' prop function", () => {
        const wrapper = mount(BmExtensionChainOfResponsibility, {
            propsData: {
                color: "green",
                extensions: [
                    { name: "extension-priority-one", path: "dummy-path" },
                    { name: "extension-priority-two", path: "dummy-path" },
                    { name: "extension-priority-three", path: "dummy-path" }
                ]
            },
            slots: {
                default: `<span>${textTwo}</span>`
            }
        });
        expect(wrapper.text()).toBe(textTwo);
    });
    test("Render the default slot when all extensions call the 'next' prop function", () => {
        const wrapper = mount(BmExtensionChainOfResponsibility, {
            propsData: {
                color: "blue",
                extensions: [
                    { name: "extension-priority-two", path: "dummy-path" },
                    { name: "extension-priority-three", path: "dummy-path" }
                ]
            },
            slots: {
                default: `<span>${defaultText}</span>`
            }
        });
        expect(wrapper.text()).toBe(defaultText);
    });
    test("Render the default slot when there is no extension", () => {
        const wrapper = mount(BmExtensionChainOfResponsibility, {
            propsData: {
                color: "blue"
            },
            scopedSlots: {
                default: `<span>${defaultText}</span>`
            }
        });
        expect(wrapper.text()).toBe(defaultText);
    });
});
