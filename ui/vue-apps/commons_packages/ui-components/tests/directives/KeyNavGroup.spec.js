import { mount } from "@vue/test-utils";
import KeyNavGroup from "../../src/directives/KeyNavGroup";

let getClientRectsFn;
describe("BmCheck", () => {
    let wrapper;
    beforeAll(() => {
        // simulate visibility based on getClientRects (JSDOM do not render elements and getClientRects has no value)
        getClientRectsFn = HTMLElement.prototype.getClientRects;
        HTMLElement.prototype.getClientRects = () => ({ length: 999 });
    });

    afterAll(() => {
        HTMLElement.prototype.getClientRects = getClientRectsFn;
    });

    beforeEach(() => {
        wrapper = mount(
            {
                template: `
            <div class="key-nav-group-test">
            <p>Navigate with right and left arrows between elements</p>
            <div>
                Group BigButtons (navigate between buttons)
                <div class="d-flex">
                    <button id="button-a" v-key-nav-group:group-bigbuttons >Nav-able Button A (click me then hit arrow right/left)</button>
                    <div>&nbsp;&nbsp;&nbsp;Input 1<input /></div>
                </div>
                <div class="d-flex">
                    <button id="button-b" v-key-nav-group:group-bigbuttons>Nav-able Button B (click me then hit arrow right/left)</button>
                    <div>&nbsp;&nbsp;&nbsp;Input 2<input /></div>
                </div>
                <div class="d-flex">
                    <button id="button-c" v-key-nav-group:group-bigbuttons>Nav-able Button C (click me then hit arrow right/left)</button>
                    <div>&nbsp;&nbsp;&nbsp;Input 3<input /></div>
                </div>
            </div>
            <br />
            <div>
                Group Rasta (navigate between divs)
                <div class="d-flex">
                    <button>Button X</button>
                    <div id="div-a" v-key-nav-group:group-rasta style="background-color: green">
                        Nav-able Div 1 (click me then hit arrow right/left)
                    </div>
                    &nbsp;&nbsp;&nbsp;Input Alpha<input />
                </div>
                <div class="d-flex">
                    <button>Button Y</button>
                    <div id="div-b" v-key-nav-group:group-rasta style="background-color: yellow">
                        Nav-able Div 2 (click me then hit arrow right/left)
                    </div>
                    &nbsp;&nbsp;&nbsp;Input Tango<input />
                </div>
                <div class="d-flex">
                    <button>Button Z</button>
                    <div id="div-c" v-key-nav-group:group-rasta style="background-color: red">
                        Nav-able Div 3 (click me then hit arrow right/left)
                    </div>
                    &nbsp;&nbsp;&nbsp;Input Charlie<input />
                </div>
            </div>
        </div>`,
                directives: { KeyNavGroup }
            },
            { attachTo: document.body }
        );
        expect(wrapper).toBeTruthy();
    });

    test("KeyNavGroup directive: navigate through buttons", () => {
        testNavigate(wrapper, "button");
    });

    test("KeyNavGroup directive: navigate through divs", () => {
        testNavigate(wrapper, "div");
    });
});

async function testNavigate(wrapper, idPrefix) {
    const elementA = `${idPrefix}-a`;
    const elementB = `${idPrefix}-b`;
    const elementC = `${idPrefix}-c`;

    wrapper.find(`#${elementB}`).trigger("focus");
    expect(document.activeElement.id).toBe(elementB);

    // hit ArrowRight, go next
    wrapper.find(`#${elementB}`).trigger("keydown", { key: "ArrowRight" });
    expect(document.activeElement.id).toBe(elementC);

    //  hit ArrowRight, no next so should return to start
    wrapper.find(`#${elementC}`).trigger("keydown", { key: "ArrowRight" });
    expect(document.activeElement.id).toBe(elementA);

    //  hit ArrowLeft, no previous so should return to end
    wrapper.find(`#${elementA}`).trigger("keydown", { key: "ArrowLeft" });
    expect(document.activeElement.id).toBe(elementC);

    //  hit Shit+ArrowLeft, go start
    wrapper.find(`#${elementC}`).trigger("keydown", { key: "ArrowLeft", shiftKey: true });
    expect(document.activeElement.id).toBe(elementA);

    //  hit Shit+ArrowRight, go end
    wrapper.find(`#${elementA}`).trigger("keydown", { key: "ArrowRight", shiftKey: true });
    expect(document.activeElement.id).toBe(elementC);
}
