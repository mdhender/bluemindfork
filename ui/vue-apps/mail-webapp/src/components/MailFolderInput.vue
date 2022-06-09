<template>
    <div class="mail-folder-input flex-fill d-none d-lg-flex align-items-center position-relative">
        <bm-form-input
            ref="input"
            v-model="newFolderName"
            type="text"
            class="d-inline-block flex-fill"
            variant="underline"
            icon="plus"
            left-icon
            resettable
            :placeholder="folder ? '' : $t('mail.folder.new.from_scratch')"
            :state="isFolderValid"
            aria-describedby="mail-folder-input-invalid"
            @focus="isActive = true"
            @focusout="onFocusOut"
            @keydown.enter="submit"
            @keydown.esc="closeInput"
            @reset="closeInput"
        />
        <bm-notice
            v-if="isFolderValid === false"
            id="mail-folder-input-invalid"
            :text="folderValidity"
            class="position-absolute z-index-110 mx-2"
        />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { BmFormInput, BmNotice } from "@bluemind/styleguide";
import { folder } from "@bluemind/mail";
import { FOLDER_BY_PATH } from "~/getters";

const { isNameValid, normalize } = folder;

export default {
    name: "MailFolderInput",
    components: {
        BmFormInput,
        BmNotice
    },
    props: {
        folder: {
            type: Object,
            default: null
        },
        shared: {
            type: Boolean,
            default: false
        },
        submitOnFocusout: {
            type: Boolean,
            default: true
        },
        mailboxes: {
            type: Array,
            required: true
        }
    },
    data() {
        return { isActive: false, newFolderName: (this.folder && this.folder.name) || "" };
    },
    computed: {
        ...mapGetters("mail", { FOLDER_BY_PATH }),
        folderValidity() {
            if ((this.folder && this.folder.name === this.newFolderName) || this.newFolderName === "") {
                return true;
            }
            return isNameValid(this.newFolderName, this.path, this.folderByPath);
        },
        /** @return true if valid, false if not valid or null if can not check validity */
        isFolderValid() {
            if (!this.newFolderName) {
                return null;
            }
            if (this.folderValidity === true) {
                return true;
            }
            return false;
        },
        path() {
            let path = "";
            if (this.isRename) {
                const splitted = this.folder.path.split("/");
                splitted.pop();
                path = splitted ? splitted + "/" : "";
            } else if (this.folder) {
                path = this.folder.path + "/";
            }
            path += this.newFolderName;
            return path;
        },
        isRename() {
            return this.folder && this.folder.name !== "";
        }
    },
    watch: {
        folder() {
            this.newFolderName = this.folder.name;
        },
        newFolderName() {
            if (this.newFolderName !== "" && !this.isActive) {
                this.isActive = true;
            }
        }
    },
    methods: {
        folderByPath: function (path) {
            return this.FOLDER_BY_PATH(path, this.mailboxes[0]);
        },
        closeInput() {
            this.$emit("close");
            this.isActive = false;
            this.newFolderName = (this.folder && this.folder.name) || "";
        },
        submit() {
            if (this.isFolderValid) {
                if (this.folder && this.folder.name === this.newFolderName) {
                    return;
                }
                const normalizedName = normalize(this.newFolderName, this.folderByPath);
                this.$emit("submit", normalizedName);
                this.closeInput();
            }
        },
        onFocusOut() {
            if (
                !this.$el.contains(document.activeElement) &&
                !this.$el.contains(event.relatedTarget) &&
                this.isActive
            ) {
                if (!this.isFolderValid || !this.submitOnFocusout) {
                    this.closeInput();
                } else {
                    this.submit();
                }
            }
        },
        select() {
            this.$refs["input"].select();
        },
        focus() {
            this.$refs["input"].focus();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.mail-folder-input {
    .bm-form-input input:not(:focus) {
        border-bottom-color: $neutral-fg-lo3;
    }
    .bm-notice {
        top: 30px;
        left: 0px;
        width: calc(100% - 2 * #{$sp-2});
    }
}
</style>
