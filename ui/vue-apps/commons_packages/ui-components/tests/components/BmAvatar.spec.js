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

import BmAvatar from "../../src/components/BmAvatar";

describe("BmAvatar", () => {
    let example = "Yeah Yeah";

    function defaultMount() {
        return mount(BmAvatar, {
            propsData: {
                alt: example
            }
        });
    }

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("BmAvatar should match snapshot", () => {
        expect(defaultMount().vm.$el).toMatchSnapshot();
    });

    test("Simple BmAvatar should contain value first letter", () => {
        const wrapper = defaultMount();

        expect(wrapper.html()).toMatch(new RegExp('.*<p [^>]*id="letter"[^>]*>' + example[0].toUpperCase() + "</p>.*"));
    });
});
