<script setup>
import { computed } from "vue";
import l10n from "@bluemind/i18n";
import { BmButton, useModal } from "@bluemind/ui-components";
import store from "@bluemind/store";

import { REMOVE_EVENT } from "~/actions";
import EventHeader from "./base/EventHeader";
import EventDetail from "./base/EventDetail";
import EventFooter from "./base/EventFooter";
import EventCancelledAndRemoved from "./EventCancelledAndRemoved";

const props = defineProps({
    message: { type: Object, required: true },
    event: { type: Object, required: true }
});

const isOccurrence = computed(() => !!props.message.eventInfo.recuridIsoDate);
const eventKey = computed(() =>
    isOccurrence.value ? "mail.viewer.invitation.cancelled.occurrence" : "mail.viewer.invitation.cancelled.event"
);
const statusKey = computed(() =>
    isOccurrence.value
        ? "mail.viewer.invitation.cancelled.status.occurrence"
        : "mail.viewer.invitation.cancelled.status"
);

const modal = useModal();

const remove = async () => {
    const confirm = await modal.msgBoxConfirm(l10n.t("mail.viewer.invitation.confirm_remove_event"), {
        title: l10n.t("mail.viewer.invitation.remove_event"),
        okTitle: l10n.t("common.delete"),
        cancelTitle: l10n.t("common.cancel")
    });
    if (await confirm) {
        store.dispatch(`mail/${REMOVE_EVENT}`, { event: props.event });
    }
};
</script>

<template>
    <div class="event-cancelled">
        <event-header>
            <i18n :path="eventKey" tag="span" class="bold event-cancelled-text">
                <template #name>{{ message.from.dn }}</template>
                <template #status>
                    <span class="event-cancelled-status">{{ $t(statusKey) }}</span>
                </template>
            </i18n>
            <template #actions>
                <bm-button variant="outline" icon="calendar-cross" @click="remove">
                    {{ $t("mail.viewer.invitation.remove_event") }}
                </bm-button>
            </template>
        </event-header>
        <div>
            <event-detail :event="event" :message="message" />
            <event-footer v-if="!event.restricted" :event="event" />
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-cancelled {
    display: flex;
    flex-direction: column;
    .event-header {
        gap: $sp-4;
        .label {
            @include regular;
        }
    }
    .event-cancelled-text .event-cancelled-status {
        color: $danger-fg-hi1;
    }

    .bm-illustration {
        flex: none;
    }
}
</style>
