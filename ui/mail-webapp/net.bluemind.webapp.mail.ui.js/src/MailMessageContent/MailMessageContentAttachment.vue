<template>
    <div class="py-1 pl-3 d-flex">
        <div class="flex-grow-1 align-self-center">
            <bm-icon icon="paper-clip" class="mr-2" />
            <span class="font-weight-bold pr-2">
                {{ $tc("common.attachments", attachments.length, { count: attachments.length }) }}
            </span>
            <!-- TODO use a real button-->
            <span
                v-for="attachment in attachments"
                :key="attachment"
                class="pr-2 text-secondary"
                style="cursor:pointer;"
            >
                {{ attachment }}
            </span>
        </div>
        <bm-button
            variant="outline-secondary"
            class="mr-2 align-self-center"
            size="sm"
            @click="$emit('saveAllAttachments')"
        >
            {{ $t("common.save_all") }}
        </bm-button>
    </div>
</template>

<script>
import BmButton from "@bluemind/styleguide/components/buttons/BmButton";
import BmIcon from "@bluemind/styleguide/components/BmIcon";

export default {
    name: "MailMessageContentAttachment",
    components: {
        BmButton,
        BmIcon
    },
    props: {
        hasAttachment: {
            type: Boolean,
            required: true
        },
        attachments: {
            type: String,
            required: true
        }
    }
};
</script>
