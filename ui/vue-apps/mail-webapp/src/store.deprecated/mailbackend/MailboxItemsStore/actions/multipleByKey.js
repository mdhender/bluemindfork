import actionTypes from "../../../../store/actionTypes";

export function multipleByKey({ dispatch, rootState }, messageKeys) {
    return dispatch(
        "mail/" + actionTypes.FETCH_MESSAGE_METADATA,
        { messageKeys, folders: rootState.mail.folders },
        { root: true }
    );
}
