<template>
    <div class="mail-message-content-body min-h-100">
        <iframe ref="iFrameMailContent" class="w-100 border-0" scrolling="no" />
    </div>
</template>

<script>
import { mailText2Html, MimeType } from "@bluemind/email";
import { sanitizeHtml } from "@bluemind/html-utils";

export default {
    name: "MailMessageContentBody",
    props: {
        parts: {
            type: Array,
            required: true
        }
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
            const offsetHeight = this.$refs.iFrameMailContent.contentWindow.document.body.offsetHeight;
            this.$refs.iFrameMailContent.style.height = offsetHeight + (offsetHeight*10/100) + 'px';

            const scrollHeight = this.$refs.iFrameMailContent.contentWindow.document.body.scrollHeight;
            const clientHeight = this.$refs.iFrameMailContent.contentWindow.document.body.clientHeight;
            
            if (scrollHeight > clientHeight) {
                this.$refs.iFrameMailContent.style.height = scrollHeight + (scrollHeight*10/100) + 'px';
            }
        },
        display() {
            if (this.parts) {
                let html = "";
                this.parts.forEach((part, index) => {
                    if (index != 0) {
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
                        const imgSrc = "data:" + part.mime + ";base64, " + part.content;
                        html += '<div align="center"><img src="' + imgSrc + '"></div>';
                    } else {
                        html += part.content;
                    }
                });
                const iframeDoc = this.$refs.iFrameMailContent.contentWindow.document;
                iframeDoc.open();
                iframeDoc.write(html);
                iframeDoc.close();

                this.addStyle(iframeDoc);
                
                this.$nextTick(function() {
                    this.resizeIFrame();
                });
            }
        },

        /** Add style to the iframe for 'reply' and 'forward' rendering. */
        addStyle(iframeDoc) {
            const css = ` .reply {
                            margin-left: 1rem;
                            padding-left: 1rem;
                            border-left: 2px solid black;
                        }
                        .forwarded {
                            margin-left: 1rem;
                            padding-left: 1rem;
                            color: purple;
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
