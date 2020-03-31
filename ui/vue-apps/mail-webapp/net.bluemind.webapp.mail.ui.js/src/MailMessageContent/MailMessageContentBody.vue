<template>
    <div class="mail-message-content-body h-100 py-2">
        <iframe ref="iFrameMailContent" :title="$t('mail.content.body')" class="w-100 border-0" @load="resizeIFrame" />
    </div>
</template>

<script>
import { mailText2Html, MimeType } from "@bluemind/email";
import { mapGetters } from "vuex";
import { sanitizeHtml } from "@bluemind/html-utils";

export default {
    name: "MailMessageContentBody",
    computed: {
        ...mapGetters("mail-webapp/currentMessage", { parts: "content" })
    },
    watch: {
        parts: {
            handler: function() {
                this.$nextTick(function() {
                    this.display();
                });
            },
            immediate: true
        }
    },
    methods: {
        resizeIFrame() {
            let htmlRootNode = this.$refs.iFrameMailContent.contentDocument.documentElement;
            this.$refs.iFrameMailContent.style.height = this.computeIFrameHeight(htmlRootNode) + "px";
        },
        // get max offset height between root, body and body children nodes
        computeIFrameHeight(htmlRootNode) {
            let maxHeight = htmlRootNode.offsetHeight;
            const bodyNode = htmlRootNode.childNodes[1];
            if (bodyNode) {
                if (bodyNode.offsetHeight) {
                    maxHeight = Math.max(maxHeight, bodyNode.offsetHeight);
                }
                bodyNode.childNodes.forEach(bodyChild => {
                    if (bodyChild.offsetHeight) {
                        maxHeight = Math.max(maxHeight, bodyChild.offsetHeight);
                    }
                });
            }
            return maxHeight + 11;
        },
        display() {
            if (this.parts) {
                let html = "";
                this.parts.forEach((part, index) => {
                    if (index !== 0) {
                        html += `<hr style='margin: 1rem 0;
                                            border: 0;
                                            border-top: 1px solid rgba(0, 0, 0, 0.3);
                                            height: 0'
                                >`;
                    }
                    if (MimeType.isHtml(part)) {
                        html += sanitizeHtml(part.content);
                    } else if (MimeType.isText(part)) {
                        html += mailText2Html(part.content);
                    } else if (MimeType.isImage(part)) {
                        const imgSrc = URL.createObjectURL(part.content);
                        html += '<div align="center"><img src="' + imgSrc + '"></div>';
                    }
                });
                const iframeDoc = this.$refs.iFrameMailContent.contentWindow.document;
                iframeDoc.open();
                // all links should be opened in an other tab by default
                iframeDoc.write('<head><base target="_blank"></head>');
                iframeDoc.write("<body><div>" + html + "</div></body>");

                this.addStyle(iframeDoc);
                iframeDoc.close();
                this.resizeIFrame();
            }
        },

        /** Apply specific styles to the given iframe. */
        addStyle(iframeDoc) {
            // add style for 'reply' and 'forward' rendering
            // add style to enable <pre> content to wrap in order to see all of it
            const css = `
                        body {
                            font-family: 'Montserrat', sans-serif;
                            font-size: 0.75rem;
                            font-weight: 400;
                            color: #2f2f2f;
                            margin: 0;
                        }
                        .reply {
                            margin-left: 1rem;
                            padding-left: 1rem;
                            border-left: 2px solid black;
                        }
                        .forwarded {
                            margin-left: 1rem;
                            padding-left: 1rem;
                            color: purple;
                        }
                        pre {
                            white-space: pre-line;
                        }`;
            const head = iframeDoc.head || iframeDoc.getElementsByTagName("head")[0];
            let style = iframeDoc.getElementsByTagName("style")[0];
            if (!style) {
                style = iframeDoc.createElement("style");
                head.appendChild(style);
                style.type = "text/css";
            }

            if (style.styleSheet) {
                // this is required for IE8 and below.
                if (!style.styleSheet.cssText) {
                    style.styleSheet.cssText = "";
                }
                style.styleSheet.cssText += css;
            } else {
                style.appendChild(iframeDoc.createTextNode(css));
            }
        }
    }
};
</script>
