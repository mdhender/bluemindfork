const FONT_FAMILIES = [
    {
        value: "Red Hat Mono, Courier New, Courier, Lucida Sans Typewriter, Lucida Typewriter, monospace",
        text: "Mono",
        id: "mono"
    },
    {
        value: "Montserrat, montserrat, Source Sans, Helvetica Neue, Helvetica, Arial, sans-serif",
        text: "Montserrat",
        id: "montserrat"
    },
    {
        value: "Garamond, Apple Garamond, Palatino Linotype, Palatino, Baskerville, Baskerville Old Face, serif",
        text: "Garamond",
        id: "garamond"
    },
    {
        value: "Georgia, Constantia, Lucida Bright, Lucidabright, Lucida Serif, Lucida, DejaVu Serif, serif",
        text: "Georgia",
        id: "georgia"
    },
    {
        value: "Helvetica Neue, Helvetica, Nimbus Sans, Arial, sans-serif",
        text: "Helvetica",
        id: "helvetica"
    },
    {
        value: "Verdana, Verdana Ref, Corbel, Lucida Grande, Lucida Sans Unicode, Lucida Sans, DejaVu Sans, Liberation Sans, sans-serif",
        text: "Verdana",
        id: "verdana"
    }
];
export default FONT_FAMILIES;

export function fontFamilyByID(fontId) {
    return FONT_FAMILIES.find(family => family.id === fontId).value;
}
