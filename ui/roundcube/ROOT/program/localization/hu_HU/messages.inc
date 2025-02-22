<?php

/*
 +-----------------------------------------------------------------------+
 | localization/hu_HU/messages.inc                                       |
 |                                                                       |
 | Language file of the Roundcube Webmail client                         |
 | Copyright (C) 2012, The Roundcube Dev Team                            |
 | Licensed under the GNU General Public License                         |
 |                                                                       |
 +-----------------------------------------------------------------------+
 | Author: Németh András <narzym@hotmail.com>                          |
 +-----------------------------------------------------------------------+
*/

$messages = array();
$messages['errortitle'] = 'Hiba történt';
$messages['loginfailed'] = 'Sikertelen bejelentkezés.';
$messages['cookiesdisabled'] = 'A böngésző nem támogatja a sütik használatát';
$messages['sessionerror'] = 'Érvénytelen vagy lejárt munkamenet';
$messages['storageerror'] = 'Nem sikerült csatlakozni az IMAP szerverhez';
$messages['servererror'] = 'Szerverhiba!';
$messages['servererrormsg'] = 'Szerverhiba: $msg';
$messages['dberror'] = 'Adatbázishiba!';
$messages['requesttimedout'] = 'A kérés túllépte az időkorlátot';
$messages['errorreadonly'] = 'A műveletet nem sikerült végrehajtani. A mappa írásvédett.';
$messages['errornoperm'] = 'A műveletet nem sikerült végrehajtani. Hozzáférés megtagadva.';
$messages['invalidrequest'] = 'Érvénytelen kérés! Az adatok nem lettek elmentve.';
$messages['nomessagesfound'] = 'A fiók nem tartalmaz leveleket';
$messages['loggedout'] = 'Sikeres kijelentkezés. Viszontlátásra!';
$messages['mailboxempty'] = 'A fiók üres';
$messages['loading'] = 'Betöltés...';
$messages['uploading'] = 'Fájl feltöltése...';
$messages['uploadingmany'] = 'Fájlok feltöltése...';
$messages['loadingdata'] = 'Az adatok betöltése...';
$messages['checkingmail'] = 'Új üzenetek keresése...';
$messages['sendingmessage'] = 'Az üzenet küldése...';
$messages['messagesent'] = 'Az üzenet elküldve';
$messages['savingmessage'] = 'Az üzenet mentése...';
$messages['messagesaved'] = 'Az üzenet elmentve a Piszkozatokhoz';
$messages['successfullysaved'] = 'A mentés sikerült';
$messages['addedsuccessfully'] = 'A kapcsolat hozzáadása a címjegyzékhez megtörtént';
$messages['contactexists'] = 'Ezzel az e-mail címmel már létezik kapcsolat';
$messages['contactnameexists'] = 'Ezzel a névvel már létezik kapcsolat';
$messages['blockedimages'] = 'Biztonsági okokból a távoli képek letöltése tiltott';
$messages['encryptedmessage'] = 'Az üzenet titkosított, ezért nem megjeleníthető';
$messages['nocontactsfound'] = 'Nem találhatóak kapcsolatok';
$messages['contactnotfound'] = 'A kiválasztott kapcsolat nem található';
$messages['contactsearchonly'] = 'Adjon meg keresőkifejezéseket a kapcsolatok közti kereséshez';
$messages['sendingfailed'] = 'Az üzenet elküldése nem sikerült';
$messages['senttooquickly'] = 'Kérem várjon még $sec másodpercet az üzenet elküldése előtt';
$messages['errorsavingsent'] = 'Hiba történt az elküldött üzenet mentése közben';
$messages['errorsaving'] = 'A mentés során hiba lépett fel';
$messages['errormoving'] = 'Az üzenet(ek)et nem sikerült áthelyezni';
$messages['errormovingreadonly'] = 'Az üzenet(ek)et nem sikerült áthelyezni. A mappa írásvédett.';
$messages['errorcopying'] = 'Az üzenet(ek)et nem sikerült másolni';
$messages['errordeleting'] = 'Az üzenet(ek)et nem sikerült törölni';
$messages['errormarking'] = 'Az üzenet(ek)et nem sikerült megjelölni';
$messages['deletecontactconfirm'] = 'Biztos, hogy törölni szeretné a kijelölt kapcsolato(ka)t?';
$messages['deletegroupconfirm'] = 'Biztos, hogy törölni szeretné a kijelölt csoportot?';
$messages['deletemessagesconfirm'] = 'Biztos, hogy törölni szeretné a kijelölt üzenete(ke)t?';
$messages['deletefolderconfirm'] = 'Biztos, hogy törölni szeretné ezt a mappát?';
$messages['purgefolderconfirm'] = 'Biztos benne, hogy az összes üzenet törölhető?';
$messages['contactdeleting'] = 'Kapcsolat(ok) törlése...';
$messages['groupdeleting'] = 'Csoport törlése...';
$messages['folderdeleting'] = 'Mappa törlése...';
$messages['foldermoving'] = 'Mappa mozgatása...';
$messages['foldersubscribing'] = 'Feliratkozás a mappára...';
$messages['folderunsubscribing'] = 'Leiratkozás a mappáról...';
$messages['formincomplete'] = 'Az űrlap hiányosan lett kitöltve';
$messages['noemailwarning'] = 'Adjon meg egy valós e-mail címet';
$messages['nonamewarning'] = 'Adjon meg egy nevet';
$messages['nopagesizewarning'] = 'Adja meg a papír méretét';
$messages['nosenderwarning'] = 'Kérem adja meg a feladó e-mail címét';
$messages['norecipientwarning'] = 'Legalább egy címzettet adjon meg';
$messages['nosubjectwarning'] = 'A Tárgy mező üres. Szeretné most kitölteni?';
$messages['nobodywarning'] = 'Elküldi az üzenetet üresen?';
$messages['notsentwarning'] = 'Az üzenet még nem lett elküldve. Eldobja az üzenetet?';
$messages['noldapserver'] = 'Adjon meg egy LDAP szervert a kereséshez';
$messages['nosearchname'] = 'Adja meg a kapcsolat nevét vagy e-mail címét';
$messages['notuploadedwarning'] = 'Még nem került feltöltésre minden csatolmány. Kérem várjon vagy állítsa le a feltöltést!';
$messages['searchsuccessful'] = '$nr üzenet található';
$messages['contactsearchsuccessful'] = '$nr találat';
$messages['searchnomatch'] = 'Nincs találat';
$messages['searching'] = 'Keresés...';
$messages['checking'] = 'Ellenőrzés...';
$messages['nospellerrors'] = 'Nem található helyesírási hiba';
$messages['folderdeleted'] = 'A mappa sikeresen törölve';
$messages['foldersubscribed'] = 'Sikerült a mappára feliratkozni';
$messages['folderunsubscribed'] = 'Sikerült a mappáról leiratkozni';
$messages['folderpurged'] = 'Mappa sikeresen törölve';
$messages['folderexpunged'] = 'Mappa sikeresen kiürítve';
$messages['deletedsuccessfully'] = 'Törölve';
$messages['converting'] = 'Formázás eltávolítása az üzenetből...';
$messages['messageopenerror'] = 'A levelek a szerverről nem tölthetők le';
$messages['fileuploaderror'] = 'Feltöltés sikertelen';
$messages['filesizeerror'] = 'A feltöltött fájl mérete meghaladja a maximális $size méretet';
$messages['copysuccess'] = '$nr cím sikeresen másolva';
$messages['copyerror'] = 'Egyetlen cím sem másolható';
$messages['sourceisreadonly'] = 'Ez a címforrás csak olvasható';
$messages['errorsavingcontact'] = 'A kapcsolat címe nem menthető';
$messages['movingmessage'] = 'Üzenet(ek) mozgatása...';
$messages['copyingmessage'] = 'Üzenet(ek) másolása...';
$messages['copyingcontact'] = 'Kapcsolat(ok) másolása...';
$messages['deletingmessage'] = 'Üzenet(ek) törlése....';
$messages['markingmessage'] = 'Üzenet(ek) megjelölése...';
$messages['addingmember'] = 'Kapcsolat(ok) hozzáadása a csoporthoz...';
$messages['removingmember'] = 'Kapcsolat(ok) törlése a csoportból...';
$messages['receiptsent'] = 'Az olvasási visszaigazolás el lett küldve';
$messages['errorsendingreceipt'] = 'Az olvasási visszaigazolást nem sikerült elküldeni';
$messages['deleteidentityconfirm'] = 'Biztosan törli ezt az azonosítót?';
$messages['nodeletelastidentity'] = 'Az azonosító nem törölhető, lennie kell legalább egy azonosítónak!';
$messages['forbiddencharacter'] = 'A mappa neve tiltott karaktert tartalmaz';
$messages['selectimportfile'] = 'Kérjük válassza ki a feltölteni kívánt fájlt';
$messages['addresswriterror'] = 'A kiválasztott címjegyzék nem írható';
$messages['contactaddedtogroup'] = 'A kapcsolat hozzáadása a csoporthoz sikeresen megtörtént';
$messages['contactremovedfromgroup'] = 'A kapcsolat törlése a csoportból sikeresen megtörtént';
$messages['importwait'] = 'Importálás folyamatban, kérem várjon...';
$messages['importerror'] = 'Az importálás sikertelen! A feltöltött állomány nem egy érvényes vCard fájl.';
$messages['importconfirm'] = '<b>Sikeresen importálásra került $inserted kapcsolat, kihagyva $skipped már létező bejegyzés</b>:<p><em>$names</em></p>';
$messages['importconfirmskipped'] = '<b>Kihagyva $skipped már létező bejegyzés</b>';
$messages['opnotpermitted'] = 'A művelet nem megengedett!';
$messages['nofromaddress'] = 'A kiválasztott azonosítónál nincs  email beállítva.';
$messages['editorwarning'] = 'Az egyszerű szöveges formátumra való váltás az összes formázás elvesztésével jár. Biztosan folytatja?';
$messages['httpreceivedencrypterror'] = 'Végzetes konfigurációs hiba történt, azonnal lépjen kapcsolatba az üzemeltetővel. <b>Az üzenet nem küldhető el.</b>';
$messages['smtpconnerror'] = 'SMTP hiba ($code): Sikertelen kapcsolódás a szerverhez';
$messages['smtpautherror'] = 'SMTP hiba ($code): Sikertelen bejelentkezés';
$messages['smtpfromerror'] = 'SMTP hiba ($code): Nem sikerült a feladó beállítása: "$from" ($msg)';
$messages['smtptoerror'] = 'SMTP hiba ($code): Nem sikerült a következő címzett hozzáadása: "$to" ($msg)';
$messages['smtprecipientserror'] = 'SMTP hiba ($code): A címzettlista feldolgozása sikertelen';
$messages['smtperror'] = 'SMTP hiba ($code): $msg';
$messages['emailformaterror'] = 'Helytelen formátumú e-mail cím: $email';
$messages['toomanyrecipients'] = 'Túl sok a címzett. Csökkentse a címzettek számát maximum $max címre!';
$messages['maxgroupmembersreached'] = 'A csoport létszáma meghaladja a maximum $max főt';
$messages['internalerror'] = 'Belső hiba történt, kérjük próbálja újra!';
$messages['contactdelerror'] = 'Hiba a kapcsolat(ok) törlésekor';
$messages['contactdeleted'] = 'Kapcsolat(ok) sikeresen törölve';
$messages['contactrestoreerror'] = 'Nem sikerült a törölt kapcsolat(ok) helyreállítása';
$messages['contactrestored'] = 'Kapcsolat(ok) sikeresen helyreállítva';
$messages['groupdeleted'] = 'Csoport sikeresen törölve';
$messages['grouprenamed'] = 'Csoport sikeresen átnevezve';
$messages['groupcreated'] = 'Csoport sikeresen létrehozva';
$messages['savedsearchdeleted'] = 'Mentett keresés sikeresen törölve';
$messages['savedsearchdeleteerror'] = 'Nem sikerült törölni a mentett keresést';
$messages['savedsearchcreated'] = 'Keresés sikeresen mentve';
$messages['savedsearchcreateerror'] = 'Nem sikerült létrehozni mentett keresést';
$messages['messagedeleted'] = 'Üzenet(ek) sikeresen törölve';
$messages['messagemoved'] = 'Üzenet(ek) sikeresen átmozgatva';
$messages['messagecopied'] = 'Üzenet(ek) sikeresen másolva';
$messages['messagemarked'] = 'Üzenet(ek) sikeresen megjelölve';
$messages['autocompletechars'] = 'Az automatikus kiegészítéshez legalább $min karakter szükséges';
$messages['autocompletemore'] = 'Több egyezés található. Kérem adjon meg további karaktereket!';
$messages['namecannotbeempty'] = 'A név nem lehet üres';
$messages['nametoolong'] = 'A név túl hosszú';
$messages['folderupdated'] = 'Mappa sikeresen frissítve';
$messages['foldercreated'] = 'Mappa sikeresen létrehozva';
$messages['invalidimageformat'] = 'Érvénytelen képformátum';
$messages['mispellingsfound'] = 'Az üzenetben helyesírási hibák találhatók';
$messages['parentnotwritable'] = 'Nem sikerült a mappa létrehozása/mozgatása a kijelölt mappába. Nincs jogosultsága a művelethez!';
$messages['messagetoobig'] = 'Az üzenetrész túl nagy a feldolgozáshoz.';

$messages['quotafulldeletemessagesconfirm'] = 'Quota limit reached. Do you really want to permanently delete the selected message(s)?';
