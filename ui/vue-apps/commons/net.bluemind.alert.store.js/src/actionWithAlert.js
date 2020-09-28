import UUIDGenerator from "@bluemind/uuid";

export default function (action, alertCode, hasLoading, propsProvider) {
    return async (store, payload) => {
        const loadingAlertUid = UUIDGenerator.generate();
        const beforeActionProps = propsProvider(store, payload);
        if (hasLoading) {
            store.commit(
                "addApplicationAlert",
                {
                    code: alertCode + "_LOADING",
                    uid: loadingAlertUid,
                    props: beforeActionProps
                },
                { root: true }
            );
        }
        try {
            const actionResult = await action(store, payload);
            const props = actionResult ? { ...beforeActionProps, ...actionResult } : beforeActionProps;
            removeLoadingAlert(hasLoading, store, loadingAlertUid);
            store.commit(
                "addApplicationAlert",
                {
                    code: alertCode + "_OK",
                    props
                },
                { root: true }
            );

            return actionResult;
        } catch (e) {
            removeLoadingAlert(hasLoading, store, loadingAlertUid);
            store.commit(
                "addApplicationAlert",
                {
                    code: alertCode + "_ERROR",
                    props: { ...beforeActionProps, reason: e }
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
