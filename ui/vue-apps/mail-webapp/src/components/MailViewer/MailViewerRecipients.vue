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
            >{{ $t("common.to") }}</mail-viewer-recipient
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
            >{{ $t("common.cc") }}</mail-viewer-recipient
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
            >{{ $t("common.bcc") }}</mail-viewer-recipient
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
import { BmModal, BmPopover } from "@bluemind/ui-components";
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
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";
@import "~@bluemind/ui-components/src/css/variables";

.mail-viewer-recipients {
    display: flex;
    flex-direction: column;
    gap: $sp-3;
}

#mail-viewer-recipients-modal-to___BV_modal_outer_,
#mail-viewer-recipients-modal-cc___BV_modal_outer_,
#mail-viewer-recipients-modal-bcc___BV_modal_outer_ {
    @include from-lg {
        display: none;
    }
    .modal-dialog {
        max-width: none;
        width: 75%;
        .modal-content {
            max-height: 75%;
            padding: 0;
            .modal-body {
                margin-bottom: 0;
            }
        }
    }
}
.popover {
    @include until-lg {
        display: none !important;
    }
    max-width: 50vw !important;
    min-width: 28.25rem;
    max-height: 65vh;
    overflow: auto;

    .arrow {
        display: none;
    }
}
</style>
