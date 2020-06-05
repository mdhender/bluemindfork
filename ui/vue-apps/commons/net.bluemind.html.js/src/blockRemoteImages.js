export function hasRemoteImages(html) {
    return /<img[\s\S]+?src\s*=\s*['"]http.*?['"]/i.test(html);
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
}
