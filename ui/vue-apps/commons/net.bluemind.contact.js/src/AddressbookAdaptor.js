export default {
    fromContainerSubscriptionModel(containerSubscriptionModel) {
        return {
            containerUid: containerSubscriptionModel.containerUid,
            name: containerSubscriptionModel.name
        };
    }
};
