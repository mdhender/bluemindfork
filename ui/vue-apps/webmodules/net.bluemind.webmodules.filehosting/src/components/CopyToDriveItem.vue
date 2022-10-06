<template>
    <bm-dropdown-item
        v-if="!isFhFile"
        class="copy-to-drive-item"
        :disabled="isToolarge"
        icon="cloud-up"
        @click.stop="addToDrive"
    >
        {{ $t("mail.filehosting.drive.add") }}
    </bm-dropdown-item>
</template>

<script>
import { mapGetters } from "vuex";
import { BmDropdownItem } from "@bluemind/styleguide";
import FilehostingL10N from "../l10n";
import { GET_FH_FILE } from "../store/types/getters";
import { GET_CONFIGURATION, SHARE_ATTACHMENT } from "../store/types/actions";

export default {
    name: "CopyToDriveItem",
    components: { BmDropdownItem },
    componentI18N: { messages: FilehostingL10N },
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            maxFilesize: null
        };
    },
    computed: {
        ...mapGetters("mail", [GET_FH_FILE]),
        isToolarge() {
            return this.maxFilesize === null || !(this.maxFilesize > this.file.size || this.maxFilesize === 0);
        },
        isFhFile() {
            return !!this.GET_FH_FILE(this.file);
        }
    },
    async beforeMount() {
        const { maxFilesize } = await this.$store.dispatch(`mail/${GET_CONFIGURATION}`);
        this.maxFilesize = maxFilesize;
    },
    methods: {
        addToDrive() {
            this.$store.dispatch(`mail/${SHARE_ATTACHMENT}`, this.file);
        }
    }
};
</script>
