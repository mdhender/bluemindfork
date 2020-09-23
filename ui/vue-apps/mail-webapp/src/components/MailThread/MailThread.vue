<template>
    <article
        v-if="(currentMessageKey && isADraft) || (message && !isADraft)"
        class="mail-thread d-flex flex-column"
        :aria-label="$t('mail.application.region.messagethread')"
    >
        <mail-component-alert
            v-if="!areRemoteImagesUnblocked(currentMessageKey) && showBlockedImagesAlert"
            icon="exclamation-circle"
            @close="setShowBlockedImagesAlert(false)"
        >
            {{ $t("mail.content.alert.images.blocked") }}
            &nbsp;
            <a href="#" @click.prevent="showImages()">{{ $t("mail.content.alert.images.show") }}</a>
        </mail-component-alert>
        <mail-component-alert
            v-if="!folderOfCurrentMessage.writable && !isReadOnlyAlertDismissed"
            icon="info-circle-plain"
            @close="isReadOnlyAlertDismissed = true"
        >
            {{ $t("mail.content.alert.readonly") }}
        </mail-component-alert>
        <mail-composer v-if="isADraft" :message-key="currentMessageKey" />
        <mail-viewer v-else-if="message" />
        <div />
    </article>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
// import { computeSubject, previousMessageContent } from "../MessageBuilder";
// import { MimeType } from "@bluemind/email";
import { ItemUri } from "@bluemind/item-uri";
import MailComponentAlert from "../MailComponentAlert";
import MailComposer from "../MailComposer";
// import MailComposerModes from "../MailComposer/MailComposerModes";
import MailViewer from "../MailViewer";

export default {
    name: "MailThread",
    components: {
        MailComponentAlert,
        MailComposer,
        MailViewer
    },
    data() {
        return {
            isReadOnlyAlertDismissed: false
        };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail-webapp/currentMessage", { message: "message" }),
        ...mapState("mail-webapp", ["showBlockedImagesAlert"]),
        ...mapGetters("mail-webapp", ["areRemoteImagesUnblocked"]),
        ...mapState("mail", ["folders", "messages"]),
        folderOfCurrentMessage() {
            return this.folders[ItemUri.container(this.currentMessageKey)];
        },
        okok() {
            return this.messages[this.currentMessageKey];
        },
        isADraft() {
            return this.currentMessageKey && this.messages[this.currentMessageKey]
                ? this.messages[this.currentMessageKey].composing
                : false;
        }
        // previousMessage() {
        //     return {
        //         content: previousMessageContent(
        //             this.pathSuffix(),
        //             this.inlineParts,
        //             this.message,
        //             this.userPrefTextOnly ? MimeType.TEXT_PLAIN : MimeType.TEXT_HTML
        //         ),
        //         messageId: this.message.messageId,
        //         references: this.message.references,
        //         messageKey: this.currentMessageKey,
        //         action: this.pathSuffix()
        //     };
        // },
        // preparedAnswer() {
        //     const action = this.pathSuffix();
        //     return {
        //         to: this.message.computeRecipients(this.message.recipientFields.TO, action),
        //         cc: this.message.computeRecipients(this.message.recipientFields.CC, action),
        //         subject: computeSubject(action, this.message)
        //     };
        // },
        // mode() {
        //     if (this.showComposer) {
        //         if (this.isReplyAll() && this.preparedAnswer.cc && this.preparedAnswer.cc.length > 0) {
        //             return MailComposerModes.TO | MailComposerModes.CC;
        //         }
        //         return MailComposerModes.TO;
        //     }
        //     return MailComposerModes.NONE;
        // },
        // showComposer() {
        //     if (this.message) {
        //         const action = this.pathSuffix();
        //         return (
        //             action === this.message.actions.REPLY ||
        //             action === this.message.actions.REPLYALL ||
        //             action === this.message.actions.FORWARD
        //         );
        //     }
        //     return false;
        // }
    },
    watch: {
        message() {
            this.isReadOnlyAlertDismissed = false;
        }
    },
    methods: {
        ...mapMutations("mail-webapp", ["setShowBlockedImagesAlert", "unblockRemoteImages"]),
        // pathSuffix() {
        //     let indexOfLastSlash = this.$store.state.route.path.lastIndexOf("/");
        //     return this.$store.state.route.path.substring(indexOfLastSlash + 1);
        // },
        // isReplyAll() {
        //     return this.pathSuffix() === this.message.actions.REPLYALL;
        // },
        showImages() {
            this.unblockRemoteImages(this.currentMessageKey);
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-thread {
    min-height: 100%;

    .mail-component-alert {
        margin-bottom: $sp-1;
    }

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
