<template>
    <mail-open-in-popup-with-shift v-slot="action" :href="{ name: 'mail:popup:message', params: { messagepath } }">
        <bm-button
            v-bm-clipping="mobile ? 'hexagon' : undefined"
            variant="secondary"
            class="new-message"
            :class="
                mobile
                    ? 'd-lg-none position-absolute new-message-responsive-btn z-index-110'
                    : 'text-nowrap d-lg-inline-block d-none'
            "
            :title="action.label()"
            @click="action.execute(openComposer)"
        >
            <bm-icon v-if="mobile" icon="plus" size="2x" />
            <bm-label-icon v-else :icon="action.icon('plus')">
                {{ $t("mail.main.new") }}
            </bm-label-icon>
        </bm-button>
    </mail-open-in-popup-with-shift>
</template>
<script>
import { BmButton, BmClipping, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import { mapGetters } from "vuex";
import { MY_DRAFTS } from "~/getters";
import { MailRoutesMixin } from "~/mixins";
import MailOpenInPopupWithShift from "./MailOpenInPopupWithShift";

export default {
    name: "NewMessage",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon,
        MailOpenInPopupWithShift
    },
    directives: { BmClipping },
    mixins: [MailRoutesMixin],
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
            return this.draftPath(this.MY_DRAFTS);
        }
    },
    methods: {
        openComposer() {
            this.$router.navigate({ name: "mail:message", params: { messagepath: this.messagepath } });
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
