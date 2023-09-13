<template>
    <span class="forwarded-event-alert">
        <default-alert :alert="alert" />
        <bm-read-more href="https://doc.bluemind.net/release/5.0/waiting-for-valid-link-to-the-doc" />
    </span>
</template>

<script>
import { DefaultAlert } from "@bluemind/alert.store";
import { BmReadMore } from "@bluemind/ui-components";

export default {
    name: "ForwardedEventAlert",
    components: { BmReadMore, DefaultAlert },
    props: {
        alert: {
            type: Object,
            required: true
        }
    }
};
</script>
