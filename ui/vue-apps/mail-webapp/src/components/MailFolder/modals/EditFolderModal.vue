<template>
    <bm-modal
        v-if="folder"
        ref="modal"
        centered
        :title="title"
        :ok-title="okTitle"
        :ok-disabled="!state"
        :scrollable="false"
        @shown="$refs.input.focus()"
        @ok="edit"
    >
        <bm-form-input
            ref="input"
            v-model="name"
            type="text"
            class="mt-2"
            :state="state"
            :formatter="format"
            @keyup.enter="edit()"
        />
        <bm-notice
            v-if="state === false"
            id="rename-folder-input-notice"
            :text="validityCheck"
            class="position-absolute z-index-110 mx-2"
        />
    </bm-modal>
</template>
<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { BmModal } from "@bluemind/styleguide";
import { BmFormInput, BmNotice } from "@bluemind/styleguide";
import { folder } from "@bluemind/mail";
import { FOLDER_BY_PATH } from "~/getters";
import { CREATE_FOLDER, RENAME_FOLDER } from "~/actions";

const { isNameValid } = folder;

export default {
    name: "EditFolderModal",
    components: { BmModal, BmFormInput, BmNotice },
    data() {
        return { name: "", folder: null };
    },
    computed: {
        ...mapState("mail", ["mailboxes", "activeFolder", "folders"]),
        ...mapGetters("mail", { FOLDER_BY_PATH }),
        isCreate() {
            return !this.folder.remoteRef.uid;
        },
        state() {
            return this.hasChanged ? this.validityCheck === true : null;
        },
        hasChanged() {
            return this.folder.name !== this.name;
        },
        path() {
            return this.folder.path.substring(0, this.folder.path.lastIndexOf("/") + 1) + this.name;
        },
        validityCheck() {
            const key = this.folder.mailboxRef.key;
            return isNameValid(this.name.trim(), this.path, path => this.FOLDER_BY_PATH(path, { key }));
        },
        title() {
            if (this.isCreate) {
                return this.$t("mail.create_subfolder.modal.title", { name: this.folders[this.folder.parent].name });
            }
            return this.$t("mail.rename_folder.modal.title", { name: this.folder.name });
        },
        okTitle() {
            return this.isCreate ? this.$t("common.create") : this.$t("common.rename");
        }
    },
    watch: {
        folder() {
            this.name = this.folder.name;
        }
    },
    methods: {
        ...mapActions("mail", { RENAME_FOLDER, CREATE_FOLDER }),
        format(value) {
            return value.replaceAll("/", "");
        },
        edit() {
            if (this.state === true) {
                const mailbox = this.mailboxes[this.folder.mailboxRef.key];
                if (this.isCreate) {
                    const parent = this.folders[this.folder.parent];
                    this.CREATE_FOLDER({ name: this.name.trim(), parent, mailbox });
                } else {
                    this.RENAME_FOLDER({ folder: this.folder, name: this.name, mailbox });
                    if (this.activeFolder === this.folder.key) {
                        this.$router.navigate({ name: "v:mail:conversation", params: { folder: this.folder.path } });
                    }
                }
                this.$refs["modal"].hide();
            }
        },
        async show(folder) {
            this.folder = folder;
            this.name = this.folder.name;
            await this.$nextTick();
            this.$refs["modal"].show();
        }
    }
};
</script>
