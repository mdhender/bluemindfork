<script setup>
import { BmToggleableButton } from "@bluemind/ui-components";
import { loadingStatusUtils } from "@bluemind/mail";
import { REPLY_ACTIONS } from "./currentEvent";

const { LoadingStatus } = loadingStatusUtils;

defineProps({ currentEvent: { type: Object, required: true } });
const emit = defineEmits(["event-replied"]);

const replyActions = [
    { name: REPLY_ACTIONS.ACCEPTED, icon: "check", i18n: "accept" },
    { name: REPLY_ACTIONS.TENTATIVE, icon: "interrogation", i18n: "tentatively" },
    { name: REPLY_ACTIONS.DECLINED, icon: "cross", i18n: "decline" }
];
</script>

<template>
    <div v-if="currentEvent.loading === LoadingStatus.LOADED" class="event-reply-header">
        <span class="label pl-3">{{ $t("mail.viewer.invitation.request") }}</span>
        <div v-if="currentEvent.status" class="reply-buttons">
            <bm-toggleable-button
                v-for="action in replyActions"
                :key="action.name"
                :icon="action.icon"
                :pressed="currentEvent.status === action.name"
                @click="emit('event-replied', action.name)"
            >
                {{ $t(`mail.viewer.invitation.${action.i18n}`) }}
            </bm-toggleable-button>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-reply-header {
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
    justify-content: space-between;
    background-color: $neutral-bg;
    .label {
        line-height: $line-height-sm;
    }
    .reply-buttons {
        display: flex;
        gap: $sp-6;

        .b-skeleton-button {
            height: base-px-to-rem(30);
        }
    }
}
</style>
