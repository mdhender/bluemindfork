<template>
    <div class="message-list-item-folder-name position-absolute d-flex slide" :class="{ 'slide-out': mouseIn }">
        <div
            class="left-spacer"
            :class="[isActive ? 'gradient-info' : isImportant ? 'gradient-warning-custom' : 'gradient-white']"
        >
            &nbsp;
        </div>
        <mail-folder-icon
            class="pl-1 pr-2 text-secondary text-truncate"
            :class="[isActive ? 'bg-info' : isImportant ? 'warning-custom' : 'bg-white']"
            :shared="isFolderOfMailshare(folder)"
            :folder="folder"
        >
            <i class="font-weight-bold">{{ folder.name }}</i>
        </mail-folder-icon>
    </div>
</template>

<script>
import ItemUri from "@bluemind/item-uri";
import MailFolderIcon from "../MailFolderIcon";
import { mapGetters, mapState } from "vuex";

export default {
    name: "MessageListItemFolderName",
    components: { MailFolderIcon },
    props: {
        message: {
            type: Object,
            required: true
        },
        isImportant: {
            type: Boolean,
            required: true
        },
        mouseIn: {
            type: Boolean,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail-webapp", ["isMessageSelected", "my"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["mailboxes"]),
        folder() {
            return this.my.folders.find(f => f.uid === ItemUri.container(this.message.key));
        },
        isActive() {
            return this.isMessageSelected(this.message.key) || this.message.key === this.currentMessageKey;
        }
    },
    methods: {
        isFolderOfMailshare(folder) {
            return this.mailboxes[folder.mailbox].type === "mailshares";
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item-folder-name {
    max-width: 50%;
    right: 0;

    // obtain the same enlightment that BAlert applies on $warning TODO move to variables.scss in SG
    $custom-warning-color: lighten($warning, 33.9%);

    .left-spacer {
        width: 20px;
    }

    .gradient-warning-custom {
        background-image: linear-gradient(to right, rgba($info, 0), rgba($info, 1));
    }

    .gradient-warning-custom {
        background-image: linear-gradient(to right, rgba($custom-warning-color, 0), rgba($custom-warning-color, 1));
    }

    .gradient-white {
        background-image: linear-gradient(to right, rgba(white, 0), rgba(white, 1));
    }

    .gradient-info {
        background-image: linear-gradient(to right, rgba($info, 0), rgba($info, 1));
    }

    &.slide {
        transition: transform 0.15s linear 0.15s;
        &.slide-out {
            transform: translate(100%, 0px);
        }
    }
}
</style>
