<script>
import { mapGetters } from "vuex";
import { html2text } from "@bluemind/html-utils";
import { darkifyHtml, darkifyingBaseLvalue, getDarkifiedCss, undarkifyHtml } from "@bluemind/ui-components";
import { WEBSERVER_HANDLER_BASE_URL } from "@bluemind/email";
import { convertBlobToBase64 } from "@bluemind/blob";
import IFrame from "../../../IFrame";
import FileViewerMixin from "../FileViewerMixin";
import TextHtmlFileViewer from "./../TextHtmlFileViewer";

const copyProcessors = [undarkifyHtml, removeImageWithPartSrc];
const copyAsyncProcessors = [undarkifyHtml, replaceImageWithPartSrc];

const getAllImagesWithPartSrc = contentAsFragment =>
    [...contentAsFragment.querySelectorAll("img[src]")].filter(el =>
        el.attributes.src.nodeValue.startsWith(WEBSERVER_HANDLER_BASE_URL)
    );

function removeImageWithPartSrc(contentAsFragment) {
    return getAllImagesWithPartSrc(contentAsFragment).forEach(el => el.parentNode.removeChild(el));
}

function replaceImageWithPartSrc(contentAsFragment) {
    return Promise.all(
        getAllImagesWithPartSrc(contentAsFragment).map(async el => {
            const data = await fetch(el.attributes.src.nodeValue);
            const blob = await data.blob();
            el.src = await convertBlobToBase64(blob);
        })
    );
}

function getFragmentHtml(fragment) {
    const div = document.createElement("div");
    div.appendChild(fragment.cloneNode(true));
    return div.innerHTML;
}

function copyHtmlToClipboard(htmlContent, event) {
    event.clipboardData.setData("text/html", htmlContent);
    event.clipboardData.setData("text/plain", html2text(htmlContent));
}

function copyHtmlToClipboardAsync(htmlContent) {
    try {
        navigator.clipboard.write([
            new ClipboardItem({
                "text/html": new Blob([htmlContent], { type: "text/html" }),
                "text/plain": new Blob([html2text(htmlContent)], { type: "text/plain" })
            })
        ]);
    } catch {
        // Async copy-paste is not supported by the browser, do nothing
    }
}

export default {
    name: "IframedTextHtmlFileViewer",
    mixins: [FileViewerMixin],
    $capabilities: ["text/html"],
    props: {
        collapse: {
            type: Boolean,
            default: true
        },
        relatedParts: {
            type: Array,
            required: true
        }
    },
    data() {
        return { IFRAME_STYLE };
    },
    computed: {
        ...mapGetters("settings", ["IS_COMPUTED_THEME_DARK"])
    },
    methods: {
        getDarkified({ htmlStr, cssStr }) {
            const customProperties = new Map();
            const htmlDoc = new DOMParser().parseFromString(htmlStr, "text/html");
            darkifyHtml(htmlDoc, darkifyingBaseLvalue(), customProperties);
            htmlStr = htmlDoc.documentElement.querySelector("body").innerHTML;
            cssStr = getDarkifiedCss(cssStr, darkifyingBaseLvalue(), customProperties);
            cssStr += "\nbody {\n";
            for (const [key, value] of customProperties) {
                cssStr += `    ${key}: ${value};\n`;
            }
            cssStr += "}\n";
            return { htmlStr, cssStr };
        },
        async formatAndCopyContent(event) {
            let contentAsFragment = event.selection.getRangeAt(0).cloneContents();
            copyProcessors.forEach(processor => processor(contentAsFragment));
            copyHtmlToClipboard(getFragmentHtml(contentAsFragment), event);

            contentAsFragment = event.selection.getRangeAt(0).cloneContents();
            for (const copyAsyncProcessor of copyAsyncProcessors) {
                await copyAsyncProcessor(contentAsFragment);
            }
            copyHtmlToClipboardAsync(getFragmentHtml(contentAsFragment));
        },
        renderIFrame(h, { htmlStr, cssStr }) {
            return h(
                IFrame,
                {
                    staticClass: "border-0",
                    on: {
                        copy: event => {
                            this.formatAndCopyContent(event);
                            event.preventDefault();
                        }
                    }
                },
                [
                    h("template", { slot: "head" }, [
                        h("base", { attrs: { target: "_blank" } }),
                        h("link", {
                            attrs: { type: "text/css", rel: "stylesheet", href: "css/montserrat/index.css" }
                        })
                    ]),
                    h("template", { slot: "style" }, cssStr),
                    h("main", { domProps: { innerHTML: htmlStr } })
                ]
            );
        },
        renderTextHtmlFileViewerSlot(h, { html, styles }) {
            let content = { htmlStr: html, cssStr: styles };
            if (this.IS_COMPUTED_THEME_DARK) {
                content = this.getDarkified(content);
            }
            content.cssStr += IFRAME_STYLE;
            return this.renderIFrame(h, content);
        }
    },
    render(h) {
        return h(TextHtmlFileViewer, {
            props: this.$props,
            scopedSlots: {
                default: props => this.renderTextHtmlFileViewerSlot(h, props)
            }
        });
    }
};
const IFRAME_STYLE = `
		html {
            height: auto !important;
        }

        body {
            font-family: "Montserrat", sans-serif;
            font-size: 0.875rem;
            font-weight: 400;
            color: var(--neutral-fg-hi1);
            margin: 0;
            overflow-wrap: break-word !important;
            height: auto !important;
        }

        main * {
            max-width: 100%;
        }
        pre {
            font-family: monospace;
            white-space: pre-wrap;
        }
        `;
</script>
<style lang="scss">
.text-html-file-viewer {
    .i-frame {
        min-width: 100%;
        max-width: 100%;
    }
}
</style>
