<template>
    <div class="chooser-button">
        <bm-dropdown-item-button variant="compact" size="lg" icon="cloud" @click="openChooser">
            {{ $t("filehosting.drive.from") }}
        </bm-dropdown-item-button>
        <chooser ref="chooser-modal" :max-attachments-size="maxAttachmentsSize" @insert="insertFiles" />
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmDropdownItemButton } from "@bluemind/ui-components";
import { MimeType } from "@bluemind/email";
import { Chooser } from "@bluemind/business-components";
import { LINK_FH_ATTACHMENT } from "../store/types/actions";
import getContentWithLinks from "../helpers/getContentWithLinks";

export default {
    name: "ChooserButton",
    components: { BmDropdownItemButton, Chooser },
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
            this.$refs["chooser-modal"].open();
        },
        async insertFiles(items, insertAsLink) {
            this.$refs["chooser-modal"].hide();
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
                    return this.$store.dispatch(`mail/${LINK_FH_ATTACHMENT}`, {
                        file: addMimeType(file),
                        message: this.message
                    });
                })
            );

            const newContent = getContentWithLinks(this, this.message);
            this.$store.dispatch("mail/SET_DRAFT_CONTENT", { html: newContent, draft: this.message });
        }
    }
};
function addMimeType(file) {
    return { ...file, type: MimeType.getFromFilename(file.name) };
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

#chooser-modal {
    header.modal-header {
        background-color: $neutral-bg-lo1;
        padding-left: $sp-7;
    }
    .modal-content {
        height: 80vh;
    }
    .modal-body {
        padding: 0;
    }
}
</style>
