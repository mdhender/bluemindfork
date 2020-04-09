<template>
    <bm-dropzone
        :states="{ active: false }"
        :accept="['message']"
        :value="folder"
        class="w-100 d-flex align-items-center"
        @dragenter="folder.expanded || expandFolder(folder.key)"
    >
        <mail-folder-icon
            :shared="shared"
            :folder="folder"
            breakpoint="xl"
            class="flex-fill"
            :class="folder.unread > 0 ? 'font-weight-bold' : ''"
        />
        <bm-counter-badge
            v-if="folder.unread > 0"
            :value="folder.unread"
            :variant="folder.key != currentFolderKey ? 'secondary' : 'primary'"
            class="mr-1 position-sticky"
        />
    </bm-dropzone>
</template>

<script>
import { BmCounterBadge, BmDropzone } from "@bluemind/styleguide";
import { mapActions, mapState } from "vuex";

import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "MailFolderItem",
    components: {
        BmCounterBadge,
        BmDropzone,
        MailFolderIcon
    },
    props: {
        folder: {
            type: Object,
            required: true
        },
        shared: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey"])
    },
    methods: {
        ...mapActions("mail-webapp", ["expandFolder"])
    }
};
</script>
