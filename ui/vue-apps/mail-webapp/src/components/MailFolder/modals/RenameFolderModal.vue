<template>
    <bm-modal
        :id="id"
        class="rename-folder-modal"
        centered
        :title="$t('mail.folder.rename')"
        :ok-title="$t('common.rename')"
        :ok-disabled="!state"
        auto-focus-button="ok"
        :scrollable="false"
        @ok="rename"
    >
        <bm-form-input ref="input" v-model="name" type="text" class="mt-2" :state="state" :formatter="format" />
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
import { isNameValid } from "~/model/folder";
import { FOLDERS_BY_UPPERCASE_PATH } from "~/getters";
import { RENAME_FOLDER } from "~/actions";

export default {
    name: "RenameFolder",
    components: { BmModal, BmFormInput, BmNotice },
    props: {
        id: {
            type: String,
            required: true
        },
        folder: {
            type: Object,
            required: true
        }
    },
    data() {
        return { name: this.folder?.name };
    },
    computed: {
        ...mapState("mail", ["mailboxes", "activeFolder"]),
        ...mapGetters("mail", { FOLDERS_BY_UPPERCASE_PATH }),
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
            return isNameValid(this.name, this.path, this.FOLDERS_BY_UPPERCASE_PATH);
        }
    },
    watch: {
        folder() {
            this.name = this.folder.name;
        }
    },
    methods: {
        ...mapActions("mail", { RENAME_FOLDER }),
        format(value) {
            return value.replaceAll("/", "");
        },
        rename() {
            const mailbox = this.mailboxes[this.folder.mailboxRef.key];
            this.RENAME_FOLDER({ folder: this.folder, name: this.name, mailbox });
            if (this.activeFolder === this.folder.key) {
                this.$router.navigate({ name: "v:mail:conversation", params: { folder: this.folder.path } });
            }
        }
    }
};
</script>
