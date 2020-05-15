<template>
    <div
        :class="
            isNewFolderNameValid === true ? 'valid border-bottom border-primary' : 'invalid border-bottom border-danger'
        "
        class="mail-folder-input flex-fill d-flex align-items-center position-relative"
    >
        <bm-icon :icon="shared ? 'folder-shared' : 'folder'" />
        <bm-form-input
            ref="input"
            v-model="newFolderName"
            type="text"
            class="d-inline-block flex-fill"
            reset
            @focusout="onInputFocusOut"
            @keydown.enter="edit"
            @keydown.esc="cancelEdit"
            @keydown.left.stop
            @keydown.right.stop
            @mousedown.stop
            @reset="cancelEdit"
        />
        <bm-notice
            v-if="isNewFolderNameValid !== true"
            :text="isNewFolderNameValid"
            class="position-absolute w-100 z-index-110 mx-2"
        />
    </div>
</template>

<script>
import { BmFormInput, BmIcon, BmNotice } from "@bluemind/styleguide";
import { isFolderNameValid } from "@bluemind/backend.mail.store";
import { mapActions, mapGetters, mapState } from "vuex";
import ItemUri from "@bluemind/item-uri";

export default {
    name: "MailFolderInput",
    components: {
        BmFormInput,
        BmIcon,
        BmNotice
    },
    props: {
        folder: {
            type: Object,
            required: false,
            default: null
        },
        shared: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return {
            newFolderName: (this.folder && this.folder.name) || ""
        };
    },
    computed: {
        ...mapGetters("mail-webapp/folders", ["getFolderByPath"]),
        ...mapGetters("mail-webapp", ["mailshares", "my"]),
        ...mapState("mail-webapp", ["currentFolderKey"]),
        isNewFolderNameValid() {
            if (this.newFolderName !== "" && this.newFolderName !== this.folder.name) {
                const currentMailbox = ItemUri.container(this.folder.key);

                const currentFolderName = this.newFolderName.toLowerCase();
                const checkValidity = isFolderNameValid(currentFolderName);
                if (checkValidity !== true) {
                    return this.$t("mail.actions.create.folder.invalid.character", {
                        character: checkValidity
                    });
                }

                let path =
                    this.folder.fullName.substring(0, this.folder.fullName.lastIndexOf("/") + 1) + this.newFolderName;
                const isMailshare = this.my.mailboxUid !== currentMailbox;
                if (isMailshare) {
                    const mailshareName = this.mailshares.find(mailshare => mailshare.uid === currentMailbox).name;
                    path = mailshareName + "/" + path;
                }
                if (this.getFolderByPath(path, currentMailbox)) {
                    return this.$t("mail.actions.create.folder.invalid.already_exist");
                }
            }
            return true;
        }
    },
    watch: {
        folder() {
            this.newFolderName = this.folder.name;
        }
    },
    mounted() {
        this.$nextTick(() => this.$refs["input"].select());
    },
    methods: {
        ...mapActions("mail-webapp", ["renameFolder"]),
        cancelEdit() {
            this.$emit("close");
            this.newFolderName = this.folder.name;
        },
        edit() {
            if (this.isNewFolderNameValid === true && this.newFolderName !== "") {
                if (this.newFolderName !== this.folder.name) {
                    this.renameFolder({ folderKey: this.folder.key, newFolderName: this.newFolderName }).then(() => {
                        if (this.currentFolderKey === this.folder.key) {
                            this.$router.navigate({ name: "v:mail:message", params: { folder: this.folder.key } });
                        }
                    });
                }
                this.$emit("close");
            }
        },
        onInputFocusOut() {
            if (!this.$el.contains(document.activeElement) && !this.$el.contains(event.relatedTarget)) {
                if (this.isNewFolderNameValid !== true || this.newFolderName === "") {
                    this.cancelEdit();
                } else {
                    this.edit();
                }
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-input {
    &.valid .fa-folder,
    &.valid .fa-folder-shared {
        color: $primary;
    }

    &.invalid {
        .fa-folder,
        .fa-folder-shared,
        input {
            color: $danger;
        }
    }

    input {
        border: none !important;
        background-color: transparent !important;
    }

    .bm-notice {
        top: 30px;
    }
}
</style>
