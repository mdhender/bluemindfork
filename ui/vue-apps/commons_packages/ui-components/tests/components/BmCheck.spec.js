import { mount } from "@vue/test-utils";
import BmCheck from "../../src/components/BmCheck";

describe("BmCheck", () => {
    function defaultMount() {
        return mount(BmCheck);
    }

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("BmCheck should match snapshot", () => {
        expect(defaultMount().vm.$el).toMatchSnapshot();
    });

    test("BmCheck have a bm-check class", () => {
        expect(defaultMount().vm.$el.className).toEqual(expect.stringContaining("bm-check"));
    });
});
