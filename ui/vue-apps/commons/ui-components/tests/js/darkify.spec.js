import { getDarkifiedCss, getUndarkifiedCss, darkifyHtml, undarkifyHtml } from "../../src/js/theming/darkify";

describe("darkify and undarkify CSS", () => {
    // Hexadecimal color

    test("darkify hexadecimal color", () => {
        const map = new Map();
        expect(getDarkifiedCss("color: #aB1234; color: #A12;", undefined, map)).toBe(
            "color: var(--hex_ab1234); color: var(--hex_a12);"
        );
        expect(map.get("--hex_ab1234")).toBe("rgb(255, 125, 135)");
        expect(map.get("--hex_a12")).toBe("rgb(255, 126, 117)");
    });

    test("undarkify hexadecimal color", () => {
        expect(getUndarkifiedCss("color: var(--hex_ab1234); color: var(--hex_a12);")).toBe(
            "color: #ab1234; color: #a12;"
        );
    });

    // RGBA color

    test("darkify rgba color", () => {
        const map = new Map();
        expect(
            getDarkifiedCss(
                `
            border-top: 1px solid rgb(255, 0, 0);
            border-left: 1px solid rgb(0 255 0);
            border-right: 1px dashed rgba(0, 0, 0, 0.42);
            border-bottom: 1px dashed rgba(0 0 0 / .42);
            --rgba_42: RGB(100%, 0%, 0%);
            --rgba_43: rgb(+.1% 100% 0%);
            --rgba_44: rgba(0%, 0%, 0%, 42%);
            --rgba_45: rgba(45% 44% 43% / 42%);
        `,
                undefined,
                map
            )
        ).toBe(
            `
            border-top: 1px solid var(--rgb_255_0_0);
            border-left: 1px solid var(--rgb_0_255_0);
            border-right: 1px dashed var(--rgba_0_0_0_0-42);
            border-bottom: 1px dashed var(--rgba_0_0_0_-42);
            --rgba_42: var(--rgb_100p_0p_0p);
            --rgba_43: var(--rgb_-1p_100p_0p);
            --rgba_44: var(--rgba_0p_0p_0p_42p);
            --rgba_45: var(--rgba_45p_44p_43p_42p);
        `
        );
        expect(map.get("--rgb_255_0_0")).toBe("rgb(255, 39, 17)");
        expect(map.get("--rgb_0_255_0")).toBe("rgb(0, 94, 0)");
        expect(map.get("--rgba_0_0_0_0-42")).toBe("rgba(255, 255, 255, 0.42)");
        expect(map.get("--rgba_0_0_0_-42")).toBe("rgba(255, 255, 255, 0.42)");
        expect(map.get("--rgb_100p_0p_0p")).toBe("rgb(255, 39, 17)");
        expect(map.get("--rgb_-1p_100p_0p")).toBe("rgb(0, 94, 0)");
        expect(map.get("--rgba_0p_0p_0p_42p")).toBe("rgba(255, 255, 255, 0.42)");
        expect(map.get("--rgba_45p_44p_43p_42p")).toBe("rgba(154, 151, 149, 0.42)");
    });

    test("undarkify rgba color", () => {
        expect(
            getUndarkifiedCss(
                `
            border-top: 1px solid var(--rgb_255_0_0);
            border-left: 1px solid var(--rgb_0_255_0);
            border-right: 1px dashed var(--rgba_0_0_0_0-42);
            border-bottom: 1px dashed var(--rgba_0_0_0_-42);
            --rgba_42: var(--rgb_100p_0p_0p);
            --rgba_43: var(--rgb_-1p_100p_0p);
            --rgba_44: var(--rgba_0p_0p_0p_42p);
            --rgba_45: var(--rgba_45p_44p_43p_42p);
        `
            )
        ).toBe(
            `
            border-top: 1px solid rgb(255, 0, 0);
            border-left: 1px solid rgb(0, 255, 0);
            border-right: 1px dashed rgba(0, 0, 0, 0.42);
            border-bottom: 1px dashed rgba(0, 0, 0, .42);
            --rgba_42: rgb(100%, 0%, 0%);
            --rgba_43: rgb(.1%, 100%, 0%);
            --rgba_44: rgba(0%, 0%, 0%, 42%);
            --rgba_45: rgba(45%, 44%, 43%, 42%);
        `
        );
    });

    // HSLA color

    test("darkify hsla color", () => {
        const map = new Map();
        expect(
            getDarkifiedCss(
                `
            background:
                linear-gradient(1deg, hsl(272, 100%, 50%), hsl(272 100% 50%) 70.01%),
                linear-gradient(2deg, hsla(272, 100%, 50%, 0.1), hsla(272 100% 50% / 0.1) 70.02%),
                linear-gradient(3deg, HSLA(180Deg, 100%, 50%, 0.1), HSLA(180Deg 100% 50% / 0.1) 70.03%),
                linear-gradient(4deg, hsl(-240, +100%, -50%, +0.1), hsl(-240 +100% -50% / +0.1) 70.04%),
                linear-gradient(5deg, hsl(240.5,.5%,49.999%,0.42) hsl(240.5 .5% 49.999%/0.42) 70.05%);
        `,
                undefined,
                map
            )
        ).toBe(
            `
            background:
                linear-gradient(1deg, var(--hsl_272_100p_50p), var(--hsl_272_100p_50p) 70.01%),
                linear-gradient(2deg, var(--hsla_272_100p_50p_0-1), var(--hsla_272_100p_50p_0-1) 70.02%),
                linear-gradient(3deg, var(--hsla_180d_100p_50p_0-1), var(--hsla_180d_100p_50p_0-1) 70.03%),
                linear-gradient(4deg, var(--hsl_m240_100p_m50p_0-1), var(--hsl_m240_100p_m50p_0-1) 70.04%),
                linear-gradient(5deg, var(--hsl_240-5_-5p_49-999p_0-42) var(--hsl_240-5_-5p_49-999p_0-42) 70.05%);
        `
        );
        expect(map.get("--hsl_272_100p_50p")).toBe("rgb(215, 99, 255)");
        expect(map.get("--hsla_272_100p_50p_0-1")).toBe("rgba(215, 99, 255, 0.1)");
        expect(map.get("--hsla_180d_100p_50p_0-1")).toBe("rgba(0, 83, 87, 0.1)");
        expect(map.get("--hsl_m240_100p_m50p_0-1")).toBe("rgba(255, 255, 255, 0.1)");
        expect(map.get("--hsl_240-5_-5p_49-999p_0-42")).toBe("rgba(140, 140, 141, 0.42)");
    });

    test("undarkify hsla color", () => {
        expect(
            getUndarkifiedCss(
                `
            background:
                linear-gradient(1deg, var(--hsl_272_100p_50p), var(--hsl_272_100p_50p) 70.01%),
                linear-gradient(2deg, var(--hsla_272_100p_50p_0-1), var(--hsla_272_100p_50p_0-1) 70.02%),
                linear-gradient(3deg, var(--hsla_180d_100p_50p_0-1), var(--hsla_180d_100p_50p_0-1) 70.03%),
                linear-gradient(4deg, var(--hsl_m240_100p_m50p_0-1), var(--hsl_m240_100p_m50p_0-1) 70.04%),
                linear-gradient(5deg, var(--hsl_240-5_-5p_49-999p_42-50p) var(--hsl_240-5_-5p_49-999p_42-50p) 70.05%);
        `
            )
        ).toBe(
            `
            background:
                linear-gradient(1deg, hsl(272, 100%, 50%), hsl(272, 100%, 50%) 70.01%),
                linear-gradient(2deg, hsla(272, 100%, 50%, 0.1), hsla(272, 100%, 50%, 0.1) 70.02%),
                linear-gradient(3deg, hsla(180deg, 100%, 50%, 0.1), hsla(180deg, 100%, 50%, 0.1) 70.03%),
                linear-gradient(4deg, hsl(-240, 100%, -50%, 0.1), hsl(-240, 100%, -50%, 0.1) 70.04%),
                linear-gradient(5deg, hsl(240.5, .5%, 49.999%, 42.50%) hsl(240.5, .5%, 49.999%, 42.50%) 70.05%);
        `
        );
    });

    // Keyword color

    test("darkify keyword color", () => {
        const map = new Map();
        expect(getDarkifiedCss("background: linear-gradient(0.25turn, Orange, goldenrod, gray);", undefined, map)).toBe(
            "background: linear-gradient(0.25turn, var(--color_orange), var(--color_goldenrod), var(--color_gray));"
        );
        expect(map.get("--color_orange")).toBe("rgb(150, 79, 0)");
        expect(map.get("--color_goldenrod")).toBe("rgb(139, 98, 0)");
        expect(map.get("--color_gray")).toBe("rgb(139, 139, 139)");
    });

    test("undarkify keyword color", () => {
        expect(
            getUndarkifiedCss(
                "background: linear-gradient(0.25turn, var(--color_orange), var(--color_goldenrod), var(--color_gray));"
            )
        ).toBe("background: linear-gradient(0.25turn, orange, goldenrod, gray);");
    });

    // Deprecated CSS2 color

    test("darkify CSS2 color", () => {
        const map = new Map();
        expect(
            getDarkifiedCss("color: windowtext; background: menu; text-decoration-color: graytext;", undefined, map)
        ).toBe(
            "color: var(--color_windowtext); background: var(--color_menu); text-decoration-color: var(--color_graytext);"
        );
        expect(map.get("--color_windowtext")).toBe("var(--neutral-fg-hi1)");
        expect(map.get("--color_menu")).toBe("var(--surface-hi1)");
        expect(map.get("--color_graytext")).toBe("var(--neutral-fg-lo2)");
    });

    // Complex CSS

    test("darkify complex CSS", () => {
        const map = new Map();
        expect(
            getDarkifiedCss(
                `
        background: linear-gradient(0.25turn, rebeccapurple, rgba(150, calc(var(--some-var, 42) + 0.2), 0, 0.42));
        border: 1px solid black;
        `,
                undefined,
                map
            )
        ).toBe(`
        background: linear-gradient(0.25turn, var(--color_rebeccapurple), rgba(150, calc(var(--some-var, 42) + 0.2), 0, 0.42));
        border: 1px solid var(--color_black);
        `);
        expect(map.get("--color_rebeccapurple")).toBe("rgb(217, 157, 255)");
        expect(map.get("--color_black")).toBe("rgb(255, 255, 255)");
    });

    test("undarkify complex CSS", () => {
        expect(
            getUndarkifiedCss(`
        background: linear-gradient(0.25turn, var(--color_rebeccapurple), rgba(150, calc(var(--some-var, 42) + 0.2), 0, 0.42));
        border: 1px solid var(--color_black);
        `)
        ).toBe(`
        background: linear-gradient(0.25turn, rebeccapurple, rgba(150, calc(var(--some-var, 42) + 0.2), 0, 0.42));
        border: 1px solid black;
        `);
    });
});

