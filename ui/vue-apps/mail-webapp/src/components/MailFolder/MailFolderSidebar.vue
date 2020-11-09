<template>
    <bm-col cols="10" lg="12" class="mail-folder-sidebar-wrapper bg-surface h-100">
        <mail-folder-sidebar-header />
        <nav class="mail-folder-sidebar h-100 scroller-y scroller-visible-on-hover">
            <mail-folder-tree
                :tree="buildTree(MY_MAILBOX_FOLDERS)"
                :collapse-name="mailboxEmail"
                show-input
                @toggle-folders="$emit('toggle-folders')"
            />
            <mail-folder-tree
                v-if="MAILSHARE_KEYS.length > 0"
                :tree="buildTree(MAILSHARE_FOLDERS)"
                :collapse-name="$t('common.mailshares')"
                @toggle-folders="$emit('toggle-folders')"
            />
        </nav>
    </bm-col>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import injector from "@bluemind/inject";
import { BmCol } from "@bluemind/styleguide";
import { DEFAULT_FOLDERS } from "../../store/folders/helpers/DefaultFolders";
import { MAILSHARE_FOLDERS, MY_MAILBOX_FOLDERS, MAILSHARE_KEYS, FOLDER_HAS_CHILDREN } from "~getters";
import MailFolderTree from "./MailFolderTree";
import MailFolderSidebarHeader from "./MailFolderSidebarHeader";

export default {
    name: "MailFolderSidebar",
    components: { MailFolderTree, BmCol, MailFolderSidebarHeader },
    data() {
        const userSession = injector.getProvider("UserSession").get();
        const mailboxEmail = userSession.defaultEmail;

        return {
            mailboxEmail
        };
    },
    computed: {
        ...mapGetters("mail", {
            FOLDER_HAS_CHILDREN,
            MAILSHARE_FOLDERS,
            MY_MAILBOX_FOLDERS,
            MAILSHARE_KEYS
        }),
        ...mapState("mail", ["folders"])
    },
    methods: {
        buildTree(foldersKey) {
            const adaptedFolders = foldersKey
                .map(key => this.folders[key])
                .filter(folder => !folder.parent || (folder.parent && this.folders[folder.parent].expanded)) // treat only displayed folders
                .map(folder => toTreeItem(folder, this.FOLDER_HAS_CHILDREN));
            return buildTreeMap(adaptedFolders);
        }
    }
};

function toTreeItem(folder, FOLDER_HAS_CHILDREN) {
    return {
        key: folder.key,
        name: folder.name,
        imapName: folder.imapName,
        expanded: folder.expanded,
        children: [],
        hasChildren: FOLDER_HAS_CHILDREN(folder.key),
        parent: folder.parent
    };
}

function buildTreeMap(nodes) {
    const nodeMap = new Map();
    nodes.forEach(node => {
        const siblings = nodeMap.has(node.parent) ? nodeMap.get(node.parent) : [];
        const children = nodeMap.has(node.key) ? nodeMap.get(node.key) : [];
        siblings.push(node);
        siblings.sort(compare);
        children.sort(compare);
        node.children = children;
        nodeMap.set(node.parent, siblings);
        nodeMap.set(node.key, children);
    });
    return nodeMap.get(null) || [];
}

function compare(f1, f2) {
    const f1Weight = DEFAULT_FOLDERS.indexOf(f1.imapName);
    const f2Weight = DEFAULT_FOLDERS.indexOf(f2.imapName);
    if (f1Weight >= 0 && f2Weight >= 0) {
        return f1Weight - f2Weight;
    } else if (f1Weight >= 0 && f2Weight < 0) {
        return -1;
    } else if (f1Weight < 0 && f2Weight >= 0) {
        return 1;
    } else {
        return f1.imapName.localeCompare(f2.imapName);
    }
}
</script>
<style>
.mail-folder-sidebar {
    min-width: 100%;
}
</style>
