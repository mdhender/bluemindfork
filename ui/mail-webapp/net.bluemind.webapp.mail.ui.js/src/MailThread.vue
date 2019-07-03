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
        <mail-message-content :message="message" :parts="parts" />
        <div />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { html2text } from "@bluemind/html-utils";
import MailMessageContent from "./MailMessageContent";
import MailMessageNew from "./MailMessageNew";

export default {
    name: "MailThread",
    components: {
        MailMessageContent,
        MailMessageNew
    },
    computed: {
        ...mapGetters("backend.mail/items", {
            message: "currentMessage",
            parts: "currentParts"
        }),
        subject() {
            let replySubject = this.$t("mail.compose.reply.subject");
            if (replySubject !== this.message.subject.substring(0, replySubject.length)) {
                return replySubject + this.message.subject;
            }
            return this.message.subject;
        },
        previousMessageContent() {
            let previousMessage = "";
            this.parts.forEach(part => {
                if (part.mime === "text/html") {
                    previousMessage += html2text.fromString(part.content);
                } else if (part.mime === "text/plain") {
                    previousMessage += part.content;
                }
            });
            previousMessage = previousMessage
                .split("\n")
                .map(line => "> " + line)
                .join("\n");
            previousMessage =
                this.$t("mail.compose.reply.body", { date: this.message.date, name: this.message.from.formattedName }) +
                "\n\n" +
                previousMessage;
            return previousMessage;
        },
        previousMessage() {
            return {
                content: this.previousMessageContent,
                messageId: this.message.messageId,
                references: this.message.references
            };
        },
        preparedAnswer() {
            const action = this.pathSuffix();
            return {
                to: this.message.computeRecipients(this.message.recipientFields.TO, action),
                cc: this.message.computeRecipients(this.message.recipientFields.CC, action),
                subject: this.subject
            };
        },
        mode() {
            if (this.showComposer) {
                if (this.isReplyAll() && this.preparedAnswer.cc && this.preparedAnswer.cc.length > 0) {
                    return "default";
                }
                return "reply";
            }
            return null;
        },
        showComposer() {
            const action = this.pathSuffix();
            return (
                action == this.message.actions.REPLY ||
                action == this.message.actions.REPLYALL ||
                action == this.message.actions.FORWARD
            );
        }
    },
    methods: {
        pathSuffix() {
            let indexOfLastSlash = this.$store.state.route.path.lastIndexOf("/");
            return this.$store.state.route.path.substring(indexOfLastSlash + 1);
        },
        isReplyAll() {
            return this.pathSuffix() == this.message.actions.REPLYALL;
        }
    }
};
</script>
