const imageTagRegex = /<img[\s\S]+?src\s*=\s*['"]http.*?['"]/i;
const imageBackgroundRegex = /background\s*:[\s\S]*?url\(\s*['"]http.*?['"]/i;

export function hasRemoteImages(html) {
    return imageTagRegex.test(html) || imageBackgroundRegex.test(html);
}

export function blockRemoteImages(html) {
    const htmlDocument = new DOMParser().parseFromString(html, "text/html");
    block(htmlDocument);
    return htmlDocument.documentElement.querySelector("body").innerHTML;
}

export function unblockRemoteImages(html) {
    const htmlDocument = new DOMParser().parseFromString(html, "text/html");
    unblock(htmlDocument);
    return htmlDocument.documentElement.querySelector("body").innerHTML;
}

const blockedImageClass = "blocked-image";
const blockedBackgroundClass = "blocked-background";
const blockedImageSrc = "//:0";

function block(htmlDoc) {
    for (let img of htmlDoc.images) {
        const src = img.getAttribute("src");
        const isRemoteImage = src && /^http/i.test(src);
        if (isRemoteImage) {
            img.setAttribute("data-src", img.getAttribute("src"));
            img.setAttribute("src", blockedImageSrc);

            // Firefox CSS restriction: must set an alt attribute value in order to be able to play with ::before
            if (!img.getAttribute("alt")) {
                img.setAttribute("alt", " ");
            }

            img.classList.add(blockedImageClass);
        }
    }

    const backgrounds = htmlDoc.querySelectorAll("*[style*='background'i][style*='url('i][style*='http'i]");
    for (let background of backgrounds) {
        const style = background.getAttribute("style");
        background.setAttribute("data-style", style);
        const modifiedStyle = style.replace(/(.*)(background.*?;)(.*)/i, (p1, p2, p3, p4) => `${p2}${p4}`);
        background.setAttribute("style", modifiedStyle);
        background.classList.add(blockedBackgroundClass);
    }
}

function unblock(htmlDoc) {
    for (let img of htmlDoc.querySelectorAll("." + blockedImageClass)) {
        img.setAttribute("src", img.getAttribute("data-src"));
        img.classList.remove(blockedImageClass);
        if (img.classList.length === 0) {
            img.removeAttribute("class");
        }
        if (img.getAttribute("alt") === " ") {
            img.removeAttribute("alt");
        }
    }

    for (let elementWithBackground of htmlDoc.querySelectorAll("." + blockedBackgroundClass)) {
        elementWithBackground.setAttribute("style", elementWithBackground.getAttribute("data-style"));
        elementWithBackground.classList.remove(blockedBackgroundClass);
        if (elementWithBackground.classList.length === 0) {
            elementWithBackground.removeAttribute("class");
        }
    }
}
