<template>
    <i18n path="mail.list.sr_info" tag="p" class="screen-reader-only-message-information sr-only">
        <template #title>
            <p id="srInfoTitle">{{ $t("mail.list.sr_info.title") }}</p>
        </template>
        <template #props>
            <i18n path="mail.list.sr_info.props" tag="ul" aria-labelledby="srInfoTitle">
                <template v-if="conversation.flags.includes(Flag.FLAGGED)" #flagged>
                    <li>{{ $t("mail.list.sr_info.flagged") }}</li>
                </template>
                <template v-if="conversation.flags.includes(Flag.SEEN)" #read>
                    <li>{{ $t("mail.list.sr_info.read") }}</li>
                </template>
                <template v-else #unread>
                    <li>{{ $t("mail.list.sr_info.unread") }}</li>
                </template>
                <template v-if="conversation.hasICS" #event>
                    <li>{{ $t("mail.list.sr_info.event") }}</li>
                </template>
                <template v-if="conversation.hasAttachment" #attachment>
                    <li>{{ $t("mail.list.sr_info.attachment") }}</li>
                </template>
                <template v-if="conversation.flags.includes(Flag.ANSWERED)" #replied>
                    <li>{{ $t("mail.list.sr_info.replied") }}</li>
                </template>
                <template v-if="conversation.flags.includes(Flag.FORWARDED)" #forwarded>
                    <li>{{ $t("mail.list.sr_info.forwarded") }}</li>
                </template>
            </i18n>
        </template>
    </i18n>
</template>
<script>
import { Flag } from "@bluemind/email";
export default {
    name: "ScreenReaderOnlyConversationInformation",
    props: {
        conversation: {
            type: Object,
            required: true
        }
    },
    data: function () {
        return {
            Flag
        };
    }
};
</script>
