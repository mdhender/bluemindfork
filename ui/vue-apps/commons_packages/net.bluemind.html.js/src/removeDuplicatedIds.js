export default function (html) {
    const fragment = htmlToFragment(html);
    const nodes = fragment.querySelectorAll("[id]:not([id=''])");
    const ids = [];
    nodes.forEach(node => {
        const id = node.getAttribute("id");
        ids.includes(id) ? node.removeAttribute("id") : ids.push(id);
    });
    return fragmentToHtml(fragment);
}

/** These special tags do not work well with Range.createContextualFragment */
const docTags = ["html", "body", "head", "table"];
const docTagsRegex = prefix => `${prefix}(` + docTags.reduce((res, tag) => `${res ? res + "|" : ""}${tag}`, "") + ")";
const replaceDocTagsRegex = new RegExp(`<(/?)${docTagsRegex("")}`, "gi");
const replaceDocTags = html => html.replace(replaceDocTagsRegex, "<$1replaced_$2");
const restoreDocTagsRegex = new RegExp(`<(/?)${docTagsRegex("replaced_")}`, "gi");
const restoreDocTags = html => html.replace(restoreDocTagsRegex, "<$1$2");
const htmlToFragment = html => document.createRange().createContextualFragment(`<root>${replaceDocTags(html)}</root>`);
const fragmentToHtml = doc => restoreDocTags(doc.firstElementChild.innerHTML);
