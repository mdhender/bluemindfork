<template>
    <bm-command-button
        v-bind="[$attrs, $props]"
        :icon="icon"
        :icon-when-checked="icon"
        :label="label"
        :label-when-checked="label"
        :success-when-checked="false"
        @click="action"
        v-on="$listeners"
    />
</template>

<script>
import { BmCommandButton } from "@bluemind/styleguide";

export default {
    name: "ReplyToInvitationButton",
    components: { BmCommandButton },
    props: {
        icon: {
            type: String,
            required: true
        },
        label: {
            type: String,
            required: true
        }
    }
};
</script>
