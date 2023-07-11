import Highlight from "../../src/directives/Highlight";
import { mount } from "@vue/test-utils";
describe("HIGHLIGHT Directive", () => {
    it("should return unchanged html if search is empty ", () => {
        const wrapper = fixture();
        expect(wrapper.inlineHTML).toMatch("<div><p>TEXT</p></div>");
    });
    it("should return to initial render if search is erased/removed", async () => {
        const wrapper = fixture();
        await wrapper.wrapper.setData({ word: "EXT" });
        await wrapper.wrapper.setData({ word: "" });

        expect(wrapper.inlineHTML).toMatch("<div><p>TEXT</p></div>");
    });
    it("highlight text matching search", () => {
        const wrapper = fixture("word", { search: "wo" });
        expect(wrapper.inlineHTML).toMatch(
            "<div>" +
                "<p>" +
                '<span data-highlight-directive-highlighted-node="word"><mark>wo</mark>rd</span>' +
                "</p>" +
                "</div>"
        );
    });
    it("is not case sensitive ", async () => {
        const wrapper = fixture("WORD", { search: "wo" });

        expect(wrapper.inlineHTML).toMatch(
            '<div><p><span data-highlight-directive-highlighted-node="WORD"><mark>WO</mark>RD</span></p></div>'
        );
    });
});

function fixture(text = undefined, options = {}) {
    const DEFAULT_TEXT = "TEXT";
    const DEFAULT_TEMPLATE = () => `<div v-highlight="word"><p>${text ?? DEFAULT_TEXT}</p></div>`;

    const wrapper = mount({
        template: DEFAULT_TEMPLATE(),
        directives: { Highlight },
        data: () => ({ word: options.search ?? "" })
    });

    return {
        wrapper,
        get inlineHTML() {
            return wrapper
                .html()
                .split("\n")
                .reduce((h, s) => h + s.trim(), "");
        }
    };
}
