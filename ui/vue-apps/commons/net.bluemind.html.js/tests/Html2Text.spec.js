import { html2text } from "../src/";

describe('Html2Text', () => {
    
    test('Html2Text transformations', () => {
        expect(html2text(`<a href="https://url.com/resources">Mon beau lien</a>`))
            .toEqual("Mon beau lien [https://url.com/resources]");

        expect(html2text(`<h1>Mon titre</h1>`)).toEqual("MON TITRE");

        expect(html2text(`<img src="/resources/img/3">`)).toEqual("");

        expect(html2text(`<a href="mailto:contact@cmsday.fr" 
            style="font-size: 12px; text-decoration: none; color: #FFF; text-decoration: underline;" 
            onclick="return rcmail.command('plugin.composenewwindow','contact@cmsday.fr',this)">contact@cmsday.fr</a>`))
            .toEqual("contact@cmsday.fr [mailto:contact@cmsday.fr]");
    });
    
});
