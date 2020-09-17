import UUIDGenerator from "@bluemind/uuid";

export default function (action, alertCode, hasLoading, propsProvider) {
    return async (store, payload) => {
        const loadingAlertUid = UUIDGenerator.generate();
        const { loadingProps, successProps, errorProps } = propsProvider(store, payload);
        if (hasLoading) {
            store.commit(
                "addApplicationAlert",
                {
                    code: alertCode + "_LOADING",
                    uid: loadingAlertUid,
                    props: loadingProps
                },
                { root: true }
            );
        }
        try {
            await action(store, payload);
            removeLoadingAlert(hasLoading, store, loadingAlertUid);
            store.commit(
                "addApplicationAlert",
                {
                    code: alertCode + "_OK",
                    props: successProps
                },
                { root: true }
            );
        } catch (e) {
            removeLoadingAlert(hasLoading, store, loadingAlertUid);
            store.commit(
                "addApplicationAlert",
                {
                    code: alertCode + "_ERROR",
                    props: { ...errorProps, reason: e }
                },
                { root: true }
            );
        }
    };
}

function removeLoadingAlert(hasLoading, store, loadingAlertUid) {
    if (hasLoading) {
        store.commit("removeApplicationAlert", loadingAlertUid, { root: true });
    }
}
