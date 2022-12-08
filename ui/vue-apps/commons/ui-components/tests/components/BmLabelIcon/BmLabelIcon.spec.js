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

import BmLabelIcon from "../../../src/components/BmLabelIcon/BmLabelIcon";

describe("BmLabelIcon", () => {
    function defaultMount(slot) {
        return mount(BmLabelIcon, {
            slots: {
                default: slot
            },
            propsData: {
                icon: "folder"
            }
        });
    }

    test("is a Vue instance", () => {
        expect(defaultMount("mySlot").vm).toBeTruthy();
    });

    // FIXME: Cas non pertinent, que faire si le composant est créé sans slot ?
    test("called with an icon", () => {
        let wrapper = mount(BmLabelIcon, {
            propsData: {
                icon: "folder"
            }
        });

        expect(wrapper.find("svg").isVisible()).toBeTrue;
        expect(wrapper.find(".fa-folder").isVisible()).toBeTrue;
    });

    test("called with an icon and slot", () => {
        let mySlot = "Hello World";
        let wrapper = defaultMount(mySlot);

        expect(wrapper.find("svg").isVisible()).toBeTrue;
        expect(wrapper.find(".fa-folder").isVisible()).toBeTrue;

        expect(wrapper.text()).toContain(mySlot);
    });
});
