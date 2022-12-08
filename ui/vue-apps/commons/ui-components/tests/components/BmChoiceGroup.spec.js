import { mount } from "@vue/test-utils";
import BmChoiceGroup from "../../src/components/BmChoiceGroup";

describe("BmChoiceGroup", () => {
    let wrapper;
    const options = [
        { text: "Initial", value: "choice1" },
        { text: "Other", value: "choice2" },
        { text: "Link", value: "choice3", href: "#link" },
        { text: "Router-Link", value: "choice4", to: "#routerLink" },
        { text: "Disabled", value: "choice5", disabled: true }
    ];

    beforeEach(() => {
        wrapper = mount(BmChoiceGroup, {
            propsData: {
                options: options,
                selected: options[0]
            }
        });
    });

    test("is a Vue instance", () => {
        expect(wrapper.vm).toBeTruthy();
    });

    test("initial selection", () => {
        expect(wrapper.vm.selectedOption).toBe("choice1");
    });

    test("click options", () => {
        // FYI: see https://vue-test-utils.vuejs.org/api/#selectors for sibling selector
        const button2 = wrapper.find(".btn + .btn");
        button2.trigger("click");
        expect(wrapper.emitted().select).toBeTruthy();
        // FYI: 'emitted' return the object { eventName: [[firstEventArg1], [secondEventArg1, secondEventArg2]] }
        expect(wrapper.emitted().select[0][0]).toBe(1);

        const button4 = wrapper.find(".btn + .btn + .btn + .btn");
        button4.trigger("click");
        expect(wrapper.emitted().select).toBeTruthy();
        expect(wrapper.emitted().select[1][0]).toBe(3);
    });
});
