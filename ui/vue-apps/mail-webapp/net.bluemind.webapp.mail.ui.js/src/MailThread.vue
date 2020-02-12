<template>
    <bm-col
        cols="12"
        md="8"
        lg="7"
        xl="7"
        class="px-0 h-100 flex-column overflow-auto"
        :class="'d-flex'"
    >
        <div class="mail-thread h-100 overflow-auto d-flex flex-column">
            <mail-message-new
                v-if="showComposer"
                :message="preparedAnswer"
                :previous-message="previousMessage"
                :mode="mode"
                :user-pref-text-only="userPrefTextOnly"
                @close="mode = 'default'"
            />
            <mail-message-content v-if="message" />
            <div />
        </div>
    </bm-col>
</template>

<script>
import { BmCol } from "@bluemind/styleguide";
import { mapGetters } from "vuex";
import { MimeType } from "@bluemind/email";
import { computeSubject, previousMessageContent } from "./MessageBuilder";
import MailMessageContent from "./MailMessageContent/MailMessageContent";
import MailMessageNew from "./MailMessageNew";
import MailMessageNewModes from "./MailMessageNew/MailMessageNewModes";

export default {
    name: "MailThread",
    components: {
        MailMessageContent,
        MailMessageNew,
        BmCol
    },
    data() {
        return {
            userPrefTextOnly: false, // TODO: initialize this with user setting
        };
    },
    computed: {
        ...mapGetters("mail-webapp", {
            message: "currentMessage",
            inlineParts: "currentMessageContent"
        }),
        previousMessage() {
            return {
                content: previousMessageContent(
                    this.pathSuffix(), 
                    this.inlineParts, 
                    this.message, 
                    this.userPrefTextOnly ? MimeType.TEXT_PLAIN : MimeType.TEXT_HTML
                ),
                messageId: this.message.messageId,
                references: this.message.references,
                messageKey: this.message.key,
                action: this.pathSuffix()
            };
        },
        preparedAnswer() {
            const action = this.pathSuffix();
            return {
                to: this.message.computeRecipients(this.message.recipientFields.TO, action),
                cc: this.message.computeRecipients(this.message.recipientFields.CC, action),
                subject: computeSubject(action, this.message)
            };
        },
        mode() {
            if (this.showComposer) {
                if (this.isReplyAll() && this.preparedAnswer.cc && this.preparedAnswer.cc.length > 0) {
                    return MailMessageNewModes.TO | MailMessageNewModes.CC;
                }
                return MailMessageNewModes.TO;
            }
            return MailMessageNewModes.NONE;
        },
        showComposer() {
            const action = this.pathSuffix();
            return (
                this.message &&
                (action === this.message.actions.REPLY ||
                    action === this.message.actions.REPLYALL ||
                    action === this.message.actions.FORWARD)
            );
        }
    },
    methods: {
        pathSuffix() {
            let indexOfLastSlash = this.$store.state.route.path.lastIndexOf("/");
            return this.$store.state.route.path.substring(indexOfLastSlash + 1);
        },
        isReplyAll() {
            return this.pathSuffix() === this.message.actions.REPLYALL;
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-thread .mail-message-new ~ .mail-message-content {
    @media (max-width: map-get($grid-breakpoints, 'lg')) {
        display: none !important;
    }
}
</style>