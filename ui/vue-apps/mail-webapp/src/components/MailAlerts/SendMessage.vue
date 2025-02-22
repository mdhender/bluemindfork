<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ subject }" />
    <default-alert v-else-if="alert.type === AlertTypes.ERROR" :alert="alert" :options="{ subject, reason: error }" />
    <i18n v-else-if="alert.type === AlertTypes.SUCCESS" :path="path" tag="span">
        <template #subject>
            <router-link :to="link">{{ subject }}</router-link>
        </template>
    </i18n>
</template>
<script>
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import { conversationUtils } from "@bluemind/mail";
import { mapState } from "vuex";

const { createConversationStub } = conversationUtils;

export default {
    name: "SendMessage",
    components: { DefaultAlert },
    mixins: [AlertMixin],
    data() {
        return { subject: "" };
    },
    computed: {
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        ...mapState("mail", ["folders", "mailboxes"]),
        link() {
            if (this.alert.type === this.AlertTypes.SUCCESS) {
                const folder = this.folders[this.result.folderRef.key];
                const conversation = createConversationStub(this.result.remoteRef.internalId, this.result.folderRef);
                return {
                    name: "v:mail:conversation",
                    params: {
                        conversation,
                        folder: folder?.path,
                        mailbox: this.mailboxes[folder?.mailboxRef.key]?.name
                    }
                };
            }
            return null;
        }
    },
    created() {
        this.subject = this.payload.subject
            ? this.payload.subject.trim()
            : this.result?.subject
            ? this.result.subject.trim()
            : this.$t("mail.viewer.no.subject");
    }
};
</script>
