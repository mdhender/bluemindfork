import { darkifyCss, darkifyHtml } from "../../src/js/theming/darkify";

describe("darkifyCss", () => {
    test("hexadecimal color", () => {
        expect(darkifyCss("color: #123456;")).toBe("color: rgb(184, 209, 253);");
    });
    test("rgba color", () => {
        expect(darkifyCss("border: 1px dashed rgb(0, 0, 0, 0.42);")).toBe(
            "border: 1px dashed rgba(255, 255, 255, 0.42);"
        );
    });
    test("keyword color", () => {
        expect(darkifyCss("background: linear-gradient(0.25turn, orange, teal, purple);")).toBe(
            "background: linear-gradient(0.25turn, rgb(150, 79, 0), rgb(63, 164, 164), rgb(255, 146, 255));"
        );
    });
    test("complex case", () => {
        expect(
            darkifyCss(`
        background: linear-gradient(0.25turn, rebeccapurple, rgba(150, calc(var(--some-var, 42) + 0.2), 0, 0.42));
        border: 1px solid black;
        `)
        ).toBe(`
        background: linear-gradient(0.25turn, rgb(217, 157, 255), rgba(150, calc(var(--some-var, 42) + 0.2), 0, 0.42));
        border: 1px solid rgb(255, 255, 255);
        `);
    });
});

describe("darkifyHtml", () => {
    const dom = document.createElement("html");
    dom.innerHTML = `
        <head>
            <style>
                background: linear-gradient(0.25turn, rebeccapurple, rgba(150, 42, 0, 0.42));
                border: 1px solid black;
            </style>
        </head>
        <body>
            <h1 text="rgb(0, 255, 0)">Hello</h1>
            <div style="background-color: yellow; color: red">
                <style>
                    .blue {
                        color: royalblue;
                    }
                    #skyblue {
                        color: powderblue;
                    }
                </style>
                <div id="skyblue" bgcolor="black">
                    <font face="verdana" color="green">Hello</font>
                    <span class="blue">world!</span>
                    <svg>
                        <rect x="10" y="10" width="100" height="100" stroke="blue" fill="purple" fill-opacity="0.5"></rect>
                    </svg>
                </div>
            </div>
        </body>
    `;

    darkifyHtml(dom);

    test("HTML style element in head", () => {
        expect(dom.querySelector("head  style").textContent).toBe(
            `
                background: linear-gradient(0.25turn, rgb(217, 157, 255), rgba(255, 144, 96, 0.42));
                border: 1px solid rgb(255, 255, 255);
            `
        );
    });

    test("HTML style element in body with insidious selectors", () => {
        expect(dom.querySelector("body style").textContent).toBe(
            `
                    .blue {
                        color: rgb(114, 141, 255);
                    }
                    #skyblue {
                        color: rgb(36, 82, 88);
                    }
                `
        );
    });

    test("inline style", () => {
        expect(dom.querySelector("[style]").getAttribute("style")).toBe(
            "background-color: rgb(56, 62, 0); color: rgb(255, 39, 17);"
        );
    });

    test("SVG attributes", () => {
        const svgRectNode = dom.querySelector("svg rect");
        expect(svgRectNode.getAttribute("stroke")).toBe("#C785FF");
        expect(svgRectNode.getAttribute("fill")).toBe("#FF92FF");
        expect(svgRectNode.getAttribute("fill-opacity")).toBe("0.5");
    });

    test("deprecated attributes", () => {
        expect(dom.querySelector("h1").getAttribute("text")).toBe("#005E00");
        expect(dom.querySelector("font").getAttribute("color")).toBe("#4AAF39");
        expect(dom.querySelector("#skyblue").getAttribute("bgcolor")).toBe("#FFFFFF");
    });
});
