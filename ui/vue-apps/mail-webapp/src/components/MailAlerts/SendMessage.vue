<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ subject }" />
    <default-alert v-else-if="alert.type === AlertTypes.ERROR" :alert="alert" :options="{ subject }" />
    <i18n v-else-if="alert.type === AlertTypes.SUCCESS" :path="path" tag="span">
        <template #subject>
            <router-link :to="link">{{ subject }}</router-link>
        </template>
    </i18n>
</template>
<script>
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import { mapState } from "vuex";

export default {
    name: "SendMessage",
    components: { DefaultAlert },
    mixins: [AlertMixin],
    computed: {
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        ...mapState("mail", ["folders", "mailboxes"]),
        link() {
            const folder = this.folders[this.result.folderRef.key];
            return {
                name: "v:mail:message",
                params: {
                    message: this.result,
                    folder: folder?.path,
                    mailbox: this.mailboxes[folder?.mailboxRef.key]?.name
                }
            };
        },
        subject() {
            let subject = "";
            if (this.messages[this.payload.draftKey]) {
                subject = this.messages[this.alert.payload.draftKey].subject;
            } else if (this.result) {
                subject = this.result.subject;
            }
            return subject.trim() || this.$t("mail.viewer.no.subject");
        }
    }
};
</script>
