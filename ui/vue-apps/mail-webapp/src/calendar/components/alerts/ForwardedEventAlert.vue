<template>
    <span class="forwarded-event-alert">
        <i18n path="alert.mail.forwarded_event.info">
            <template #sender>
                <span>{{ alert.payload.sender }}</span>
            </template>
            <template #organizer>
                <span class="organizer">{{ alert.payload.organizer }}</span>
            </template>
        </i18n>
        <bm-read-more href="https://doc.bluemind.net/release/5.0/waiting-for-valid-link-to-the-doc" />
    </span>
</template>

<script>
import { DefaultAlert } from "@bluemind/alert.store";
import { BmReadMore } from "@bluemind/ui-components";

export default {
    name: "ForwardedEventAlert",
    components: { BmReadMore },
    props: {
        alert: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.forwarded-event-alert {
    .organizer {
        font-weight: $font-weight-bold;
    }
}
</style>
