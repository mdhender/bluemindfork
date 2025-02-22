<template>
    <div class="mail-message d-flex flex-column">
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template #default="context"><component :is="context.alert.renderer" :alert="context.alert" /></template>
        </bm-alert-area>
        <template v-if="ACTIVE_MESSAGE.composing">
            <mail-composer v-if="DEFAULT_IDENTITY" :message="ACTIVE_MESSAGE" />
            <mail-composer-loading v-else />
        </template>
        <mail-viewer v-else :message="ACTIVE_MESSAGE" />
        <preview-modal />
    </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { INFO, REMOVE } from "@bluemind/alert.store";
import { BmAlertArea } from "@bluemind/ui-components";

import { SET_ACTIVE_FOLDER, SET_BLOCK_REMOTE_IMAGES, SET_MESSAGE_COMPOSING } from "~/mutations";
import { ACTIVE_MESSAGE, IS_SEARCH_ENABLED, MY_DRAFTS } from "~/getters";
import MailComposer from "../MailComposer";
import MailComposerLoading from "../MailComposer/MailComposerLoading";
import MailViewer from "../MailViewer";
import MailViewerLoading from "../MailViewer/MailViewerLoading";
import PreviewModal from "../MailAttachment/PreviewModal";

export default {
    name: "MailMessage",
    components: {
        BmAlertArea,
        MailComposer,
        MailComposerLoading,
        MailViewer,
        MailViewerLoading,
        PreviewModal
    },
    provide() {
        return { $messageViewerRoot: this };
    },

    computed: {
        ...mapState("mail", { folders: "folders", messages: state => state.conversations.messages }),
        ...mapGetters("root-app", ["DEFAULT_IDENTITY"]),
        ...mapGetters("mail", { ACTIVE_MESSAGE, IS_SEARCH_ENABLED, MY_DRAFTS }),
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "right-panel") }),
        folder() {
            return this.ACTIVE_MESSAGE && this.folders[this.ACTIVE_MESSAGE.folderRef.key];
        },
        readOnlyAlert() {
            return {
                alert: { name: "mail.READ_ONLY_FOLDER", uid: "READ_ONLY_FOLDER" },
                options: { area: "right-panel", renderer: "DefaultAlert" }
            };
        }
    },
    watch: {
        "folder.key": {
            handler() {
                if (this.folder && !this.folder.writable) {
                    this.INFO(this.readOnlyAlert);
                } else {
                    this.REMOVE(this.readOnlyAlert.alert);
                }
            },
            immediate: true
        },
        "ACTIVE_MESSAGE.key": {
            handler(value, oldValue) {
                try {
                    if (oldValue) {
                        const oldMessage = this.messages[oldValue];
                        if (oldMessage?.composing && oldMessage?.folderRef.key !== this.MY_DRAFTS.key) {
                            this.SET_MESSAGE_COMPOSING({ messageKey: oldValue, composing: false });
                        }
                    }
                    if (this.ACTIVE_MESSAGE && !this.ACTIVE_MESSAGE.composing) {
                        const folderKey = this.ACTIVE_MESSAGE.folderRef.key;
                        if (this.MY_DRAFTS && folderKey === this.MY_DRAFTS.key) {
                            this.SET_MESSAGE_COMPOSING({ messageKey: this.ACTIVE_MESSAGE.key, composing: true });
                        }
                        if (this.IS_SEARCH_ENABLED) {
                            this.SET_ACTIVE_FOLDER(this.folders[folderKey]);
                        }
                    }
                } catch (e) {
                    this.$router.push({ name: "mail:home" });
                    throw e;
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", { SET_ACTIVE_FOLDER, SET_BLOCK_REMOTE_IMAGES, SET_MESSAGE_COMPOSING }),
        ...mapActions("alert", { REMOVE, INFO })
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-message {
    .mail-composer ~ .mail-viewer {
        display: none !important;
        @include from-lg {
            display: flex !important;
        }
    }

    .mail-composer {
        @include from-lg {
            height: auto !important;
        }
    }

    min-width: 0;
}
</style>
