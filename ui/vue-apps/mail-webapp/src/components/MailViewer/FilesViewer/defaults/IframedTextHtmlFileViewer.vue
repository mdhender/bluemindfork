<script>
import { mapGetters } from "vuex";
import { html2text } from "@bluemind/html-utils";
import { darkifyHtml, darkifyingBaseLvalue, getDarkifiedCss, undarkifyHtml } from "@bluemind/ui-components";
import IFrame from "../../../IFrame";
import FileViewerMixin from "../FileViewerMixin";
import TextHtmlFileViewer from "./../TextHtmlFileViewer";

export default {
    name: "IframedTextHtmlFileViewer",
    mixins: [FileViewerMixin],
    $capabilities: ["text/html"],
    props: { collapse: { type: Boolean, default: true } },
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
        undarkifyContent(event) {
            const contentAsFragment = event.selection.getRangeAt(0).cloneContents();
            undarkifyHtml(contentAsFragment);
            const div = document.createElement("div");
            div.appendChild(contentAsFragment.cloneNode(true));
            event.clipboardData.setData("text/html", div.innerHTML);
            event.clipboardData.setData("text/plain", html2text(div.innerHTML));
        },
        renderIFrame(h, { htmlStr, cssStr }) {
            return h(
                IFrame,
                {
                    staticClass: "border-0",
                    on: {
                        copy: event => {
                            this.undarkifyContent(event);
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
