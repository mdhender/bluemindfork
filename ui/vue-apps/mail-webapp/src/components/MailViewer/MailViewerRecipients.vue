<template>
    <div class="mail-viewer-recipients" tabindex="0" @keyup.esc.prevent.stop="toShowMore = false">
        <mail-viewer-recipient
            v-if="message.to.length"
            id="mail-viewer-recipient-to"
            :recipients="message.to"
            @show-more="
                ccShowMore = false;
                bccShowMore = false;
                toShowMore = !toShowMore;
            "
            >{{ $t("mail.content.to") }}</mail-viewer-recipient
        >
        <mail-viewer-recipient
            v-if="message.cc.length"
            id="mail-viewer-recipient-cc"
            :recipients="message.cc"
            @show-more="
                toShowMore = false;
                ccShowMore = false;
                ccShowMore = !ccShowMore;
            "
            >{{ $t("mail.content.copy") }}</mail-viewer-recipient
        >
        <mail-viewer-recipient
            v-if="message.bcc.length"
            id="mail-viewer-recipient-bcc"
            :recipients="message.bcc"
            @show-more="
                toShowMore = false;
                ccShowMore = false;
                bccShowMore = !bccShowMore;
            "
            >{{ $t("mail.content.blindcopy") }}</mail-viewer-recipient
        >
        <bm-popover
            v-if="message.to.length"
            target="mail-viewer-recipient-to"
            triggers="manuel"
            :show.sync="toShowMore"
            placement="bottom"
            no-fade
        >
            <mail-viewer-recipients-more-content :message="message" @close="toShowMore = false" />
        </bm-popover>
        <bm-popover
            v-if="message.cc.length"
            target="mail-viewer-recipient-cc"
            triggers="manuel"
            :show.sync="ccShowMore"
            placement="bottom"
            no-fade
        >
            <mail-viewer-recipients-more-content :message="message" @close="ccShowMore = false" />
        </bm-popover>
        <bm-popover
            v-if="message.bcc.length"
            target="mail-viewer-recipient-bcc"
            triggers="manuel"
            :show.sync="bccShowMore"
            placement="bottom"
            no-fade
        >
            <mail-viewer-recipients-more-content :message="message" @close="bccShowMore = false" />
        </bm-popover>
        <bm-modal
            v-if="message.to.length"
            id="mail-viewer-recipients-modal-to"
            v-model="toShowMore"
            centered
            hide-footer
            hide-header
        >
            <mail-viewer-recipients-more-content :message="message" class="pt-5" hide-close />
        </bm-modal>
        <bm-modal
            v-if="message.cc.length"
            id="mail-viewer-recipients-modal-cc"
            v-model="ccShowMore"
            centered
            hide-footer
            hide-header
        >
            <mail-viewer-recipients-more-content :message="message" class="pt-5" hide-close />
        </bm-modal>
        <bm-modal
            v-if="message.bcc.length"
            id="mail-viewer-recipients-modal-bcc"
            v-model="bccShowMore"
            centered
            hide-footer
            hide-header
        >
            <mail-viewer-recipients-more-content :message="message" class="pt-5" hide-close />
        </bm-modal>
    </div>
</template>

<script>
import { BmModal, BmPopover } from "@bluemind/styleguide";
import MailViewerRecipient from "./MailViewerRecipient";
import MailViewerRecipientsMoreContent from "./MailViewerRecipientsMoreContent";

export default {
    name: "MailViewerRecipients",
    components: { BmModal, BmPopover, MailViewerRecipient, MailViewerRecipientsMoreContent },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { bccShowMore: false, ccShowMore: false, toShowMore: false };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

#mail-viewer-recipients-modal-to___BV_modal_outer_,
#mail-viewer-recipients-modal-cc___BV_modal_outer_,
#mail-viewer-recipients-modal-bcc___BV_modal_outer_ {
    @media (min-width: map-get($grid-breakpoints, "lg")) {
        display: none !important;
    }
    .modal-dialog {
        max-width: none;
        width: 75%;
        .modal-content {
            max-height: 75%;
        }
    }
}
.popover {
    @media (max-width: map-get($grid-breakpoints, "lg")) {
        display: none !important;
    }
    max-width: 50vw !important;
    min-width: 28.25rem;
    max-height: 65vh;
    overflow: auto;
    .popover-body {
        padding-top: 0;
    }
}
.popover .arrow {
    display: none;
}
</style>
