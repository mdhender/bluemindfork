import { inject } from "@bluemind/inject";
import { messageUtils } from "@bluemind/mail";
import { retrieveTaskResult } from "@bluemind/task";
import { buildStructure } from "./EmlTemplate";

export default async function sendTemplate({ template, parameters, from, to, outboxUid, additionalHeaders }) {
    const service = inject("MailboxItemsPersistence", outboxUid);
    const structure = buildStructure(template, parameters);
    const subject = structure.subject;
    delete structure.subject;
    await walk(structure, structureNode => createPart(structureNode, parameters, service));
    const date = new Date();
    const message = {
        from,
        to: [to],
        subject,
        date,
        headers: [
            messageUtils.generateMessageIDHeader(from.address),
            { name: messageUtils.MessageHeader.X_BM_DRAFT_REFRESH_DATE, values: [date.getTime()] },
            ...additionalHeaders
        ],
        remoteRef: { internaId: 0 }
    };
    await service.create(messageUtils.MessageAdaptor.toMailboxItem(message, structure));
    await inject("OutboxPersistence")
        .flush()
        .then(taskRef => {
            const taskService = inject("TaskService", taskRef.id);
            return retrieveTaskResult(taskService);
        });
}

async function walk(structureNode, fn) {
    await fn(structureNode);
    const promises = [];
    structureNode.children?.forEach(child => promises.push(walk(child, fn)));
    return Promise.all(promises);
}

async function createPart(structureNode, parameters, service) {
    if (!structureNode.children?.length) {
        structureNode.address = await service.uploadPart(structureNode.content);
        delete structureNode.content;
    }
}
