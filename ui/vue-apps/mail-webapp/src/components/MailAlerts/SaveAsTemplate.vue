<template>
    <default-alert :alert="alert" :options="{ subject }" />
</template>
<script>
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import { mapState } from "vuex";

export default {
    name: "SaveAsTemplate",
    components: { DefaultAlert },
    mixins: [AlertMixin],
    computed: {
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        subject() {
            let subject = "";
            if (this.messages[this.payload.template.key]) {
                subject = this.messages[this.alert.payload.template.key].subject;
            } else if (this.result) {
                subject = this.result.subject;
            }
            return subject.trim() || this.$t("mail.viewer.no.subject");
        }
    }
};
</script>
