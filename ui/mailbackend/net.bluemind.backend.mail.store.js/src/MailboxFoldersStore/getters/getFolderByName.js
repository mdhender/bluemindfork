export function getFolderByName(state) {
    return name => state.items.find(item => !item.value.parentUid && name == item.value.name);
}
