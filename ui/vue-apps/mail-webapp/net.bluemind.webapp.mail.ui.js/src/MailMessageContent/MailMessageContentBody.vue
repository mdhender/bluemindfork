<template>
    <div class="mail-message-content-body h-100 py-2">
        <iframe
            ref="iFrameMailContent"
            :title="$t('mail.content.body')"
            class="w-100 border-0"
            :srcdoc="iFrameContent"
            @load="resizeIFrame"
        />
    </div>
</template>

<script>
import { mailText2Html, MimeType } from "@bluemind/email";
import { mapGetters, mapMutations, mapState } from "vuex";
import { sanitizeHtml, hasRemoteImages, blockRemoteImages, unblockRemoteImages } from "@bluemind/html-utils";
import brokenImageIcon from "../../assets/brokenImageIcon.png";

export default {
    name: "MailMessageContentBody",
    computed: {
        ...mapGetters("mail-webapp/currentMessage", { parts: "content" }),
        ...mapState("mail-webapp/currentMessage", { messageKey: "key" }),
        ...mapGetters("mail-webapp", ["areRemoteImagesUnblocked"]),
        iFrameContent() {
            let bodyContent = this.bodyContentFromParts;

            if (this.areRemoteImagesUnblocked(this.messageKey)) {
                bodyContent = unblockRemoteImages(bodyContent);
            } else if (hasRemoteImages(bodyContent)) {
                bodyContent = blockRemoteImages(bodyContent);
                this.setShowBlockedImagesAlert(true);
            } else {
                this.setShowBlockedImagesAlert(false);
            }

            return this.buildHtml(bodyContent);
        },
        bodyContentFromParts() {
            return this.parts ? this.buildBodyContentFromParts() : "";
        }
    },
    methods: {
        ...mapMutations("mail-webapp", ["setShowBlockedImagesAlert"]),
        buildBodyContentFromParts() {
            let content = "";
            this.parts.forEach((part, index) => {
                if (index !== 0) {
                    content += this.buildSeparator();
                }
                if (MimeType.isHtml(part)) {
                    content += sanitizeHtml(part.content);
                } else if (MimeType.isText(part)) {
                    content += mailText2Html(part.content);
                } else if (MimeType.isImage(part)) {
                    const imgSrc = URL.createObjectURL(part.content);
                    content += this.buildImage(imgSrc);
                }
            });
            return content;
        },
        buildHtml(bodyContent) {
            const style = `
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
                        }

                        img.blocked-image {
                            position: relative;
                            min-height: 50px;
                            min-width: 55px;
                            display: inline-block;
                            border: 1px solid black;
                            border: solid 1px #727272 !important;
                            vertical-align: top;
                        }

                        img.blocked-image:before { 
                            content: attr(alt);
                            color: #2F2F2F;
                            display: block;
                            position: absolute;  
                            width: 100%;
                            height: 100%;
                            background: #fff;
                            background-image: url(${brokenImageIcon});
                            background-repeat: no-repeat;
                            background-position: 7px 7px;
                            padding: 9px 7px 7px 27px;
                            box-sizing: border-box;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            text-align start;
                            white-space: nowrap;
                            font-family: Montserrat;
                            font-style: normal;
                            font-weight: normal;
                            font-size: 12px;
                        }

                        a img.blocked-image:before {
                            color: #00AAEB !important;
                            text-decoration-line: underline;  
                        }`;
            return `<html>
                <head><base target="_blank"><style>${style}</style></head>
                <body><div>${bodyContent}</div></body>
            </html>`;
        },
        buildSeparator() {
            return `<hr style='margin: 1rem 0;
                        border: 0;
                        border-top: 1px solid rgba(0, 0, 0, 0.3);
                        height: 0'
            >`;
        },
        buildImage(imgSrc) {
            return '<div align="center"><img src="' + imgSrc + '"></div>';
        },
        resizeIFrame() {
            let htmlRootNode = this.$refs.iFrameMailContent.contentDocument.documentElement;
            this.$refs.iFrameMailContent.style.height = this.computeIFrameHeight(htmlRootNode) + "px";
        },
        /** get max offset height between root, body and body children nodes */
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
        }
    }
};
</script>
