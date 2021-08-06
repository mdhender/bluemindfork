<template>
    <div class="mail-viewer-recipients">
        <bm-row>
            <bm-col cols="12">
                <mail-viewer-recipient v-if="message.to.length > 0" :recipients="message.to"
                    >{{ $t("mail.content.to") }}
                </mail-viewer-recipient>
            </bm-col>
        </bm-row>
        <bm-row class="pb-2">
            <bm-col cols="12">
                <mail-viewer-recipient v-if="message.cc.length > 0" :recipients="message.cc"
                    >{{ $t("mail.content.copy") }}
                </mail-viewer-recipient>
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { BmCol, BmRow } from "@bluemind/styleguide";
import MailViewerRecipient from "./MailViewerRecipient";

export default {
    name: "MailViewerRecipients",
    components: { BmCol, BmRow, MailViewerRecipient },
    props: {
        message: {
            type: Object,
            required: true
        }
    }
};
</script>
