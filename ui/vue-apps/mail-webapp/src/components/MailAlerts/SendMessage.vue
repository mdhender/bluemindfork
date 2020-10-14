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
        link() {
            return {
                name: "v:mail:message",
                params: { message: this.result, folder: this.alert.payload.sentFolder.path }
            };
        },
        subject() {
            if (this.messages[this.payload.draftKey]) {
                return this.messages[this.alert.payload.draftKey].subject;
            } else if (this.result) {
                return this.result.subject;
            }
            return "";
        }
    }
};
</script>
