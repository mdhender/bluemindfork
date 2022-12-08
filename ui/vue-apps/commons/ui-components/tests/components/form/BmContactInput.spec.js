import { mount } from "@vue/test-utils";

jest.mock("../../../src/css/exports/avatar.scss", () => ({
    1: "#007bff",
    2: "#6610f2",
    3: "#6f42c1",
    4: "#e83e8c",
    5: "#dc3545",
    6: "#fd7e14",
    7: "#ffc107",
    8: "#28a745",
    9: "#20c997",
    10: "#17a2b8",
    11: "#fff",
    12: "#6c757d",
    13: "#343a40"
}));
jest.mock("../../../src/css/exports/colors.scss", () => ({}));

import BmContactInput from "../../../src/components/form/BmContactInput";
import exampleContacts from "../../data/contacts";

class ResizeObserver {
    observe() {}
    unobserve() {}
}
class MutationObserver {
    observe() {}
    unobserve() {}
}
window.ResizeObserver = ResizeObserver;
window.MutationObserver = MutationObserver;

describe("BmContactInput", () => {
    function defaultMount() {
        return mount(BmContactInput, {
            propsData: { validateAddressFn: () => true },
            slots: {
                default: "To"
            },
            mocks: {
                $t: () => {},
                $tc: () => {},
                $te: () => {}
            }
        });
    }

    function mountWithData() {
        return mount(BmContactInput, {
            propsData: {
                contacts: exampleContacts.slice(0, 3),
                autocompleteResults: exampleContacts,
                validateAddressFn: () => true
            },
            slots: {
                default: "To"
            },
            mocks: {
                $t: () => {},
                $tc: () => {},
                $te: () => {}
            },
            sync: false
        });
    }

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("BmContactInput should match snapshot", () => {
        expect(defaultMount().vm.$el).toMatchSnapshot();
    });

    test("BmContactInput displays a label and an input", () => {
        const wrapper = defaultMount();
        expect(wrapper.text()).toContain("To");
        expect(wrapper.html()).toContain("<input");
    });

    test("BmContactInput instanciated with emails also displays contacts", () => {
        const wrapper = mountWithData();
        expect(wrapper.text()).toContain("To");
        expect(wrapper.html()).toContain("<input");
        exampleContacts.slice(0, 3).forEach(contact => {
            expect(wrapper.text()).toContain(contact.dn);
        });
    });

    test("Edit a contact", async () => {
        const wrapper = mountWithData();
        expect(wrapper.findAll(".contact-wrapper").length).toBe(3);
        wrapper.vm.$data.contacts_[0].selected = true; // set a selected contact

        expect(wrapper.findAll(".bm-contact").length).toBe(3);
        expect(wrapper.findAll("input").length).toBe(1);

        wrapper.find(".contact-wrapper").trigger("dblclick");

        await wrapper.vm.$nextTick();
        expect(wrapper.text()).toContain(exampleContacts[1].dn);
        expect(wrapper.findAll(".bm-contact").length).toBe(2);
        expect(wrapper.findAll("input").length).toBe(2);
    });

    test("Edit contact and select it with autocomplete", async () => {
        const wrapper = mountWithData();
        expect(wrapper.findAll(".contact-wrapper").length).toBe(3);

        wrapper.vm.$data.contacts_[0].selected = true; // set a selected contact
        wrapper.vm.$data.contacts_[0].edit = true;

        await wrapper.vm.$nextTick();
        wrapper.find("input[type='text']").trigger("focusin");
        wrapper.find(".suggestions .list-group-item:nth-child(5)").trigger("click"); // click on last autocomplete suggestion

        const updatedContact = wrapper.vm.$data.contacts_[0];
        const expectedContact = exampleContacts[4];
        expect(updatedContact.address).toBe(expectedContact.address);
        expect(updatedContact.dn).toBe(expectedContact.dn);
    });
});
