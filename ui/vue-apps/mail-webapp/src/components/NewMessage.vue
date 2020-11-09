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
import { mapActions, mapGetters } from "vuex";
import { BmButton, BmClipPath, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import { MessageCreationModes } from "../model/message";
import { MY_DRAFTS } from "~getters";
import { CREATE_MESSAGE } from "~actions";

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
        ...mapGetters("mail", { MY_DRAFTS })
    },
    methods: {
        ...mapActions("mail", { CREATE_MESSAGE }),
        async composeNewMessage() {
            const messageKey = await this.CREATE_MESSAGE({
                myDraftsFolder: this.MY_DRAFTS,
                creationMode: MessageCreationModes.NEW
            });
            return this.$router.navigate({ name: "v:mail:message", params: { message: messageKey } });
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
