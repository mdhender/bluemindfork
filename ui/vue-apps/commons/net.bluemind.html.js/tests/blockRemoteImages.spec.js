import { blockRemoteImages, hasRemoteImages, unblockRemoteImages } from "../src/blockRemoteImages";

const htmlWithRemoteImages = `
<html>
    <head></head>
    <body>
        <div>
            Blabla bla <img toto tata src="http://bluemind.net/myfirstImage.png" />
        </div>
        <div>
            Blabla bla blaaaaaaaaaaaaaaaaaaaaaaaa <IMG 
                toto
                tata  SRC =   'http://bluemind.net/mySecondImage.png' />
        </div>
        <div>
            Blabla bla <img toto tata src="blob:https://vm40.bluemind.net/7324a443-db03-4a5f-9dd9-c21251234361" />
        </div>
        <div>
            Blabla bla <img toto tata src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIA" />
        </div>
    </body>
</html>`;

const htmlWithoutRemoteImages = `
<html>
    <head></head>
    <body>
        <div>
            Blabla bla <img toto tata src="blob:https://vm40.bluemind.net/7324a443-db03-4a5f-9dd9-c21251234361" />
        </div>
        <div>
        Blabla bla <img toto tata src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIA" />
        </div>
    </body>
</html>`;

describe("Block/Unblock remote images in HTML.", () => {
    test("Block then unblock HTML with remote images (snapshot test).", () => {
        const blockedImagesHtml = blockRemoteImages(htmlWithRemoteImages);
        expect(blockedImagesHtml).toMatchSnapshot();
        expect(unblockRemoteImages(blockedImagesHtml)).toMatchSnapshot();
    });
    test("Block then unblock HTML without remote images (snapshot test).", () => {
        const unblockedImagesHtml = blockRemoteImages(htmlWithoutRemoteImages);
        expect(unblockedImagesHtml).toMatchSnapshot();
        expect(unblockRemoteImages(unblockedImagesHtml)).toMatchSnapshot();
    });
    test("Contain remote images.", () => {
        expect(hasRemoteImages(htmlWithRemoteImages)).toBe(true);
    });
    test("Do not contain remote images (base64 and blob images should not match).", () => {
        expect(hasRemoteImages(htmlWithoutRemoteImages)).toBe(false);
    });
});
