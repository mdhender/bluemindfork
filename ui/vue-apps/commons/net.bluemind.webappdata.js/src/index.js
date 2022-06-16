const AppDataKeys = {
    MAIL_MESSAGE_LIST_WIDTH: "mail:message_list:width",
    MAIL_FOLDERS_EXPANDED: "mail:folders:expanded",
    MAIL_COMPOSITION_SHOW_FORMATTING_TOOLBAR: "mail:composition:show_formatting_toolbar"
};

const KeysWhoseValueIsString = [AppDataKeys.MAIL_MESSAGE_LIST_WIDTH];

function adapt(remote) {
    let adaptedValue = remote.value.value;
    if (!KeysWhoseValueIsString.includes(remote.value.key)) {
        adaptedValue = JSON.parse(adaptedValue);
    }
    return { uid: remote.uid, value: adaptedValue, key: remote.value.key };
}

function toRemote({ key, value }) {
    let adaptedValue = value;
    if (!KeysWhoseValueIsString.includes(key)) {
        adaptedValue = JSON.stringify(adaptedValue);
    }
    return { key, value: adaptedValue };
}

export { AppDataKeys, adapt, toRemote };
