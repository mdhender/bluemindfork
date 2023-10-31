import { mount } from "@vue/test-utils";
import BmNotice from "../../src/components/BmNotice";

describe("BmNotice", () => {
    function defaultMount() {
        return mount(BmNotice);
    }

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("should match snapshot", () => {
        expect(defaultMount().vm.$el).toMatchSnapshot();
    });

    test("should have a bm-alert class", () => {
        expect(defaultMount().vm.$el.className).toEqual(expect.stringContaining("bm-notice"));
    });
});
