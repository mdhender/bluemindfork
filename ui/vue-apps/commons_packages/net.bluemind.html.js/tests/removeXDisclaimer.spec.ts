import { removeXDisclaimer } from "../src/removeXDisclaimer";

describe("Remove Disclaimers", () => {
    it("removes x-disclaimer content wrapped in a bm-composer class", () => {
        expect(
            removeXDisclaimer(
                "<div class=bm-composer-wrapper> <div class=x-disclaimer-XXXX>JE SUIS UNE SIGNATURE</div> </div>"
            )
        ).toEqual(`<div class="bm-composer-wrapper">  </div>`);
    });

    it("does not take x-disclaimer outside of bm-composer wrapper", () => {
        expect(removeXDisclaimer("<div> <div class=x-disclaimer-XXXX>JE SUIS UNE SIGNATURE</div> </div>")).toEqual(
            `<div> <div class="x-disclaimer-XXXX">JE SUIS UNE SIGNATURE</div> </div>`
        );
    });

    it("removes only x-disclaimers elements of first level", () => {
        expect(
            removeXDisclaimer(
                "<div class=bm-composer-wrapper>" +
                "<div class=x-disclaimer-XXXX>JE SUIS UNE SIGNATURE</div>" +
                "<div class=bm-composer-wrapper> <div class=x-disclaimer-XXXX>JE SUIS UNE SIGNATURE</div>" +
                "</div>" +
                "</div>"
            )
        ).toEqual(
            '<div class="bm-composer-wrapper">' +
            '<div class="bm-composer-wrapper"> <div class="x-disclaimer-XXXX">JE SUIS UNE SIGNATURE</div>' +
            "</div>" +
            "</div>"
        );
    });
});
