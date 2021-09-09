<template>
    <div class="mail-message d-flex flex-column">
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <template v-if="ACTIVE_MESSAGE.composing">
            <mail-composer v-if="DEFAULT_IDENTITY" :message="ACTIVE_MESSAGE" />
            <mail-composer-loading v-else />
        </template>
        <mail-viewer v-else :message="ACTIVE_MESSAGE" />
        <div />
    </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { INFO, REMOVE } from "@bluemind/alert.store";
import { BmAlertArea } from "@bluemind/styleguide";

import { SET_ACTIVE_FOLDER, SET_BLOCK_REMOTE_IMAGES, SET_MESSAGE_COMPOSING } from "~/mutations";
import { ACTIVE_MESSAGE, CONVERSATION_LIST_IS_SEARCH_MODE, MY_DRAFTS } from "~/getters";
import BlockedRemoteContent from "./Alerts/BlockedRemoteContent";
import VideoConferencing from "./Alerts/VideoConferencing";
import MailComposer from "../MailComposer";
import MailComposerLoading from "../MailComposer/MailComposerLoading";
import MailViewer from "../MailViewer";
import MailViewerLoading from "../MailViewer/MailViewerLoading";

export default {
    name: "MailMessage",
    components: {
        BlockedRemoteContent,
        BmAlertArea,
        MailComposer,
        MailComposerLoading,
        MailViewer,
        MailViewerLoading,
        VideoConferencing
    },
    provide: {
        area: "mail-message"
    },
    computed: {
        ...mapState("mail", ["folders"]),
        ...mapGetters("root-app", ["DEFAULT_IDENTITY"]),
        ...mapGetters("mail", { ACTIVE_MESSAGE, CONVERSATION_LIST_IS_SEARCH_MODE, MY_DRAFTS }),
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "mail-message") }),
        folder() {
            return this.ACTIVE_MESSAGE && this.folders[this.ACTIVE_MESSAGE.folderRef.key];
        },
        readOnlyAlert() {
            return {
                alert: { name: "mail.READ_ONLY_FOLDER", uid: "READ_ONLY_FOLDER" },
                options: { area: "mail-message", renderer: "DefaultAlert" }
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
        async "ACTIVE_MESSAGE.key"() {
            this.SET_BLOCK_REMOTE_IMAGES(false);
            try {
                if (this.ACTIVE_MESSAGE && !this.ACTIVE_MESSAGE.composing) {
                    const folderKey = this.ACTIVE_MESSAGE.folderRef.key;
                    if (this.MY_DRAFTS && folderKey === this.MY_DRAFTS.key) {
                        this.SET_MESSAGE_COMPOSING({ messageKey: this.ACTIVE_MESSAGE.key, composing: true });
                    }
                    if (this.CONVERSATION_LIST_IS_SEARCH_MODE) {
                        this.SET_ACTIVE_FOLDER(this.folders[folderKey]);
                    }
                }
            } catch (e) {
                this.$router.push({ name: "mail:home" });
                throw e;
            }
        }
    },
    methods: {
        ...mapMutations("mail", { SET_ACTIVE_FOLDER, SET_BLOCK_REMOTE_IMAGES, SET_MESSAGE_COMPOSING }),
        ...mapActions("alert", { REMOVE, INFO })
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-message {
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
</style>
