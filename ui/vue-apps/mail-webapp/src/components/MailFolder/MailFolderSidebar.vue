<template>
    <div class="mail-folder-sidebar h-100 scroller-y scroller-visible-on-hover">
        <mail-folder-tree
            :tree="buildTree(myMailboxFolders)"
            :collapse-name="mailboxEmail"
            show-input
            @toggle-folders="$emit('toggle-folders')"
        />
        <mail-folder-tree
            v-if="mailshareKeys.length > 0"
            :tree="buildTree(mailshareFolders)"
            :collapse-name="$t('common.mailshares')"
            @toggle-folders="$emit('toggle-folders')"
        />
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import injector from "@bluemind/inject";
import { DEFAULT_FOLDERS } from "../../store/helpers/DefaultFolders";
import MailFolderTree from "./MailFolderTree";

export default {
    name: "MailFolderSidebar",
    components: { MailFolderTree },
    data() {
        return {
            mailboxEmail: injector.getProvider("UserSession").get().defaultEmail
        };
    },
    computed: {
        ...mapGetters("mail", {
            hasChildrenGetter: "HAS_CHILDREN_GETTER",
            mailshareFolders: "MAILSHARE_FOLDERS",
            myMailboxFolders: "MY_MAILBOX_FOLDERS",
            mailshareKeys: "MAILSHARE_KEYS"
        }),
        ...mapState("mail", ["folders"])
    },
    methods: {
        buildTree(foldersKey) {
            const adaptedFolders = foldersKey
                .map(key => this.folders[key])
                .filter(folder => !folder.parent || (folder.parent && this.folders[folder.parent].expanded)) // treat only displayed folders
                .map(folder => toTreeItem(folder, this.hasChildrenGetter));
            return buildTreeMap(adaptedFolders);
        }
    }
};

function toTreeItem(folder, hasChildrenGetter) {
    return {
        key: folder.key,
        name: folder.name,
        imapName: folder.imapName,
        expanded: folder.expanded,
        children: [],
        hasChildren: hasChildrenGetter(folder.key),
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
<style lang="scss">
.mail-folder-sidebar {
    min-width: 100%;
}
</style>
