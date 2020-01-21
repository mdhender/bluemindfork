<template>
    <div class="mail-thread h-100 overflow-auto">
        <mail-message-new
            v-if="showComposer"
            class="mb-5"
            :message="preparedAnswer"
            :previous-message="previousMessage"
            :mode="mode"
            @close="mode = 'default'"
        />
        <mail-message-content v-if="message" />
        <div />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { computeSubject, previousMessageContent } from "./MessageBuilder";
import MailMessageContent from "./MailMessageContent/MailMessageContent";
import MailMessageNew from "./MailMessageNew";
import MailMessageNewModes from "./MailMessageNew/MailMessageNewModes";

export default {
    name: "MailThread",
    components: {
        MailMessageContent,
        MailMessageNew
    },
    computed: {
        ...mapGetters("mail-webapp", {
            message: "currentMessage",
            inlineParts: "currentMessageContent"
        }),
        previousMessage() {
            return {
                content: previousMessageContent(this.pathSuffix(), this.inlineParts, this.message),
                messageId: this.message.messageId,
                references: this.message.references
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