describe("darkify and undarkify HTML", () => {
    const map = new Map();
    const original = document.createElement("html");
    original.innerHTML = `
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
                    <font face="verdana" color="#123456">Hello</font>
                    <span class="blue">world!</span>
                    <svg>
                        <rect x="10" y="10" width="100" height="100" stroke="blue" fill="hsl(272, 100%, 76%)" fill-opacity="0.5"></rect>
                    </svg>
                </div>
            </div>
        </body>
    `;
    const darkified = original.cloneNode(true);
    darkifyHtml(darkified, undefined, map);

    const undarkified = darkified.cloneNode(true);
    undarkifyHtml(undarkified);

    test("darkify HTML style element in head", () => {
        expect(darkified.querySelector("head style").textContent).toBe(
            `
                background: linear-gradient(0.25turn, var(--color_rebeccapurple), var(--rgba_150_42_0_0-42));
                border: 1px solid var(--color_black);
            `
        );
        expect(map.get("--color_rebeccapurple")).toBe("rgb(217, 157, 255)");
        expect(map.get("--rgba_150_42_0_0-42")).toBe("rgba(255, 144, 96, 0.42)");
        expect(map.get("--color_black")).toBe("rgb(255, 255, 255)");
    });
    test("undarkify HTML style element in head", () => {
        expect(undarkified.querySelector("head style").textContent).toBe(
            `
                background: linear-gradient(0.25turn, rebeccapurple, rgba(150, 42, 0, 0.42));
                border: 1px solid black;
            `
        );
    });

    test("darkify HTML style element in body with insidious selectors", () => {
        expect(darkified.querySelector("body style").textContent).toBe(
            `
                    .blue {
                        color: var(--color_royalblue);
                    }
                    #skyblue {
                        color: var(--color_powderblue);
                    }
                `
        );
        expect(map.get("--color_royalblue")).toBe("rgb(114, 141, 255)");
        expect(map.get("--color_powderblue")).toBe("rgb(36, 82, 88)");
    });
    test("undarkify HTML style element in body with insidious selectors", () => {
        expect(undarkified.querySelector("body style").textContent).toBe(
            `
                    .blue {
                        color: royalblue;
                    }
                    #skyblue {
                        color: powderblue;
                    }
                `
        );
    });

    test("darkify inline style", () => {
        expect(darkified.querySelector("[style]").getAttribute("style")).toBe(
            "background-color: var(--color_yellow); color: var(--color_red)"
        );
        expect(map.get("--color_yellow")).toBe("rgb(56, 62, 0)");
        expect(map.get("--color_red")).toBe("rgb(255, 39, 17)");
    });
    test("undarkify inline style", () => {
        expect(undarkified.querySelector("[style]").getAttribute("style")).toBe("background-color: yellow; color: red");
    });

    test("darkify SVG attributes", () => {
        const svgRectNode = darkified.querySelector("svg rect");
        expect(svgRectNode.getAttribute("stroke")).toBe("var(--color_blue)");
        expect(map.get("--color_blue")).toBe("rgb(199, 133, 255)");
        expect(svgRectNode.getAttribute("fill")).toBe("var(--hsl_272_100p_76p)");
        expect(map.get("--hsl_272_100p_76p")).toBe("rgb(145, 84, 201)");
        expect(svgRectNode.getAttribute("fill-opacity")).toBe("0.5");
    });
    test("undarkify SVG attributes", () => {
        const svgRectNode = undarkified.querySelector("svg rect");
        expect(svgRectNode.getAttribute("stroke")).toBe("blue");
        expect(svgRectNode.getAttribute("fill")).toBe("hsl(272, 100%, 76%)");
        expect(svgRectNode.getAttribute("fill-opacity")).toBe("0.5");
    });

    test("darkify valid attributes", () => {
        expect(darkified.querySelector("h1").getAttribute("text")).toBe("var(--rgb_0_255_0)");
        expect(map.get("--rgb_0_255_0")).toBe("rgb(0, 94, 0)");
        expect(darkified.querySelector("font").getAttribute("color")).toBe("var(--hex_123456)");
        expect(map.get("--hex_123456")).toBe("rgb(184, 209, 253)");
    });
    test("undarkify valid attributes", () => {
        expect(undarkified.querySelector("h1").getAttribute("text")).toBe("rgb(0, 255, 0)");
        expect(undarkified.querySelector("font").getAttribute("color")).toBe("#123456");
    });

    test("darkify twice", () => {
        darkifyHtml(darkified, undefined, map);
        expect(darkified.querySelector("h1").getAttribute("text")).toBe("var(--rgb_0_255_0)");
        expect(map.get("--rgb_0_255_0")).toBe("rgb(0, 94, 0)");
        expect(darkified.querySelector("font").getAttribute("color")).toBe("var(--hex_123456)");
        expect(map.get("--hex_123456")).toBe("rgb(184, 209, 253)");
    });
    test("undarkify twice", () => {
        undarkifyHtml(undarkified);
        expect(undarkified.querySelector("h1").getAttribute("text")).toBe("rgb(0, 255, 0)");
        expect(undarkified.querySelector("font").getAttribute("color")).toBe("#123456");
    });
});
