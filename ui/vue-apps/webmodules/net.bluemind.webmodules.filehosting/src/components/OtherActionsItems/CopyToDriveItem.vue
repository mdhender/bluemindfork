<template>
    <bm-dropdown-item
        v-if="!fhFile"
        class="copy-to-drive-item"
        :disabled="isToolarge"
        icon="cloud"
        @click.stop="addToDrive"
    >
        {{ $t("filehosting.drive.add") }}
    </bm-dropdown-item>
</template>

<script>
import { BmDropdownItem } from "@bluemind/ui-components";
import OtherActionsMixin from "~/mixins/OtherActionsMixin";
import { SHARE_ATTACHMENT } from "~/store/types/actions";

export default {
    name: "CopyToDriveItem",
    components: { BmDropdownItem },
    mixins: [OtherActionsMixin],
    methods: {
        addToDrive() {
            this.$store.dispatch(`mail/${SHARE_ATTACHMENT}`, this.file);
        }
    }
};
</script>
