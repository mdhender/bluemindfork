import { mount } from "@vue/test-utils";

jest.mock("../../src/css/exports/avatar.scss", () => ({
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

import BmChip from "../../src/components/BmChip";

describe("BmChip", () => {
    let closeButtonSelector = ".bm-chip button.close";
    let example = "efzefez@ezfef.fr";

    function defaultMount() {
        return mountBmChip({}, example);
    }

    function mountBmChip(propsData, text) {
        return mount(BmChip, {
            propsData,
            slots: {
                default: text
            },
            mocks: {
                $t: () => {},
                $tc: () => {}
            }
        });
    }

    let regexCloseButton = new RegExp(/<\s*button (.*?)class="(.*?)close(.*?)"(.*?)[^>]*>(.*?)<\s*\/\s*button>/);

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("BmChip should match snapshot", () => {
        expect(defaultMount().vm.$el).toMatchSnapshot();
    });

    test("Simple BmChip display: a text", () => {
        const wrapper = defaultMount();
        expect(wrapper.text()).toContain(example);
        expect(wrapper.html()).not.toMatch(regexCloseButton);
    });

    test("BmChip with a close button", () => {
        const wrapper = mountBmChip({ closeable: true }, example);
        expect(wrapper.text()).toContain(example);
        expect(wrapper.html()).toMatch(regexCloseButton);
    });

    test("When clicking on close button, a remove event is fired", () => {
        const wrapper = mountBmChip({ closeable: true }, example);
        wrapper.find(closeButtonSelector).trigger("click");
        expect(wrapper.emitted().remove).toBeTruthy();
    });

    test("No icon", () => {
        const wrapper = mountBmChip({ closeable: true }, example);
        expect(wrapper.text()).toContain(example);
        expect(wrapper.html()).toMatch(regexCloseButton);
    });
});
