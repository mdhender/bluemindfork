<template>
    <div class="chooser-button">
        <bm-icon-button
            v-b-modal.chooser
            variant="compact"
            size="lg"
            icon="cloud"
            :title="$tc('mail.filehosting.from_drive')"
            @click="openChooser"
        />
        <chooser-modal :max-attachments-size="maxAttachmentsSize" @insert="insertFiles" />
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmIconButton } from "@bluemind/styleguide";
import { MimeType } from "@bluemind/email";
import { ChooserModal } from "@bluemind/business-components";
import getContentWithLinks from "../helpers/getContentWithLinks";

export default {
    name: "ChooserButton",
    components: { BmIconButton, ChooserModal },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        maxAttachmentsSize() {
            return this.$store.state.mail.messageCompose.maxMessageSize - this.message.size;
        }
    },
    methods: {
        openChooser() {
            this.$bvModal.show("chooser-modal");
        },
        async insertFiles(items, insertAsLink) {
            this.$bvModal.hide("chooser-modal");
            if (insertAsLink) {
                await this.linkAttachments(items);
            } else {
                const service = inject("FileHostingPersistence");

                const filesPromises = items.map(async item => {
                    const content = await service.get(encodeURIComponent(item.path));
                    const file = new File([content], item.name, addMimeType(item));
                    return file;
                });
                const files = await Promise.all(filesPromises);
                this.$execute("add-attachments", { files, message: this.message });
            }
        },
        async linkAttachments(files) {
            await Promise.all(
                files.map(file => {
                    return this.$store.dispatch(`mail/LINK_FH_ATTACHMENT`, {
                        file: addMimeType(file),
                        message: this.message
                    });
                })
            );

            const newContent = getContentWithLinks(this, this.message);
            this.$store.commit("mail/SET_DRAFT_EDITOR_CONTENT", newContent);
        }
    }
};
function addMimeType(file) {
    return { ...file, type: MimeType.getFromFilename(file.name) };
}
</script>
