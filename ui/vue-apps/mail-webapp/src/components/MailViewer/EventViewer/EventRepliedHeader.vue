<script setup>
import { loadingStatusUtils } from "@bluemind/mail";
import { computed } from "vue";
import { REPLY_ACTIONS } from "./currentEvent";

const { LoadingStatus } = loadingStatusUtils;

const props = defineProps({
    currentEvent: { type: Object, required: true },
    fromAttendee: { type: Object, required: true },
    isOccurrence: { type: Boolean, default: false }
});

const eventKey = computed(() =>
    props.isOccurrence ? "mail.viewer.invitation.replyOccurrence" : "mail.viewer.invitation.reply"
);

const eventStatusKey = computed(() => {
    switch (props.fromAttendee.status) {
        case REPLY_ACTIONS.ACCEPTED:
            return props.isOccurrence
                ? "mail.viewer.invitationReply.acceptedOccurrence"
                : "mail.viewer.invitationReply.accepted";
        case REPLY_ACTIONS.TENTATIVE:
            return props.isOccurrence
                ? "mail.viewer.invitationReply.tentativeOccurrence"
                : "mail.viewer.invitationReply.tentative";
        case REPLY_ACTIONS.DECLINED:
            return props.isOccurrence
                ? "mail.viewer.invitationReply.declinedOccurrence"
                : "mail.viewer.invitationReply.declined";
        default:
            return undefined;
    }
});
</script>

<template>
    <div v-if="currentEvent.loading === LoadingStatus.LOADED && eventStatusKey" class="event-replied-header">
        <i18n :path="eventKey" tag="span" class="label pl-3 font-weight-bold">
            <template #name>{{ fromAttendee.name }}</template>
            <template #status>
                <span :class="`event-replied-status-${fromAttendee.status.toLowerCase()}`">
                    {{ $t(eventStatusKey) }}
                </span>
            </template>
        </i18n>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-replied-header {
    @include until-lg {
        display: grid;
        gap: $sp-4;
    }
    @include from-lg {
        display: flex;
        align-items: center;
        padding: $sp-5 $sp-5 $sp-5 $sp-6;
        .label {
            text-align: center;
        }
    }
    border-width: 1px 0 1px 0;
    border-style: solid;
    border-color: $neutral-fg-lo3;
    padding: $sp-5;
    background-color: $neutral-bg;
    min-height: 50px;
    .label {
        line-height: $line-height-sm;

        .event-replied-status-accepted {
            color: $success-fg-hi1;
        }
        .event-replied-status-tentative {
            color: $warning-fg-hi1;
        }
        .event-replied-status-declined {
            color: $danger-fg-hi1;
        }
    }
}
</style>
