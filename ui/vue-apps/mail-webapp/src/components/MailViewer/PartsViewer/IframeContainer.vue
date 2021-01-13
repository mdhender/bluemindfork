<template>
    <iframe
        ref="iFrameMailContent"
        :title="$t('mail.content.body')"
        class="w-100 border-0"
        :srcdoc="iFrameContent"
        @load="resizeIFrame"
    />
</template>

<script>
import { mapMutations, mapState } from "vuex";

import { hasRemoteImages, blockRemoteImages, unblockRemoteImages } from "@bluemind/html-utils";

import apiAddressbooks from "../../../store/api/apiAddressbooks";
import { SET_BLOCK_REMOTE_IMAGES, SET_SHOW_REMOTE_IMAGES_ALERT } from "~mutations";
import brokenImageIcon from "../../../../assets/brokenImageIcon.png";

export default {
    name: "IframeContainer",
    props: {
        body: {
            type: String,
            required: true
        },
        styles: {
            type: String,
            required: false,
            default: ""
        }
    },
    data() {
        return { iFrameContent: "" };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { messageKey: "key" }),
        ...mapState("mail", { mustBlockRemoteImages: state => state.consultPanel.remoteImages.mustBeBlocked }),
        ...mapState("mail", ["messages"]),
        ...mapState("session", { settings: "userSettings" }),
        message() {
            return this.messages[this.messageKey];
        }
    },
    watch: {
        mustBlockRemoteImages(newValue, oldValue) {
            if (oldValue && !newValue) {
                const content = unblockRemoteImages(this.body);
                this.iFrameContent = this.buildHtml(content);
            }
        },
        "settings.trust_every_remote_content"(newValue) {
            if (newValue === "true") {
                const content = unblockRemoteImages(this.body);
                this.iFrameContent = this.buildHtml(content);
                this.SET_SHOW_REMOTE_IMAGES_ALERT(false);
            }
        }
    },
    async mounted() {
        let content = this.body;

        if (hasRemoteImages(content) && this.settings.trust_every_remote_content === "false") {
            // check if sender is known (found in any suscribed addressbook)
            const searchResult = await apiAddressbooks.search(this.message.from.address);
            const isSenderKnown = searchResult.total > 0;
            if (!isSenderKnown) {
                this.SET_SHOW_REMOTE_IMAGES_ALERT(true);
                this.SET_BLOCK_REMOTE_IMAGES(true);
                content = blockRemoteImages(content);
            }
        }

        this.iFrameContent = this.buildHtml(content);
    },
    methods: {
        ...mapMutations("mail", [SET_BLOCK_REMOTE_IMAGES, SET_SHOW_REMOTE_IMAGES_ALERT]),
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
        },
        buildHtml(content) {
            const label = this.$t("mail.application.region.messagecontent");
            const style = this.styles + BM_STYLE;
            return `<html>
                <head><base target="_blank"><style>${style}</style></head>
                <body><main aria-label="${label}">${content}</main></body>
            </html>`;
        }
    }
};

const BM_STYLE = `
        body {  
            font-family: 'Montserrat', sans-serif;     
            font-size: 0.75rem;
            font-weight: 400;
            color: #1f1f1f;
            margin: 0;
            word-break: break-word !important;
        }

        pre {
            font-family: 'Montserrat', sans-serif;
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
</script>
