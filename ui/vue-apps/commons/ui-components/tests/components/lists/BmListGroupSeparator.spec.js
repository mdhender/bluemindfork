import { mount } from "@vue/test-utils";
import BmListGroupSeparator from "../../../src/components/lists/BmListGroupSeparator";

describe("BmListGroupSeparator", () => {
    function defaultMount() {
        return mount(BmListGroupSeparator, {
            propsData: {
                tag: "span"
            }
        });
    }

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("BmListGroupSeparator should match snapshot", () => {
        expect(defaultMount().vm.$el).toMatchSnapshot();
    });

    test("BmListGroupSeparator have a list-group-separator class", () => {
        expect(defaultMount().vm.$el.className).toEqual(expect.stringContaining("list-group-separator"));
    });

    test("BmListGroupSeparator html tag match property", () => {
        expect(defaultMount().vm.$el.tagName).toEqual("SPAN");
    });
});
