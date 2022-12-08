function createClipPathCircle(ns) {
    const clipPath = document.createElementNS(ns, "clipPath");
    clipPath.setAttributeNS(null, "id", "bm-clip-path-circle");
    clipPath.setAttributeNS(null, "clipPathUnits", "objectBoundingBox");
    clipPath.innerHTML = '<circle  cx="0.5" cy="0.5" r="0.5" />';
    return clipPath;
}

function createClipPathHexagon(ns) {
    const clipPath = document.createElementNS(ns, "clipPath");
    clipPath.setAttributeNS(null, "id", "bm-clip-path-hexagon");
    clipPath.setAttributeNS(null, "clipPathUnits", "userSpaceOnUse");
    clipPath.innerHTML = '<polygon points="50,2 100,31 100,89 50,118 0,89 0,31 50,2" />';
    return clipPath;
}

function createClipPathHexagonAdjusted(ns) {
    const clipPath = document.createElementNS(ns, "clipPath");
    clipPath.setAttributeNS(null, "id", "bm-clip-path-hexagon-adjusted");
    clipPath.setAttributeNS(null, "clipPathUnits", "userSpaceOnUse");
    clipPath.innerHTML = `<polygon points="50,0 100,30 100,90 50,120 0,90 0,30 50,0" />`;
    return clipPath;
}

function createClipPathCountHexagon(ns) {
    const clipPath = document.createElementNS(ns, "clipPath");
    clipPath.setAttributeNS(null, "id", "bm-clip-path-count-hexagon");
    clipPath.setAttributeNS(null, "clipPathUnits", "userSpaceOnUse");
    clipPath.innerHTML = `
        <polygon points="50,2 100,31 100,36 50,7 0,36 0,31 50,2" />
        <polygon points="50,118 0,89 0,84 50,113 100,84 100,89 50,118" />
        `;
    return clipPath;
}

function createClipPathCountHexagonAdjusted(ns) {
    const clipPath = document.createElementNS(ns, "clipPath");
    clipPath.setAttributeNS(null, "id", "bm-clip-path-count-hexagon-adjusted");
    clipPath.setAttributeNS(null, "clipPathUnits", "userSpaceOnUse");
    clipPath.innerHTML = `
        <polygon points="50,2 100,31 100,36 50,7 0,36 0,31 50,2" />
        <polygon points="50,118 0,89 0,84 50,113 100,84 100,89 50,118" />
        `;
    return clipPath;
}

function createMaskStatus(ns) {
    const mask = document.createElementNS(ns, "mask");
    mask.setAttributeNS(null, "id", "bm-mask-status");
    mask.setAttributeNS(null, "maskContentUnits", "userSpaceOnUse");
    mask.innerHTML = `
        <rect x="0" y="0" width="100" height="120" fill="white"></rect>
        <circle cx="100" cy="20" r="20" fill="black"></circle>
        `;
    return mask;
}

export default {
    bind(el, binding) {
        var ns = "http://www.w3.org/2000/svg";
        if (!document.getElementById("bm-clip-path-shapes")) {
            const defs = document.createElementNS(ns, "defs");

            const shape = document.createElementNS(ns, "svg");
            shape.setAttributeNS(null, "id", "bm-clip-path-shapes");
            shape.setAttributeNS(null, "width", "0");
            shape.setAttributeNS(null, "height", "0");
            shape.setAttributeNS(null, "aria-hidden", "true");
            shape.style.position = "absolute";
            shape.appendChild(defs);

            defs.appendChild(createClipPathCircle(ns));
            defs.appendChild(createClipPathHexagon(ns));
            defs.appendChild(createClipPathHexagonAdjusted(ns));
            defs.appendChild(createClipPathCountHexagon(ns));
            defs.appendChild(createClipPathCountHexagonAdjusted(ns));

            defs.appendChild(createMaskStatus(ns));

            document.body.appendChild(shape);
        }
        let { clip, mask } = typeof binding.value !== "object" ? { clip: binding.value, mask: "none" } : binding.value;
        if (clip && clip !== "none") {
            el.style.clipPath = `url(#bm-clip-path-${clip})`;
        }
        if (mask && mask !== "none") {
            el.style.mask = `url(#bm-mask-${mask})`;
        }
        let align = "";
        let ratio = "";

        if (binding.modifiers.slice) {
            ratio = "slice";
        }
        if (binding.modifiers.top) {
            align = "xMidYMin";
        }
        if (binding.modifiers.left) {
            align = "xMaxYMid";
        }
        if (binding.modifiers.bottom) {
            align = "xMidYMax";
        }
        if (binding.modifiers.right) {
            align = "xMinYMid";
        }
        if (binding.modifiers.extend) {
            align = "";
            ratio = "none";
        }
        if (ratio || align) {
            el.setAttributeNS(null, "preserveAspectRatio", align + " " + ratio);
        }
    }
};
