import { mount } from "@vue/test-utils";
import ResolvedContact from "../src/ResolvedContact";
jest.mock("@bluemind/inject", () => ({
    inject: () => ({
        search: () => Promise.resolve({ values: [{}] }),
        getComplete: () => Promise.resolve({})
    })
}));

global.fetch = jest.fn();

describe("ResolvedContact component", () => {
    test("Mount with address prop", async () => {
        const wrapper = mount(ResolvedContact, {
            propsData: {
                recipient: "Test Address <test.address@bluemind.net>"
            },
            slots: {
                default: "<div>Hello!</div>"
            }
        });

        await wrapper.vm.resolveContact();

        expect(wrapper.vm.resolvedContact).toBeTruthy();
    });

    test("Mount with contact prop", async () => {
        const wrapper = mount(ResolvedContact, {
            propsData: {
                contact: {}
            },
            slots: {
                default: "<div>Hello!</div>"
            }
        });

        await wrapper.vm.resolveContact();

        expect(wrapper.vm.resolvedContact).toBeTruthy();
    });

    test("Mount with uid prop", async () => {
        const wrapper = mount(ResolvedContact, {
            propsData: {
                uid: "contactUid"
            },
            slots: {
                default: "<div>Hello!</div>"
            }
        });

        await wrapper.vm.resolveContact();

        expect(wrapper.vm.resolvedContact).toBeTruthy();
    });

    test("Mount with uid and containerUid props", async () => {
        const wrapper = mount(ResolvedContact, {
            propsData: {
                uid: "contactUid",
                containerUid: "containerUid"
            },
            slots: {
                default: "<div>Hello!</div>"
            }
        });

        await wrapper.vm.resolveContact();

        expect(wrapper.vm.resolvedContact).toBeTruthy();
    });

    test("Mount with containerUid prop but no uid", async () => {
        const wrapper = mount(ResolvedContact, {
            propsData: {
                containerUid: "containerUid"
            },
            slots: {
                default: "<div>Hello!</div>"
            }
        });

        let error;
        try {
            await wrapper.vm.resolveContact();
        } catch (e) {
            error = e;
        }

        expect(wrapper.vm.resolvedContact).toBeFalsy();
        expect(error).not.toBeUndefined();
    });
});
