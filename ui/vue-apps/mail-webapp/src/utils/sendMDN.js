import { PartsBuilder } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { messageUtils, partUtils } from "@bluemind/mail";
import { retrieveTaskResult } from "@bluemind/task";

/**
 * Send a reading confirmation.
 * @param {Object} from the sender of the MDN
 * @param {Object} to the recipient of the MDN
 * @param {Object} message the original message for which a reading confirmation is asked
 * @param {String} outboxUid the Outbox identifier
 */
export default async function sendMDN(from, to, message, outboxUid) {
    const service = inject("MailboxItemsPersistence", outboxUid);

    const textAddress = await service.uploadPart(
        partUtils.sanitizeTextPartForCyrus(`Ceci est un accusé de réception pour votre message

	À: ${from.dn ? from.dn + " <" + from.address + ">" : from.address}
	Sujet: ${message.subject}
	Date: ${message.date.toLocaleString()}

Note : Cet accusé de réception indique seulement que le message a été
affiché sur l'ordinateur du destinataire. Il n'y a aucune garantie que le
destinataire a lu ou compris le contenu du message.
`)
    );

    const reportAddress = await service.uploadPart(`Reporting-UA: BlueMind Mail App
Original-Recipient: ${to.dn ? to.dn + " <" + to.address + ">" : to.address}
Final-Recipient: rfc822; ${to.address}
Original-Message-ID: ${message.messageId}
Disposition: manual-action/MDN-sent-manually; displayed
`);

    const textPart = PartsBuilder.createTextPart(textAddress);
    const reportPart = PartsBuilder.createReportPart(reportAddress, "MDN.txt");
    let structure = PartsBuilder.createMultipartReport(textPart, reportPart);

    const date = new Date();
    const report = {
        from,
        to: [to],
        subject: `Accusé de réception (lu): ${message.subject}`,
        date,
        headers: [
            messageUtils.generateMessageIDHeader(from.address),
            { name: messageUtils.MessageHeader.REFERENCES, values: [message.messageId] },
            { name: messageUtils.MessageHeader.X_BM_DRAFT_REFRESH_DATE, values: [date.getTime()] }
        ],
        remoteRef: { internaId: 0 }
    };

    await service.create(messageUtils.MessageAdaptor.toMailboxItem(report, structure));

    await inject("OutboxPersistence")
        .flush()
        .then(taskRef => {
            const taskService = inject("TaskService", taskRef.id);
            return retrieveTaskResult(taskService);
        });
}
