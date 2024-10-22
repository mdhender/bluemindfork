import mailText2Html from "../src/mailText2Html";
import ForwardTextTransformer from "../src/transformers/ForwardTextTransformer";

describe("mailText2Html", () => {
    const userLang = "FR";

    test("all text mail transformations apply and produce expected HTML", () => {
        let mailTextContent = `Test mailto : zefze@mail.com
            Test link : https://bm.blue-mind.net/webmail/?_task=mail
            Test bold : *zefzefzef*
            Test tel 1 : 0625486518`;
        const res = mailText2Html(mailTextContent, userLang);
        expect(res).toContain('<a href="mailto:zefze@mail.com" class="linkified">zefze@mail.com</a>');
        expect(res).toContain(
            '<a href="https://bm.blue-mind.net/webmail/?_task=mail" ' +
                'class="linkified" target="_blank">https://bm.blue-mind.net/webmail/?_task=mail</a>'
        );
        expect(res).toContain('<a href="tel:+33625486518">0625486518</a>');
        expect(res).toContain("<strong>*zefzefzef*</strong>");
        expect(res.startsWith("<pre>")).toBe(true);
        expect(res.endsWith("</pre>")).toBe(true);
    });

    test("tel transformer", () => {
        let mailTextContent = `Test tel 1 : 0625486518, tel 2 : 0625486519

            Test tel 3 : +33425786541`;
        const res = mailText2Html(mailTextContent, userLang);
        expect(res).toContain('<a href="tel:+33625486518">0625486518</a>');
        expect(res).toContain('<a href="tel:+33625486519">0625486519</a>');
        expect(res).toContain('<a href="tel:+33425786541">+33425786541</a>');
    });

    test("tel transformer must not apply on a line containing HTML", () => {
        let mailTextContent = `
            [Tel should not apply] Site & call: https://bluemind.net/callme?+33425786541
            [Tel should apply] <a href="https://bluemind.net">https://bluemind.net</a> and call me on +33425786541 
            `;
        const html = mailText2Html(mailTextContent, userLang);
        expect(html).toContain(
            ' <a href="https://bluemind.net/callme?+33425786541" class="linkified" target="_blank">https://bluemind.net/callme?+33425786541</a>'
        );
        expect(html).toContain('<a href="tel:+33425786541">+33425786541</a>');
    });

    test("replyTransformer", () => {
        let mailTextContent = `Réponse 3
Le 14/03/2019 à 17:53, test a écrit :
> Réponse 2
> Le 14/03/2019 à 17:53, test a écrit :
>> Réponse 1
>> Le 04/03/2019 à 15:21, test a écrit :
>>> zrerger
>>> *fzerfrze*
`;
        const res = mailText2Html(mailTextContent, userLang);
        let expectedHTML = `<pre>Réponse 3
Le 14/03/2019 à 17:53, test a écrit :
<blockquote class="reply"> Réponse 2
 Le 14/03/2019 à 17:53, test a écrit :
<blockquote class="reply"> Réponse 1
 Le 04/03/2019 à 15:21, test a écrit :
<blockquote class="reply"> zrerger
 <strong>*fzerfrze*</strong>
</blockquote></blockquote></blockquote></pre>`;
        expect(res).toContain(expectedHTML);
    });

    test("forwardTransformer", () => {
        const transformer = new ForwardTextTransformer(null);
        let mailTextContent = `-------- Message transféré --------
            Sujet : blabla
            Date : Tue, 19 Mar 2019 18:25:34 +0100
            De : test <test@bluemind.loc>
            Pour : test test <test@bluemind.loc>
            Blabla`;
        const res = transformer.transform(mailTextContent);
        let expectedHTML = `<blockquote class='forwarded'>-------- Message transféré --------
            Sujet : blabla
            Date : Tue, 19 Mar 2019 18:25:34 +0100
            De : test <test@bluemind.loc>
            Pour : test test <test@bluemind.loc>
            Blabla</blockquote>`;
        expect(res).toContain(expectedHTML);
    });
});
