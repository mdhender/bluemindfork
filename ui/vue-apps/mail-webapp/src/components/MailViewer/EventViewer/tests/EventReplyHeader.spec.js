import { mount } from "@vue/test-utils";
import ReplyHeader from "../EventReplyHeader.vue";
describe("Invitaiton Insert -> Reply Header", () => {
    let wrapper;
    beforeEach(() => {
        wrapper = EventReplyHeader();
    });
    it("is a vue Instance", () => {
        const localWrapper = wrapper.mount();
        expect(localWrapper.vm).toBeDefined();
    });
    it("should have a button to accept", () => {
        const replyHeader = wrapper.mount();
        expect(replyHeader.findAll('[type="button"]').at(0).text()).toEqual("accept");
    });
    it("should have a button to decline", () => {
        const replyHeader = wrapper.mount();
        expect(replyHeader.findAll('[type="button"]').at(2).text()).toEqual("decline");
    });
    it("should have a button to answer maybe", () => {
        const replyHeader = wrapper.mount();
        expect(replyHeader.findAll('[type="button"]').at(1).text()).toEqual("tentatively");
    });

    it("any of this button should call for a status change ", async () => {
        const replyHeader = wrapper.mount();
        await replyHeader.find("[type='button']").trigger("click");

        expect(wrapper.vuexActionSpy.length).toEqual(1);
    });
    it("button accpeted should call for return message with status at `Accepted`  ", async () => {
        const replyHeader = wrapper.mount();
        await replyHeader.find("[type='button']").trigger("click");

        expect(wrapper.statusSendArgument).toEqual("Accepted");
    });
    it("button maybe should call for return message with status at `tentative`  ", async () => {
        const replyHeader = wrapper.mount();
        await replyHeader.findAll("[type='button']").at(1).trigger("click");

        expect(wrapper.statusSendArgument).toEqual("Tentative");
    });
    it("button decline should call for return message with status at `decline`  ", async () => {
        const replyHeader = wrapper.mount();
        await replyHeader.findAll("[type='button']").at(2).trigger("click");

        expect(wrapper.statusSendArgument).toEqual("Declined");
    });
});

function EventReplyHeader() {
    let mountedComponent = undefined;

    return {
        get statusSendArgument() {
            return this.vuexActionSpy[0][0];
        },
        get vuexActionSpy() {
            return mountedComponent.emitted("event-replied");
        },
        get wrapper() {
            if (!mountedComponent) throw new Error("Component must be mounted first!");

            return mountedComponent;
        },
        mount() {
            mountedComponent = mount(ReplyHeader, {
                propsData: {
                    currentEvent: {
                        loading: "LOADED",
                        summary: "SUMMARY TEXT",
                        date: "toutes les 2 semaines, le lundi, mardi, mercredi, jeudi et vendredi jusqu’au jeu. 11/08/22",
                        status: "?",
                        serverEvent: {
                            value: {
                                main: {
                                    dtstart: { iso8601: new Date(2023, 0, 1, 9, 0).toISOString() },
                                    dtend: { iso8601: new Date(2023, 0, 1, 10, 0).toISOString() }
                                }
                            }
                        }
                    },
                    message: {
                        eventInfo: {
                            needsReply: true
                        }
                    }
                },
                mocks: {
                    $t: path => {
                        return path.split(".").pop();
                    }
                }
            });
            return mountedComponent;
        }
    };
}
