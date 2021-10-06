<template>
    <div>
        <iframe
            ref="iFrameMailContent"
            :title="$t('mail.content.body')"
            class="w-100 border-0"
            :srcdoc="iFrameContent"
            @load="
                resizeIFrame();
                addClickListener();
            "
        />
        <div v-show="!scrollbarHeight" ref="scrollbarMeasure" class="scrollbar-measure" />
    </div>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";

import { WARNING, REMOVE } from "@bluemind/alert.store";
import { hasRemoteImages, blockRemoteImages, unblockRemoteImages } from "@bluemind/html-utils";

import apiAddressbooks from "~/store/api/apiAddressbooks";
import { SET_BLOCK_REMOTE_IMAGES } from "~/mutations";
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
        },
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { iFrameContent: "", scrollbarHeight: null };
    },
    inject: ["$messageViewerRoot"],
    computed: {
        ...mapState("mail", { mustBlockRemoteImages: state => state.consultPanel.remoteImages.mustBeBlocked }),
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        blockedContentAlert() {
            return {
                alert: { name: "mail.BLOCK_REMOTE_CONTENT", uid: "BLOCK_REMOTE_CONTENT", payload: this.message },
                options: { area: "right-panel", renderer: "BlockedRemoteContent" }
            };
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
                this.REMOVE(this.blockedContentAlert.alert);
            }
        },
        body() {
            this.init();
        }
    },
    destroyed() {
        this.REMOVE(this.blockedContentAlert.alert);
    },
    mounted() {
        this.init();
    },
    methods: {
        ...mapMutations("mail", [SET_BLOCK_REMOTE_IMAGES]),
        ...mapActions("alert", { WARNING, REMOVE }),
        async init() {
            let content = this.body;

            if (hasRemoteImages(content) && this.settings.trust_every_remote_content === "false") {
                // check if sender is known (found in any suscribed addressbook)
                const searchResult = await apiAddressbooks.search(this.message.from.address);
                const isSenderKnown = searchResult.total > 0;
                if (!isSenderKnown) {
                    this.WARNING(this.blockedContentAlert);
                    this.SET_BLOCK_REMOTE_IMAGES(true);
                    content = blockRemoteImages(content);
                }
            }

            this.scrollbarHeight = this.srollbarHeight();
            this.iFrameContent = this.buildHtml(content);
        },
        resizeIFrame() {
            const resizeObserver = new ResizeObserver(entries => {
                if (this.$refs.iFrameMailContent) {
                    entries.forEach(() => {
                        let htmlRootNode = this.$refs.iFrameMailContent.contentDocument.documentElement;
                        let currentHeight = this.$refs.iFrameMailContent.style.height?.replace("px", "");
                        this.$refs.iFrameMailContent.style.height =
                            this.computeIFrameHeight(htmlRootNode, currentHeight) + "px";
                    });
                    this.$messageViewerRoot.$emit("resized");
                }
            });
            resizeObserver.observe(this.$refs.iFrameMailContent);
        },
        /** get max offset height between root, body and body children nodes */
        computeIFrameHeight(htmlRootNode, currentHeight) {
            let maxHeight = htmlRootNode.offsetHeight;
            const bodyNode = Array.from(htmlRootNode.childNodes).find(
                node => node.tagName && node.tagName.toLowerCase() === "body"
            );
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
            return currentHeight >= maxHeight ? currentHeight : maxHeight + this.scrollbarHeight;
        },
        buildHtml(content) {
            const label = this.$t("mail.application.region.messagecontent");
            const style = this.styles + BM_STYLE;
            return `<html>
                <head><base target="_blank"><style>${style}</style></head>
                <body><main aria-label="${label}">${content}</main></body>
            </html>`;
        },
        srollbarHeight() {
            return this.$refs.scrollbarMeasure.offsetHeight - this.$refs.scrollbarMeasure.clientHeight;
        },
        addClickListener() {
            const iFrame = this.$refs.iFrameMailContent;
            const doc = iFrame.contentDocument?.document || iFrame.contentWindow?.document;
            if (doc) {
                const el = this.$el;
                doc.body.addEventListener("click", function () {
                    el.click();
                });
            }
        }
    }
};

const BM_STYLE = `
        @import url('/webapp/css/montserrat/index.css');

        body {
            font-family: "Montserrat", sans-serif;
            font-size: 0.75rem;
            font-weight: 400;
            color: #1f1f1f;
            margin: 0;
            overflow-wrap: break-word !important;
        }

        pre {
            font-family: "Montserrat", sans-serif;
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
        }

        blockquote {
            width: unset !important;
            margin-inline-end: unset !important;
        }`;
</script>

<style lang="scss">
.scrollbar-measure {
    width: 100px;
    height: 100px;
    overflow: scroll;
    position: absolute;
    top: -9999px;
}
</style>
