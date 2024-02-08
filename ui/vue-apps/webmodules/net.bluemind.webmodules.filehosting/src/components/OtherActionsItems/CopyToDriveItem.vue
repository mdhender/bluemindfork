<template>
    <bm-toolbar-icon-button
        v-if="!fhFile && !message.composing"
        class="copy-to-drive-item"
        :disabled="isToolarge"
        icon="cloud"
        @click.stop="addToDrive"
    >
        {{ $t("filehosting.drive.add") }}
    </bm-toolbar-icon-button>
</template>

<script>
import { BmToolbarIconButton } from "@bluemind/ui-components";
import OtherActionsMixin from "~/mixins/OtherActionsMixin";
import { SHARE_ATTACHMENT } from "~/store/types/actions";

export default {
    name: "CopyToDriveItem",
    components: { BmToolbarIconButton },
    mixins: [OtherActionsMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    methods: {
        addToDrive() {
            this.$store.dispatch(`mail/${SHARE_ATTACHMENT}`, this.file);
        }
    }
};
</script>
