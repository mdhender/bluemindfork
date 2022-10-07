<template>
    <div class="chooser-button">
        <bm-icon-button
            v-b-modal.chooser
            variant="compact"
            size="lg"
            icon="cloud"
            :title="$tc('filehosting.drive.from')"
            @click="openChooser"
        />
        <bm-modal
            id="chooser-modal"
            hide-footer
            centered
            size="fluid"
            :scrollable="false"
            :title="$t('chooser.choose')"
        >
            <chooser :max-attachments-size="maxAttachmentsSize" @insert="insertFiles" />
        </bm-modal>
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmIconButton, BmModal } from "@bluemind/styleguide";
import { MimeType } from "@bluemind/email";
import { Chooser } from "@bluemind/business-components";
import FilehostingL10N from "../l10n";
import { LINK_FH_ATTACHMENT } from "../store/types/actions";
import getContentWithLinks from "../helpers/getContentWithLinks";

export default {
    name: "ChooserButton",
    components: { BmIconButton, BmModal, Chooser },
    componentI18N: { messages: FilehostingL10N },
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
                    return this.$store.dispatch(`mail/${LINK_FH_ATTACHMENT}`, {
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

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

#chooser-modal {
    header.modal-header {
        background-color: $neutral-bg-lo1;
        padding-left: $sp-7;

        & > .modal-title {
            font-size: $font-size-lg;
        }
    }
    .modal-content {
        height: 80vh;
    }
    .modal-body {
        padding: 0;
    }
}
</style>
