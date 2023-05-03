import { mount } from "@vue/test-utils";
import BmFormFontSelector from "../../src/components/form/BmFormFontSelector";

describe("BMFormFontSelector", () => {
    function defaultMount(props) {
        return mount(BmFormFontSelector, {
            propsData: {
                selected: "mono",
                defaultFont: "mono",
                ...props
            },
            mocks: {
                $t: () => {
                    return "Default";
                }
            }
        });
    }

    test("is a Vue instance", () => {
        const wrapper = defaultMount();
        expect(wrapper.vm).toBeTruthy();
    });

    test("display selected font", () => {
        const wrapper = defaultMount();
        expect(wrapper.find("button").text().toLowerCase()).toBe("mono");
    });

    test("display different selected font if passed through props", () => {
        const wrapper = defaultMount({ selected: "Garamond" });
        expect(wrapper.find("button").text().toLowerCase()).toBe("garamond");
    });

    test("selecting another font should update selected fontFamily displayed in dropdown button", async () => {
        const wrapper = defaultMount();

        const dropdownItemToBeSelected = wrapper.findAll("[role=menuitem]").at(3);
        await dropdownItemToBeSelected.trigger("click");

        expect(wrapper.find("button").text()).toBe(dropdownItemToBeSelected.text());
    });

    test("input event is emitted when selection change", async () => {
        const wrapper = defaultMount();
        await wrapper.findAll("[role=menuitem]").at(3).trigger("click"); // at(3) === GEORGIA

        expect(wrapper.emitted("input")).toBeTruthy();
        expect(wrapper.emitted("input")[0][0]).toHaveProperty("id");
        expect(wrapper.emitted("input")[0][0]).toHaveProperty("text");
        expect(wrapper.emitted("input")[0][0]).toHaveProperty("value");
        expect(wrapper.emitted("input")[0][0]).toMatchObject({ id: "georgia" });
    });
});
