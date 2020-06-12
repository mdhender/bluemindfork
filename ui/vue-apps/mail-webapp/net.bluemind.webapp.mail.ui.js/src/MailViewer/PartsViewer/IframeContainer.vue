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
import { hasRemoteImages, blockRemoteImages, unblockRemoteImages } from "@bluemind/html-utils";
import { mapGetters, mapMutations, mapState } from "vuex";
import brokenImageIcon from "../../../assets/brokenImageIcon.png";

export default {
    name: "IframeContainer",
    props: {
        value: {
            type: String,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail-webapp", ["areRemoteImagesUnblocked"]),
        ...mapState("mail-webapp/currentMessage", { messageKey: "key" }),
        iFrameContent() {
            let content = this.value;

            if (this.areRemoteImagesUnblocked(this.messageKey)) {
                content = unblockRemoteImages(content);
            } else if (hasRemoteImages(content)) {
                content = blockRemoteImages(content);
                this.setShowBlockedImagesAlert(true);
            } else {
                this.setShowBlockedImagesAlert(false);
            }

            return buildHtml(content);
        }
    },
    methods: {
        ...mapMutations("mail-webapp", ["setShowBlockedImagesAlert"]),
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

function buildHtml(content) {
    const style = `
        body {
            font-family: 'Montserrat', sans-serif;
            font-size: 0.75rem;
            font-weight: 400;
            color: #2f2f2f;
            margin: 0;
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

    // FIXME: as we add our own <head> tag, we should parse existing <head> in content variable
    return `<html>
                <head><base target="_blank"><style>${style}</style></head>
                <body>${content}</body>
            </html>`;
}
</script>
