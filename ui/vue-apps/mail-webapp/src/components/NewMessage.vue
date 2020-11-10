<template>
    <bm-button
        v-bm-clip-path="mobile ? 'hexagon' : undefined"
        variant="primary"
        class="new-message"
        :class="
            mobile
                ? 'd-lg-none position-absolute new-message-responsive-btn z-index-110'
                : 'text-nowrap d-lg-inline-block d-none'
        "
        @click="composeNewMessage"
    >
        <bm-icon v-if="mobile" icon="plus" size="2x" />
        <bm-label-icon v-else icon="plus">{{ $t("mail.main.new") }}</bm-label-icon>
    </bm-button>
</template>
<script>
import { mapGetters, mapMutations, mapState } from "vuex";

import { BmButton, BmClipPath, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import { inject } from "@bluemind/inject";

import { MY_DRAFTS } from "~getters";
import { ADD_MESSAGES, SET_DRAFT_EDITOR_CONTENT } from "~mutations";
import { createEmpty } from "../model/draft";
import { addSignature } from "../model/signature";

export default {
    name: "NewMessage",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon
    },
    directives: { BmClipPath },
    props: {
        mobile: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
        ...mapState("mail", ["messageCompose"]),
        ...mapState("session", { settings: "userSettings" })
    },
    methods: {
        ...mapMutations("mail", [ADD_MESSAGES, SET_DRAFT_EDITOR_CONTENT]),
        async composeNewMessage() {
            const message = createEmpty(this.MY_DRAFTS, inject("UserSession"));
            this.ADD_MESSAGES([message]);
            let content = "";
            const userPrefTextOnly = false; // FIXME with user settings
            if (this.messageCompose.signature && this.settings.insert_signature === "true") {
                content = addSignature(content, userPrefTextOnly, this.messageCompose.signature);
            }
            this.SET_DRAFT_EDITOR_CONTENT(content);
            return this.$router.navigate({ name: "v:mail:message", params: { message: message.key } });
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.new-message {
    &.new-message-responsive-btn {
        bottom: $sp-2;
        right: $sp-2;
        height: 4em;
        width: 4em;
    }
}
</style>
