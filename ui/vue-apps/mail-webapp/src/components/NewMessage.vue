<template>
    <bm-button
        v-bm-clipping="mobile ? 'hexagon' : undefined"
        variant="primary"
        class="new-message"
        :class="
            mobile
                ? 'd-lg-none position-absolute new-message-responsive-btn z-index-110'
                : 'text-nowrap d-lg-inline-block d-none'
        "
        @click="$router.navigate({ name: 'mail:message', params: { messagepath } })"
    >
        <bm-icon v-if="mobile" icon="plus" size="2x" />
        <bm-label-icon v-else icon="plus">{{ $t("mail.main.new") }}</bm-label-icon>
    </bm-button>
</template>
<script>
import { BmButton, BmClipping, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import { mapGetters } from "vuex";
import { MY_DRAFTS } from "~/getters";
import { draftPath } from "~/model/draft";

export default {
    name: "NewMessage",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon
    },
    directives: { BmClipping },
    props: {
        mobile: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
        messagepath() {
            return draftPath(this.MY_DRAFTS);
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
