<template>
    <article
        v-if="message"
        class="mail-thread d-flex flex-column overflow-x-hidden"
        :aria-label="$t('mail.application.region.messagethread')"
    >
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <mail-composer v-if="isADraft" :message-key="currentMessageKey" />
        <mail-viewer v-else :message-key="currentMessageKey" />
        <div />
    </article>
    <article v-else class="mail-thread">
        <mail-viewer-loading />
    </article>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { CLEAR, INFO, REMOVE } from "@bluemind/alert.store";
import ItemUri from "@bluemind/item-uri";
import { BmAlertArea } from "@bluemind/styleguide";

import { RESET_ACTIVE_MESSAGE, SET_ACTIVE_FOLDER, SET_BLOCK_REMOTE_IMAGES, SET_MESSAGE_COMPOSING } from "~mutations";
import { MESSAGE_IS_LOADED, MY_DRAFTS } from "~getters";
import BlockedRemoteContent from "./Alerts/BlockedRemoteContent";
import VideoConferencing from "./Alerts/VideoConferencing";
import MailComposer from "../MailComposer";
import MailViewer from "../MailViewer";
import MailViewerLoading from "../MailViewer/MailViewerLoading";
import { WaitForMixin } from "~mixins";

export default {
    name: "MailThread",
    components: {
        BlockedRemoteContent,
        BmAlertArea,
        MailComposer,
        MailViewer,
        MailViewerLoading,
        VideoConferencing
    },
    mixins: [WaitForMixin],
    provide: {
        area: "mail-thread"
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["folders", "messages"]),
        ...mapGetters("mail", { MESSAGE_IS_LOADED, MY_DRAFTS }),

        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "mail-thread") }),
        message() {
            return this.MESSAGE_IS_LOADED(this.currentMessageKey) && this.messages[this.currentMessageKey];
        },
        folder() {
            return this.message && this.folders[this.message.folderRef.key];
        },
        isADraft() {
            return this.currentMessageKey && this.messages[this.currentMessageKey]
                ? this.messages[this.currentMessageKey].composing
                : false;
        },
        readOnlyAlert() {
            return {
                alert: { name: "mail.READ_ONLY_FOLDER", uid: "READ_ONLY_FOLDER" },
                options: { area: "mail-thread", renderer: "DefaultAlert" }
            };
        }
    },
    watch: {
        "folder.key"() {
            if (this.folder && !this.folder.writable) {
                this.INFO(this.readOnlyAlert);
            } else {
                this.REMOVE(this.readOnlyAlert.alert);
            }
        },
        async currentMessageKey(value) {
            this.SET_BLOCK_REMOTE_IMAGES(false);
            try {
                await this.$store.dispatch("mail-webapp/$_getIfNotPresent", [value]);
                const message = this.messages[value];
                const folderKey = message.folderRef.key;
                if (!message.composing) {
                    this.SET_ACTIVE_FOLDER(this.folders[folderKey]);
                    if (this.MY_DRAFTS && folderKey === this.MY_DRAFTS.key) {
                        this.SET_MESSAGE_COMPOSING({ messageKey: value, composing: true });
                    }
                }
            } catch {
                this.$router.push({ name: "mail:home" });
            }
        },

        "$route.params.message": {
            async handler(value) {
                this.RESET_ACTIVE_MESSAGE();
                if (value) {
                    // FIXME: This is bad bad bad... naughty boy...
                    // Remove this once you have a solution for getifNotPresent....
                    // P.S : Bad because based on message.key can be decoded to folderKey AND folder.uid === folder.key
                    const folderKey = ItemUri.container(value);
                    await this.$waitFor(() => this.folders[folderKey] || this.MAILBOX_ARE_LOADED);
                    this.$store.commit("mail-webapp/currentMessage/update", { key: value });
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", {
            RESET_ACTIVE_MESSAGE,
            SET_ACTIVE_FOLDER,
            SET_BLOCK_REMOTE_IMAGES,
            SET_MESSAGE_COMPOSING
        }),
        ...mapActions("alert", { REMOVE, CLEAR, INFO })
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-thread {
    min-height: 100%;

    .mail-composer ~ .mail-viewer {
        @media (max-width: map-get($grid-breakpoints, "lg")) {
            display: none !important;
        }
    }

    .mail-composer {
        @media (min-width: map-get($grid-breakpoints, "lg")) {
            height: auto !important;
        }
    }
}

.overflow-x-hidden {
    overflow-x: hidden;
}
</style>
