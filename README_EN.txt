======================================================================
 NeReO 1.5.7 - Ne(ws) Re(ader) O(pen-source)
 Text-only Usenet (NNTP) newsreader for Android
 NeReO 1.5.7 (versionCode 175) - By MCSM - 2026/10/22
======================================================================
 ***  VERSION 1.5.7  ***

 A week of requests: users, testers and Marco.

  * THE FADE BETWEEN SCREENS CAN BE SWITCHED OFF. In Appearance, "Screen transition animation" is on by default and behaves as before; turned off, messages and screens open instantly, without the short flash.

  * FILTERS AGAINST THOSE WHO CREATE A NEW IDENTITY FOR EVERY POST. The match type "nick = address", for the "from" field, fires when the nickname is the e-mail address split in two (Coltivatore Diretto <colti@vatorediret.to>) and works before downloading. The new field "Headers" searches all the headers of a message. Through the score, several weak clues add up: none hides on its own, together they do.

  * SEE WHO FEEDS A TROLL. A rule that hides an author and is extended to replies can now HIGHLIGHT the replies instead of hiding them: the author disappears and you see who keeps answering.

  * THE VOLUME KEYS CAN TURN THE PAGE. In Appearance there is a new switch, off by default: when it is on, in full-screen reading volume down opens the next message and volume up the previous one. Everywhere else the keys change the volume as usual.

  * SAFER AGAINST MALFORMED MESSAGES: filters extended to replies now read the References line with the same safety limit as the rest of the app.

  * QUOTATION MARKS ARE SHOWN AGAIN. In some fifty texts of the app, words meant to appear in quotes were shown without them, because of how those texts were written. Fixed everywhere, with a check that prevents it from coming back.

 ***  VERSION 1.5.6  ***

 A report from Roxy on his TV box, the day after 1.5.5.

  * NO MORE CRASHES ON TV BOXES WITHOUT A FILE PICKER. Many Android TV boxes lack the system screen for choosing where to save or what to open, and NeReO closed instantly when it tried to show it (for instance with "Export configuration"). Now it says so instead, in all nine places that use it. Exporting and restoring the configuration work anyway: without a picker the backup is saved in NeReO's folder (Android/data/org.openusenet.reader/files, the path is shown) and restoring lists the backups found there. Copy the backup somewhere else: uninstalling NeReO deletes that folder.

 ***  VERSION 1.5.5  ***

 Three entries from the to-do notebook: a known defect and two small confusions met by users. Plus a new look for the author of a message, and the themed icon; and a fix to 1.5.4 reported by a user.

  * THE AUTHOR OF A MESSAGE HAS A NEW LOOK. Next to the name, their Face or, when there is none, their initials on a coloured circle: the colour comes from the address, so the same person always has the same colour, in every group. The date says "today", "yesterday" or the day of the week for recent messages (tap it for the full date). In the panels too, where the Face was missing until now.

  * REPLYING IN VERY DEEP THREADS WORKS AGAIN. When a message carried a very long References line (one in nl.politiek had 27 message-ids), the reply went past the 998-character limit of a header line and the server refused it. Now NeReO removes duplicate ids and, above the limit, drops the oldest ones after the first, keeping the start of the thread and the most recent messages (RFC 5537). Below the limit nothing changes.

  * X-FACES ARE SHOWN. The classic black-and-white Usenet face (the X-Face header, sent by MesNews and many traditional newsreaders) now appears next to the author's name, where until now only the colour Face did. Face first, then X-Face, then the coloured initials. Tap it to enlarge it: it stays sharp. NeReO shows X-Faces but does not send them.

  * NEW MESSAGES REALLY STAY IN VIEW. The 1.5.4 fix worked only if NeReO had just taken you back to where you left off: on the first visit to a group after starting the app, or with the list already at the top, messages arriving with the sync could still end up above the edge of the screen. Now, every time you open a group, if new messages arrive and you have not yet moved the list with your finger, it goes to the top to show them. With the "newest first" orders it also notices a reply in the thread already at the top; with the "oldest first" orders, where new messages are at the bottom, you stay where you are. In the panel view too.

  * THEMED ICON: on Android 13 and later, with themed icons switched on, NeReO's icon takes the colours of your theme too.

  * SEARCH TELLS YOU WHERE IT FOUND THE WORD. When the word is only in the text of the message, the result shows a short excerpt with the word highlighted in yellow. Before, such a result had no highlight at all and looked wrong.

  * THE SERVER'S 441 REFUSAL EXPLAINED. The server's reply, kept whole, is now followed by a line in your language. For the most common case, "Article must contain real content", it says what to do: write a little more.

  * IN THE PANEL VIEW THE MESSAGE FIELD IS NO LONGER SQUEEZED: the writing area scrolls and the field keeps a usable height, as in the page view since 1.4.9.

  * THE NAME IS RESERVED. The code stays GPL, but modified versions must use a different name and a different applicationId: see TRADEMARK.txt.

 ***  VERSION 1.5.4  ***

 Three requests from users, received by Marco the day after 1.5.3.

  * "QUOTE AND REPLY". Select some text in a message and the menu offers, next to "Quote", a command that opens the reply with that quotation already in place, the cursor below it and your signature at the end. Before, it took five steps.

  * OPENING A GROUP, NEW MESSAGES ARE NO LONGER MISSED. The list went back to where you had left it before the new messages arrived on top, and reopening the group always went back there. Now, if new messages arrive and you have not touched the list yet, it goes to the top to show them.

  * UPDATE CHECK "DAILY" now checks at the first opening of each new day, and in any case after 24 hours. Before, it waited 24 hours from the last check, and a new version could be noticed up to a day late.

 ***  VERSION 1.5.3  ***

 From Marco's testing: a rule that is removed must take away what it did.

  * REMOVING A RULE UNDOES ITS EFFECT. Deleting or editing an "Ignore" rule brings the messages it hid back into view WITHOUT DOWNLOADING THEM AGAIN; removing a "Highlight" rule takes the colour away; removing a "Follow the thread" rule takes away star and colour - unless another active rule still hides, highlights or follows them. "Apply now" now works both ways. Stars you set by hand stay. Limits: a message you starred by hand that also matched the rule loses its star; authors hidden by the quick plonk before 1.5.3 stay hidden.

  * THE DIAGNOSTIC LOG NOW TELLS WHAT FILTERS DO: "Filtri:" lines for every rule saved or deleted and every "Apply now", with how many messages came back into view, lost a star or changed score.

 ***  VERSION 1.5.2  ***

 No new features: the app is the same, but much lighter.

  * THE APK SLIMS DOWN. NeReO used to carry the whole Material icon library - more than ELEVEN THOUSAND icons, over half of the compiled code - and uses 75 of them. Now it carries only those, copied into the sources under the same names and verified identical one by one (shapes, colours, mirroring for right-to-left languages). Nothing changes on screen.

 ***  VERSION 1.5.1  ***

 First closing release: it protects the text you write, and withdraws a promise that does not need keeping.

  * DRAFTS SAVE THEMSELVES. A second and a half after the last change, and again when you leave the screen or the app goes to the background. Leaving without tapping "Save draft" no longer loses your text. A reply opened and left untouched does NOT become a draft; after a successful send the draft does NOT come back; if sending fails, the draft protects the text again.

  * "DISCARD" in the bottom bar of the writing screen: throws the text away and deletes the draft, after a confirmation. With automatic saving it is the way to leave without a trace.

  * FILTERS: PROPAGATION TO REPLIES FOLLOWS THE RULE'S ACTION (versionCode 163). "Extend to replies" on a "Follow the thread" or "Highlight" rule used to HIDE the replies - the very branches you wanted to see: now it follows them with a star or highlights them. "Apply now" also stars already downloaded messages matching a "Follow the thread" rule. For rules that hide, behaviour is IDENTICAL. Revoking a rule that highlighted removes the colour; stars stay, because they may have been set by hand.

  * THE LINE-BY-LINE SCREEN FOR REMOTE CONTROLS, announced in 1.5, is not coming for now. "Quote" works with text selection; the screen will be made if someone really needs it.

 ***  VERSION 1.5  ***

 This opens a long cycle: the QUOTING PACKAGE, asked for by Ammammata and widened by Bingo3331. This first stone is invisible, and we say so rather than keeping quiet.

  * "QUOTE" IN THE SELECTION MENU (versionCode 158). Select part of a message and the menu that appears above the selection offers "Quote": the clipboard receives a complete quotation - your attribution line, the lines with "> " in front and, at the end, the Message-ID with its permanent link. It always quotes the ORIGINAL LINES, WHOLE: the text on screen is reworked (bars instead of ">", level numbers, flowed paragraphs joined), so the selection only says WHICH lines, and what was already quoted keeps its levels. In full-screen reading and, from versionCode 161, in the panel view too; a line-by-line screen for remote controls comes in a later step.

  * "QUOTE" INSIDE A QUOTATION NAMES WHO REALLY WROTE IT (versionCode 159). If the chosen lines were already quoted in the message - say Giangi's sentence inside Roberto's reply - the quotation no longer starts with "Roberto wrote:": it uses the attribution line that introduces those lines in the message ("On 05/10, Giangi wrote:") and drops the extra levels. If there is no recognisable attribution, it behaves as before.

  * THE ITALIAN README IS NOW CALLED README_IT.txt (versionCode 160), next to README_EN.txt: one name, the same inside the source zip and in the SourceForge folder.

  * THE QUOTING ENGINE HAS BEEN SPLIT IN TWO. On one side a VALUE describing a quotation - who wrote it, where, when, under what subject, the Message-ID with its permanent link, and the lines you picked; on the other a FUNCTION that turns that value into text. This is not a matter of style: the collector that comes later will have to keep a LIST of quotations and format them all in a row, and if capturing wrote a string straight to the clipboard the collector would have to reopen and dismantle it. That is exactly how the sixth copy of a piece of logic was born in this project before.

  * THE QUOTATION HEADER IS NOT INVENTED HERE: it is your own reply introduction, with all its placeholders including %subject%. One template in the whole app - anyone who customised theirs finds it here too.

  * THE LINE THAT PUTS "> " IN FRONT OF QUOTED TEXT now lives in one place, instead of inside the function that prepares replies. Behaviour identical, verified line by line by a test bench.

  * TESTED BEFORE IT WAS EVER COMPILED. The new file is pure Kotlin, without a single import: it compiles on its own with kotlinc and runs 21 assertions against the real code before the APK exists. In 1.4.11 two defects out of two were found by execution and none by reading - this time the bench is born with the code instead of after a failure.

 ***  VERSION 1.4.11  ***

 A single entry, asked for by ObiWan. It states its own limit, because the RFC that describes it states it too.

  * IF A SERVER USED TO OFFER STARTTLS AND STOPS ADVERTISING IT, NeReO NOW TELLS YOU. STARTTLS is the upgrade from a plaintext connection to an encrypted one: the server advertises it among its capabilities, and anyone sitting in the middle of your connection can delete that single line from the reply - a reader that knows no better simply never tries to encrypt, and everything looks normal. NeReO now remembers which servers offer it, and marks, next to the server name in the server list, the one that used to offer it and no longer does. This is exactly the defence RFC 4642 section 5 suggests, in its own words: "recording the fact" and "generating an alarm".

  * AND THE LIMIT, SAID UP FRONT: the RFC writes "partially protect", and that "partially" is not a nuance. The comparison needs an earlier CLEAN connection: if someone is already in the middle the very first time you talk to a server, this mechanism never sees them. It protects against the DOWNGRADE of a server already seen encrypted, not against the first encounter.

  * IT STAYS SILENT IN THE TWO CASES WHERE IT WOULD CRY WOLF. If the server does not answer the capability question at all, the list arrives empty, and an empty list does not mean "STARTTLS removed": nothing is reported. And on servers already encrypted from the first byte (implicit TLS, usually port 563) nothing is reported either, because they have no reason to advertise an upgrade they do not need - otherwise every encrypted server would become a permanent alarm.

  * NO EXTRA TRAFFIC, NO NEW SETTING. The data was already in hand: NeReO has read the capabilities on every connection, BEFORE attempting STARTTLS, since 1.3. The memory is keyed by ADDRESS AND PORT rather than by server table row, because restoring a backup gives servers new identifiers and a memory keyed by identifier would have landed on the wrong server - a false alarm is worse than silence.

 ***  VERSION 1.4.10  ***

 Five entries: three MAINTENANCE ones nobody asked for, and two reported by users. We say so rather than keeping quiet, because anyone who opens the release notes and finds nothing new deserves to know why the number changed.

  * IN THE NEWSGROUP LIST, A LONG GROUP NAME NOW SHORTENS instead of being cut off. It drops the middle of the hierarchy rather than chopping the end with an ellipsis, and only when it genuinely does not fit; short names are untouched, and groups with unread messages stay in bold. The shared component that does this was already used about sixty lines further down in the same screen: only this one spot had been left out, and for a technical reason rather than a product one - bold was needed for unread groups and the component has no font-weight parameter. It did not need one: it already takes a text style, and bold is an attribute of the style.

  * UNDER THE BONNET, BEHAVIOUR IDENTICAL. The deprecated fallbackToDestructiveMigration() call is replaced by the equivalent Room itself prescribes in its own ReplaceWith; and the two files that handle line wrapping and Format=Flowed MOVE into a "text" package of their own, closing the debt declared in 1.4.8 - they were sitting among the screens, and the data layer was reaching up into them. The move is verified by the test bench that compiles those two REAL files with kotlinc: it passes every assertion at the new path.

  * A NOTE FOR THOSE WATCHING THE NUMBERS: 1.4.10 is the project's first two-digit version segment. The update check compares versions segment by segment AS NUMBERS, so 1.4.10 ranks later than 1.4.9 - verified before adopting it, because a textual comparison would have said the opposite and would have silently stopped seeing new releases.
  * THE ACTION PANEL, opened by holding a message pressed, now scrolls. When the thread contained messages of your own, one extra entry appeared and the last actions - among them "Withdraw from server" - were left cut off the bottom with no way to reach them. It was not getting stuck: it simply did not scroll. The same panel in the panel view had the same fault and is fixed too, because a fix applied to one copy out of two is how this project has already hurt itself once. (Reported by Macchia, who worked out the condition himself)
  * THE "+" NOW OPENS THE DISCUSSION even when its replies have already been read. In the grouped-threads view, with "unread only" switched on, a discussion whose replies were all read still showed the "+" and the count of its replies next to it - but tapping it revealed nothing. The filter decides what the LIST shows; tapping the "+" is an explicit request to see ONE discussion, and the explicit gesture now wins over the general preference. Reported in five words by a user who thought he was doing something wrong: he was not. A control you can press that does nothing is worse than a control that is not there.

 ***  VERSION 1.4.9  ***

 Three entries. The first two are from Jeff, one that TAKES AWAY and one that ADDS; the third comes from a user with a phone no tester has. The first makes it possible to switch off an ornament he had mistaken for another one; the second answers a one-line question of his. The second entry also states its own downside in the body of the text: NeReO's search does not see a kept message. People keep a message in order to find it again, and a note that kept quiet about that limit would be selling a different feature from the one that is there.

  * A SWITCH FOR THE VERTICAL INDENT RAILS (Appearance, right below the quote style). These are the grey lines that mark one level of depth each in the lists, switched on everywhere since 1.4.7. Jeff had chosen the "marks only" quote style and still saw vertical lines: it was NOT a regression - the drawing of the message body is identical byte for byte since 1.4.6 - they were the rails, that is, a different ornament that looks alike. The clue that tells them apart was in front of him: quote bars take THE COLOUR OF THEIR LEVEL, the rails are all one grey. A clue visible to everyone is not a clue noticed by anyone. So it is not fixed, it is made optional, and ON by default, because those rails were asked for by Dave Royal. The switch sits below the quote style because that is where Jeff looked for it. Switching the rails off leaves the indentation in place - only the lines go. (Reported by Jeff)

  * "KEEP THIS MESSAGE". A new action in the reading bar, and in the three-dot menu in panel view: it writes the message as an .eml file into a folder you choose ONCE (Maintenance -> Kept messages). After that, keeping a message is one tap. "Save as .eml file" stays where it was and still asks you where to put the file every time: that is the right shape for the occasional hand-off to a mail client, and it is also the reason why nobody used it to put one message after another aside. A kept message LEAVES THE LOCAL STORE and becomes a file of yours: the automatic clean-up will not take it away, nor will unsubscribing, nor a re-download. In exchange, NEREO'S SEARCH NO LONGER SEES IT - the search reads the local store, and that message has left it; to read it again, open the file with another app. It is deliberately not called an "archive": the name is the promise, and an archive promises a list, a search and permanence, which are three things this feature does not give. It comes from a question by Jeff, "does NeReO store posts?": the answer was no, and now it is "if you ask it to". (From a question by Jeff)

  * THE MESSAGE FIELD IS NO LONGER SQUEEZED FLAT. On a small screen, with large text and a tall keyboard, the letters you had just typed appeared CUT IN HALF. Reported by a user on a Samsung J6, and reproduced here by turning the text size up and opening "Sender and headers". The keyboard was not covering the field - the bar with "Send" was in its proper place above it: the field was simply being given almost no height, because the writing form could not scroll and the field was taking "whatever is left". A minimum height was already in the code, with a comment next to it claiming it was GUARANTEED; it was not, because a minimum cannot ask for more than the maximum it is given. The form now scrolls and the minimum is a minimum. The same user, with a lower keyboard, could write: the keyboard was not the cause, it was the condition that exposed the defect.

 ***  VERSION 1.4.8  ***

 Seven entries: two about filters, three from BIG Umberto about the "search -> open -> see where it sits" path, which is the feature he himself asked for in 1.4.6 - he is testing his own request and has found its three holes. The sixth one is outside the app, and the seventh is the only change in this version that affects WHAT NEREO SENDS - which is why it ships switched off. The second entry says what this release does NOT do: the "Apply now" command covered two actions out of four, this version adds one of the two missing ones, and keeping quiet about the other would suggest a feature that is not there.

  * "APPLY NOW" WORKS FOR RULES THAT ADD AS WELL. A "Highlight" rule, or a score typed by hand in advanced mode, used to apply only to messages arriving afterwards: nothing happened to the ones already downloaded. The cause was not a filter evaluating the wrong way - the engine was computing the right score and the function that applies rules to already downloaded messages WAS THROWING IT AWAY, because it only saved it in the branch that serves to hide. And in NeReO "highlighted" is not a flag of its own: it is the score that the thread list reads in order to colour the row. No new column, no migration: a write was missing. The closing summary now has a third figure next to deleted and hidden. (Reported by Cercaparole on Usenet)

  * A STATED LIMIT, FOR THE HALF THAT IS MISSING. A "Follow thread" rule applied to already downloaded messages now sets the score - the row is coloured - but it does NOT yet grant the star of followed threads; and the "extend to replies" box still HIDES branches whatever the rule's action is, even when that action is "follow". These are writes that touch many rows of the database and they are tested one at a time. Messages already hidden and threads you follow are not touched, as before.

  * THE TARGET NOW WORKS ON REPLIES TOO. From the search results, "Show in the list" took you back to the thread only if the message was the FIRST POST; in the grouped-threads view a reply was never highlighted. The tree and sequential views were fine, and it was not a difference between views: in the grouped view a closed thread is A SINGLE ROW, so a reply needs the thread to be opened first - and that was the step being skipped, because it was attempted while the message list had not yet arrived from the database. (Reported by BIG Umberto)

  * IN THE SEARCH, THE UNDERSCORE AND THE PERCENT SIGN NOW SEARCH FOR THEMSELVES. They were database wildcards: searching for _114 meant "any single character, then 114", and the results included messages with no underscore in them at all. The giveaway: those rows showed no yellow highlight, because the subject did not really contain what you had typed. Worth remembering that the search looks at SUBJECT, AUTHOR AND BODY of downloaded messages, so a result with no yellow in its subject may have matched inside the text. (Reported by BIG Umberto)

  * THE BACK ARROW NO LONGER RETRACES THE WHOLE WAY. After "Show in the list" the list used to open ON TOP of the message you came from, so going back made it reappear before you got where you wanted. The message now steps aside and the arrow takes you back to the SEARCH RESULTS, from which you can jump to the next one. (Reported by BIG Umberto)

  * OUTSIDE THE APP: THE VERSION HISTORY AT THE END OF THIS FILE. It was A SINGLE LINE of nearly four thousand characters, with every entry from 1.2.7 down to beta1 pasted one after another - a table read as a paragraph by a reformatting done in late September. And it started again from 1.2.7, that is, it repeated in short form SEVEN VERSIONS ALREADY DESCRIBED IN FULL above. It is now one line per entry and starts at 1.1, the first version the sections above do not cover. From the next release, moreover, the Italian README is uploaded already extracted into the SourceForge folder, next to this one. (Both reported by il Merovingio)

  * FORMAT=FLOWED ON THE WAY OUT, OFF BY DEFAULT (Maintenance -> Layout of the messages you send). Switched on, long paragraphs leave with the SOFT break of RFC 3676 - a space at the end of the lines NeReO breaks - instead of a hard wrap at 72 columns: readers whose program knows the format see the paragraph re-joined to the width of their own screen, even on a narrow phone, and readers whose program does not see lines at 72 columns as always. The signature is left alone, and the space after the quote mark stays where it is - the standard allows it "to maintain the relative indentation of the lines when viewed by clients which are not aware of Format=Flowed". IT IS THE ONLY SETTING THAT CHANGES WHAT LEAVES NEREO, and a published message cannot be withdrawn: that is why it ships switched off, and why the format is declared only when there is actually something to re-join. NeReO has been able to READ f=f since 1.4.6, so it can check its own output.

 ***  VERSION 1.4.7  ***

 Six entries, five of them from two testers. They all have the same shape: something that existed for one screen and not for its sibling. This is the release of the SEAMS - the places where two parts of the app, written at different times, had stopped resembling each other without anyone deciding so.

  * THE VERTICAL RAILS OF THE TREE now appear in all three views: the message list on the phone, the thread tree and the panel layout. They used to be in the panel layout only. Dave Royal asked whether that was by design: it was not. The same indentation formula was written in THREE different files and only one of them also drew the rails, so the difference had never been decided by anyone - it was a copy left behind. Unified into one place and switched on everywhere in two separate builds: first the extraction with the rendering unchanged, then the new look, so that a fault would have had a single cause to look for. (Reported by Dave Royal)

  * THAT SCREEN IS NOW CALLED "thread tree", in both languages. It used to be called "conversation tree" while the rest of the app says "thread" everywhere, and the screen contradicted itself: its title said conversation and its error message said thread. This is not a matter of style: a conversation is a set of messages ESTIMATED from subject lines, which is what mail clients do; a thread is a GRAPH OF DECLARED REFERENCES, which is what NeReO actually builds. (Reported by Dave Royal)

  * APPEARANCE NOW HAS A SWITCH for showing unread messages only. The setting always existed and ships switched on, but it could be changed only from the envelope icon above a list, and nowhere in Settings. A tester on his first day looked where the setting ought to be, did not find it, and concluded that the app was DELETING the messages he had read. Nothing was ever deleted: the switch simply was not where anyone would look for it. (Reported by Jeff)

  * RE-DOWNLOADING A GROUP can now be started from the newsgroup list too, by holding a group pressed. It was only available from the menu inside a group, while everything else of that family - download bodies, unsubscribe, refresh all groups of this server - already lived in the list. The command was the thing out of place, not the place it was looked for. (Reported by Jeff)

  * THE RE-DOWNLOAD DIALOG now says how many messages will actually come back. Re-downloading does NOT restore: it fetches again as many headers as your sync settings allow, and anything older you had locally is lost for good. A group with 415 messages came back with 298, because the setting was 300. The dialog warned about read state, flags and bodies, but not about ending up with FEWER messages than before. The new warning carries the real number from your own configuration.

  * THE NEWSGROUP LIST REMEMBERS WHERE YOU WERE. Scrolling down to look for groups with new messages, opening one and coming back used to put you at the top again: with dozens of groups across two servers that meant scrolling all the way down every time. It records not the row but WHAT was at the top, so unsubscribing from a group does not send the position somewhere random. (Reported by Jeff)

 ***  VERSION 1.4.6  ***

 Nine entries, and the drawer is empty. The oldest open defect of the project - honouring Followup-To, reported by Dave Royal on 21 September - is closed here. Six of the nine defects were found by Marco during testing, after he had been told everything was fine.

  * FOLLOWUP-TO is no longer thrown away by opening the group selector. When the author asks for follow-ups to continue elsewhere, the selector now starts from the groups HE named rather than from the one you are reading, and it lists them even if you are not subscribed: someone else chose them, so you should not have to subscribe in order to reply where you were asked to.

    A destination such as "uk.test only", which simply could not be expressed before, is now what you get by confirming without adding anything. And opening the selector just to LOOK no longer throws the request away: that was the worst case, because the act of checking destroyed the very thing being checked.

    Finally, the notice at the top of the compose screen and the line saying where the message will go now read the same value: before, they could contradict each other one line apart. (Reported by Dave Royal, with tests 9A and 9B.)

  * THE PANEL LAYOUT gains the navigation commands that until now existed only in the page layout: previous and next message, first unread, previous and next discussion, start and end of the message, "reload". Reaching the next message meant scrolling the list and tapping - and with a remote control that scrolling is the hard part of the day.

    The new entries start out in the menu, so that nobody who has already arranged their toolbar finds it changed underneath them; two taps in the toolbar editor move them in among the icons. A command that cannot be used in a given pane is shown greyed out rather than doing nothing. (It comes from the way Roxy reads, on a TV box.)

  * THE SUBJECT LINE no longer collapses into a column of single letters. In the panel layout on a narrow pane the header and the toolbar were competing for the same row: the toolbar was measured first and took what it asked for, while the text was given whatever was left - and on a pane narrower than the toolbar, what was left was nothing.

    The point was never that the toolbar is long; it is that the one thing you have to be able to READ was always the one that paid. Below a certain width the toolbar now moves under the header, and wraps rather than having its last icon cut off - and that icon was the menu button, the way to reach everything else. (Found by Marco on the setup no tester has: the panel layout on a phone held sideways.)

  * NeReO READS FORMAT=FLOWED (RFC 3676), the way almost every modern newsreader states where a paragraph really ends. Until yesterday NeReO guessed: it estimated the layout width and joined the lines that looked like they belonged together. It worked nearly always, and "always" and "nearly always" are two different things.

    Now, when the sender declares it and actually uses it, nothing is estimated: the lines that continue were marked one by one by the person who wrote them. For messages that do not declare it, nothing changes.

  * "SHOW IN THE LIST", from a message you are reading: it opens the group list, expands the discussion holding it and highlights it. It comes from one exact sentence - "search works, but it opens the post, it does not show you where it is" - and that was right to the letter: from a result you landed inside the message with no idea which discussion it belonged to or what surrounded it. (Reported by BIG Umberto.)

  * THE LABEL AT THE TOP of the compose screen now says where the message will go, not the group you were reading. With a Followup-To to a single group the two had stopped coinciding, and the screen stated something false one line away from the notice stating the truth. (Test 9A by Dave Royal.)

  * IN THE PANEL LAYOUT, your choice of reply groups is no longer discarded. The dialogue opened, you chose, and an instant later the author's request silently overrode it: the message went where the header said, not where you had sent it. It was not a confusing dialogue: it was a user decision thrown away. (Test 9B by Dave Royal.)

 ***  VERSION 1.4.5  ***

 Six entries. Two had been left behind - parked in a list that was later replaced, and never carried over into the new one - and one comes from a report that looked cosmetic and was not.

  * FOLLOWING A THREAD now follows it forward, not just backwards. Until now "watch this thread" marked the messages that existed at the moment you tapped it: replies arriving afterwards were born unwatched, which is the opposite of what you asked for.

    And it was not only the missing star. The watched mark also keeps a message safe from filters and from automatic clean-up, so the newer messages of a thread you were following were LESS protected than the old ones, and a followed thread could quietly hollow out from the middle.

    New replies now inherit the mark from any watched ancestor, at any depth, so it survives even when a message in the middle of the chain was never downloaded.

    This applies to messages arriving from now on. Replies already in your archive keep whatever mark they had: to bring an old thread up to date, unwatch it and watch it again. (reported by BIG Umberto, who called it "only cosmetic" - it was not)

  * A THANK-YOU TO DAVE ROYAL, and it is not a formality. Since 1.4 most of what has been fixed in NeReO started with one of his reports: the Followup-To handling, the refresh menu, the three-dot menus that opened in the wrong place, "mark all as read" in the panel layout, replies to crossposts on the phone.

    What makes them worth so much is not the number. He tells us where the defect is NOT, which saves half an investigation; he corrects us when we misread him instead of letting it slide; he accepts a trade-off and says which one; he sends designs, not just complaints; and he keeps prepared test cases on uk.test and it.test so the next fix can be checked against the same ground.

  * "MARK ALL AS READ" is back in the toolbar of the panel layout, where it costs one tap again instead of two. It was removed in 1.4.1 for a good reason - it sat right next to the unread filter and got pressed by accident - but the paged layout got a better cure for the same problem: there the icon was MOVED away from the filter rather than taken out. The panel layout now gets that same cure, in the same position, and the menu entry is gone: one way in each layout, and it is the same way. "Mark all as unread" stays in the menu, because that one really is rare. (reported, twice and about two different things, by Dave Royal)

  * "ALWAYS REPLY TO ALL GROUPS" now works on the phone too. The setting has existed since 1.2.7, but it was only ever read when the crosspost dialog opened - and in the paged layout replying goes straight to the writing screen without that dialog. So it was on and did nothing: the screen showed only the group you had been reading, until you opened the group selector by hand. A reply to a crossposted article now starts with all of the original's groups, up to the cap. If the author asked for followups elsewhere, that request still wins. (reported by Dave Royal)

  * AN ARTICLE CAN BE SAVED AS AN .eml FILE, which an email client opens as a message instead of a wall of pasted text. It is the other half of a request from 1.3.6: back then the answer was sharing the raw article, which covers reading it elsewhere but not keeping it. The file is written with its encoding headers rewritten to match what is actually inside it: NeReO stores message bodies already decoded, so copying the original Content-Type across would have made the file lie about itself and every accented character would have come out wrong. It is a bar action like the others, so you can move it into the menu or remove it in Appearance -> Customise bars. (requested by Adam H. Kerman)

  * THE QUOTING INTRODUCTION can now carry the subject of the message you are quoting, with %subject% (or %oggetto%). It joins the author, email, date, time, group and Message-ID placeholders that were already there, and it works in ordinary replies too, not only when quoting across groups. (requested by Ammammata)

 ***  VERSION 1.4.4  ***

 Five entries: three close reports from Roxy - two of which were never "it doesn't work", they were "it isn't there" - and two come from Dave Royal.

  * The PASTE button is now in the writing panel of the PANEL layout too, where it had never been. Everything done for that button - asked for in 1.3.3, made always visible in 1.4.1, and taught in build 106 to read the clipboard on the click rather than while the bar is being drawn - had been done to the paged view only, because the panel layout had no such button at all to fix. (reported over and over by Roxy, who was right every time)

  * "WITHDRAW FROM THE SERVER (cancel)" is now in the context menu of the panel layout as well. It was left out on purpose in 1.4.2: it carries three dialogs - confirmation, waiting, per-server outcome - and copying them would have been the very defect this release is about. They have been moved into one shared place instead, and both views now run the same code. It still appears only on your own messages, and it still sends a control message onto Usenet. (reported by Roxy)

  * With a SINGLE server configured, the refresh icon no longer asks which servers you want to refresh - it just refreshes. The menu still appears when you have more than one, where the choice means something. Until now it opened regardless, offering two entries that did the same thing and a dialog where you ticked the one server you own: not a choice, a formality. (reported by Dave Royal)

  * THE THREE-DOT MENU now opens underneath the icon you tapped, on the thread list of the paged layout too. There it appeared at the far left edge of the screen: on a tablet, a hand's width away from the icon. The cause: those actions sit in a full-width row of their own, and a menu with nothing around it hangs off whatever contains it, so it was measuring itself against the whole row. In the narrow columns of the panel layout the same defect is invisible, which is why it went unreported for so long. A census found twelve icon-and-menu pairs and eight without an anchor of their own: all of them have one now, not just the one that was found. (reported by Dave Royal)

  * GROUPS WITH THE SAME NAME on different servers can be told apart again. In the PANEL layout the group column was one flat list that never said which server a group came from - and it was not even sorted by server, so groups from two servers came out interleaved and two identical names could land side by side. It now has a heading per server, like the paged view always had. And in BOTH layouts the bar above the thread list names the server under the group name: until now, once you were inside a group, nothing told you which of the two you were reading. With a single server configured nothing changes, because there is nothing to tell apart.

    Two smaller things came with it. The heading now shows the name you gave the server rather than its host: that is the only one that differs when you have two accounts on the same provider. And groups left behind by a deleted server no longer vanish from the list - they gather at the end under "?", where the database cleanup can find them. (reported by Roxy)

 ***  VERSION 1.4.3  ***

 Three fixes, all three born from a single message from a tester who had turned the diagnostic log on.

  * Lists no longer go DEAF. It could happen - almost always on a large group, and right after a refresh - that the open list stopped showing new messages: the group badge counted them, the database held them, but the list stayed put until you left the group and came back in. Underneath there was a database read that failed, and a defence written badly around it: instead of starting to listen again, the list shut down for good. It now repairs itself within an instant. Without the diagnostic log switched on and sent in by the person who kept hitting this, it would still be a suspicion instead of a fix. (reported and diagnosed by BIG Umberto)

  * The Cancel button of the LISTGROUP diagnostic works. It was disabled for as long as the operation was running, and on a group with a hundred and seventy thousand articles that wait is over a minute and a half: the only way out was to force-close the app. It now really interrupts, and whatever had already been recorded stays in the log. (reported by BIG Umberto)

  * The rows orphaned by a server deleted with 1.4.1 or earlier can finally be removed. 1.4.2 only counted them, at the end of the Database diagnostics report: the measurement has arrived - three groups and 99,906 catalogue rows with no server, in a 36 MB database - and with a real number in front of it the cleanup stopped being an idea of mine. The button sits right under the report, and before deleting anything it tells you how many rows are going, item by item. (measured by BIG Umberto)

 ***  VERSION 1.4.2  ***

 Seven fixes. No new features.

  * "Reload message" now really reloads. In 1.4 and 1.4.1 that button on the reading bar DELETED the article instead of fetching it again: it vanished from the list along with its read state, its "watched" flag, its filter score and the marker for replies to you, and it did not come back even on a group refresh, because a refresh resumes from the last article already downloaded. The cause was the name of a function: eliminaCopiaLocale sounded like "drop the cached copy" and actually did DELETE FROM articles. The button now clears ONLY the message text and asks the server for it again, and that function is called eliminaArticoloLocale, which is what it does. (reported on the group, with a technical analysis attached)

  * Your own posts come back as unread like any other message. 1.4.1 made them arrive already read: it seemed convenient and it is not, because after posting you want to FIND your own message again, to see how it came out and where it landed in the thread, and with the "unread only" filter on it simply was not there. No other reader behaves like this: not PAN, not Thunderbird, not Piao Hong, not MesNews. The rule was also wider than it looked: on the first sync of a group it marked your WHOLE history there as read. (reported by Dave Royal)

  * "Mark all read" is no longer hit by mistake. A rare action with consequences was sitting next to a frequent, harmless one, the unread filter. On a PHONE it swaps places with "Refresh"; in the PANEL view, where the toolbar is denser and the same action was already in the three-dot menu, the toolbar icon has been REMOVED: it was a duplicate, and dropping it makes the accidental tap impossible rather than merely less likely. The two layouts now show the icons in the same order. (reported by Dave Royal)

  * Crossposting now works when you start a NEW thread from the panel view too. The group picker only appeared when replying to an already-crossposted message: starting a thread from scratch it was missing, while the paged view always offers it. It is the same shape of defect that 1.4 fixed here for REPLIES, left uncovered on the other branch of the same if. (reported by Dave Royal)

  * "Supersede" now also appears in the context menu of the PANEL view, where it had never been: it only existed in the paged view. As before it shows only on your own messages. "Withdraw from the server (cancel)" stays in the paged view for now: that part has to be reorganised first so there are not two copies of it, and it sends control messages onto the network, so it deserves a test round of its own. (reported by Roxy)

  * Deleting a server now really takes away what belongs to it, and asks you first, telling you how many groups and how many messages will go. Up to 1.4.1 a single tap on the bin did it with no questions, and only the server row disappeared: its groups and all its messages stayed in the database FOREVER, invisible and unreachable, taking up space and inflating the diagnostics figures. If that already happened to you, the Database diagnostics in Maintenance now says so: an "ORFANI" block with the counts appears at the end of the report. For now it only COUNTS them: removing them is the next step, and I want to know what numbers we are talking about first.

  * Deleting a whole thread now brings the group's unread badge down at once. It used to stay at the old number until the next refresh: it was the only deletion that forgot to recompute it.

 ***  VERSION 1.4.1  ***

 A CORRECTIONS-only release after the public 1.4. No new features.

  * "Read follows the Message-ID" now actually works. It was announced in 1.4 and worked for nobody: the rule was wired to the "mark as read" menu actions, not to reading a message. Every place now goes through a single point. The same in reverse: "mark all unread" propagates too, so it can bring messages back to unread in other groups. (Dave Royal)
  * Your own posts no longer come back as unread after sending. In the same pass the "is this mine?" test was tightened in the three places left behind by the 1.2.12 audit: the address is extracted from the From line and compared, instead of being looked for inside it. (BIG Umberto)
  * Replying no longer loses your place: you replied to the 23rd message and came back to the first post. (BIG Umberto)
  * The thread popup follows "show filtered posts" like the group list. This also fixes the false "thread cannot be reconstructed" and next/previous doing nothing on a filtered post. (Dave Royal)
  * In an expanded thread the first post shows its own read state rather than the whole thread's: no more two red lines when only one message is unread. (Dave Royal)
  * The count after an update says how many of the new messages were filtered, instead of announcing arrivals you will never see. (Dave Royal)
  * The paste button in the compose screen is always there, and it now works where it did not. It only appeared when the clipboard already had text, and NeReO read the clipboard while DRAWING the screen rather than on the press: since Android 10 reading the clipboard needs input focus, writing to it does not. On a phone this never showed, because the on-screen keyboard appearing redraws the bar and the second read succeeds; with no on-screen keyboard that second read never happens. The clipboard is now read on the press, the tolerant way every other app uses, so text copied as HTML or as a link arrives too. (diagnosed from two measurements by Roxy)

  * Custom headers that NeReO reserves for itself are now named BEFORE you send. Some cannot be overridden: the vital ones (From, Subject...) and the control ones (Followup-To, Control, Sender, Expires...), which exist so that text pasted from elsewhere cannot turn a message into a control message by accident. They used to be dropped IN SILENCE - you wrote a valid line, you sent, and the line simply was not there - and the note under the field offered Followup-To as its example. The editor now lists them as you type, and the same rule feeds both the warning and the sending code, so the two cannot drift apart. (reported by Dave Royal)

  * "Mark all read" and "Refresh" swap places in the message list toolbar. A rare action with consequences was sitting next to a frequent, harmless one - the unread filter - and was being hit by mistake. Moving it into the menu would have cost a tap a day to whoever uses it every evening; swapping the two costs nobody anything. (reported by Dave Royal)


 ***  VERSION 1.4  ***

 Release in progress: entries are added as they land.

  * STARTTLS now follows both halves of RFC 4642 instead of one. If a server REFUSES STARTTLS before the handshake, NeReO may carry on unencrypted as the standard allows, and that is unchanged. But if the server ACCEPTS (382) and the handshake then fails, RFC 4642 (section 2.2) says both sides SHOULD close the connection - a strong recommendation, not an absolute requirement - and until 1.3.7 NeReO carried on in cleartext instead. That fallback was probably broken anyway, because the TLS ClientHello had already been written to the socket, but the point is that it should never have been attempted. Such a connection now fails with an explicit message rather than quietly continuing unencrypted.
  * The database has been updated (schema version 12). Servers, groups, articles, filters, identities and drafts are migrated in place and nothing is deleted - and if NeReO ever meets a version it cannot migrate, it makes a physical copy first and tells you. The new structures are not used by anything yet: they are there for the trust-and-certificates work that follows in this same release. As always before a version that touches the database, a backup from Maintenance costs nothing.
  * A restore could give a server a user name it never had. Servers with no account were written to the backup correctly, as an empty value, but read back as the literal word "null": four characters, which NeReO then dutifully tried to log in with, on servers that want no login at all. The server answered "381 Enter password", NeReO sent an empty password, and the server replied with a syntax error that meant nothing to anyone. It was a single missing check among seven fields of the same kind, and it had been there since at least 1.2.8. Restores are now correct, and any server still carrying that fake user name is repaired the first time you restore a backup: the message tells you how many.
  * NeReO no longer sends an empty password when a server asks for one. It says plainly that no password is saved for that server, and suggests clearing the user name if the server does not need an account. The check sits where the server actually asked for the password (a 381 response): a server that is happy with the user name alone answers 281 and keeps working, as RFC 4643 requires.
  * Maintenance has a new "Database diagnostics" panel. It counts the rows of every table and shows the schema version and the space used, with a Copy button. It exists because a database migration that OPENS is not the same as a migration that WORKED: it can be formally correct and still have emptied a table, and the app would start perfectly - it is the archive that would be missing. Take the numbers before an update and again after, and if they match, nothing was lost. It is also the quickest thing to attach to a bug report. Nothing is computed until you press the button.
  * Replying to a crosspost now shows where the message is actually going. Until now the compose screen showed only the group you were reading, even when the reply was set to go out on three. Two related faults went with it: reopening the group selector to CHECK your choice silently reset it to the default (the worst possible case, because the act of checking destroyed what was being checked), and in the panel layout the crosspost button did not exist at all, so changing the groups meant abandoning the reply and starting over. All three reported by Dave Royal.
  * "All messages" now works on high-retention servers. Asking for the whole history of a very large group made NeReO request the entire article range in one go, and the reply could exceed the 64 MB ceiling that protects the app from running out of memory: an error, and nothing downloaded at all. The overview is now fetched in slices, each one saved before the next is asked for. The ceiling stays where it was (raising it would have moved the wall, not removed it) and there are two bonuses: a sync interrupted halfway now resumes instead of starting over, and memory use no longer depends on how big the group is. Reported by Bingo3331.
  * TLS trust is now something you grant, not something you are handed. The normal path is unchanged. What changes is what happens when it FAILS: instead of a dead end, NeReO writes down the identity the server showed and marks it SEEN, NOT TRUSTED, and the connection still fails. You approve it later, calmly, from Servers -> TLS identity, never in a dialog on top of a running sync: a security warning that interrupts you is the one you click without reading. What is pinned is the SHA-256 of the leaf public key, never an intermediate. From then on that key is compared on EVERY connection, and if it changes NeReO stops and shows you both, with no "accept the new one" button. The friction is the feature. There is also a per-server "expired certificate" exception that relaxes the dates and nothing else, and goes inert by itself the day the server renews.
  * "Certificate expires in N days" warning. The trust manager sees the certificate on every connection, so the expiry date is free, and not using it would have been a waste. It comes from a real case: an administrator who renews BY HAND, and a Let's Encrypt certificate that lives 90 days. That situation is not closed, it is cyclical.
  * Accented text no longer breaks in articles with no character set declaration, or with a wrong one. Of seven measured cases, five lost the text; the worst was the WRONG declaration, which produced no error markers at all, just plausible gibberish that NeReO re-published in impeccable UTF-8. A three-step rule, ten cases checked on every build. And because NeReO stores the ALREADY DECODED text, there is a new "Reload message" action for the article bar: without it the fix would be invisible on everything already in your archive.
  * Read follows the Message-ID. A crosspost read in one group counts as read in the others: it is one message, and it is what MesNews does. Watched threads and filters already worked this way; read was the odd one out. On by default.
  * Followup-To read and respected. It was the one GNKSA criterion NeReO did not meet. If the author asks for follow-ups to continue elsewhere, the reply goes THERE, and the compose screen SAYS so: redirecting without saying it would replace a discourtesy with a surprise. If you open the groups selector and choose yourself, your choice wins. The "poster" case does not block posting but warns and offers the author's address.
  * Quote intro settable per group (asked for by Henry The Mole). It REPLACES the one from your identity instead of joining the random draw: the point is a deliberate choice, typically the language of the group.


 ***  VERSION 1.3.7  ***

 Two reports from Dave Royal about restoring a backup. One was right, and the defect went further than he had seen.

  * Restoring no longer duplicates filters and identities. He noticed that a restore ADDED his filters instead of replacing them; checking the code showed IDENTITIES had the same problem. Restoring is now IDEMPOTENT: restoring the same file twice leaves exactly the same state. Filters are matched on name, field, match type, pattern and group scope; identities on their nickname, which the backup format already used as a key to re-link servers.
  * Restoring now asks how: Merge or Replace. The real defect was not "adds instead of replacing": it was that one operation did THREE different things (dedup on servers and subscriptions, blind insert on filters and identities, overwrite on settings). Neither option touches servers, subscriptions or downloaded articles: deleting a server would take its groups and messages with it.
  * "Wrong password" no longer covers import errors. The dialog decrypted AND imported inside one guarded block, so a failure in the second step was announced as a problem with the first.
  * Wrapping a long line on send no longer leaves a trailing space on the broken line. A trailing space is the soft-break marker of Format=Flowed (RFC 3676), so a reader that understands it would have joined the line back to the next one - undoing the very line break just made. It only happened when the break fell on a double space, which is common enough after a full stop. Double spaces INSIDE a line are left exactly as you typed them, and the signature delimiter keeps its required trailing space. Question raised by Adam H. Kerman.
  * "Unreadable file" now tells you why. When a backup could not be opened, the message was those two words and nothing else: a missing file, a permission refused and an empty stream all looked identical. The technical detail now appears in a dialog with a Copy button, because that is what a bug report needs. Brought out by Dave Royal, who repeated the same test six times across two versions and could tell us no more than "unreadable file" - which was all we ever showed him.

 ***  VERSION 1.3.6  ***

 Two conformance defects and one missing function, all of them raised by Adam H. Kerman's questions in an English-language group. They go out ahead of 1.4 because they concern what NeReO puts ON THE WIRE, and what ends up in other people's archives cannot be fixed later.

  * Long subjects with accented or non-Latin characters are now encoded correctly. RFC 2047 allows at most 75 characters per encoded word; NeReO produced a SINGLE word of any length - an ordinary Italian subject came out at 140 characters. The encoding is now split and folded as the standard requires, cutting only between whole characters so that emoji and other multi-byte characters survive intact.
  * An article whose body is pure ASCII is now declared as such (us-ascii, 7bit) instead of always claiming UTF-8. The check happens AFTER the signature has been added to the body, which is where it has to happen.
  * New bar action: Export / share. It hands the article - full headers included - to another app of your choice, so you can archive it or answer the author by e-mail, which NeReO cannot do itself. It replaces a Share action that had quietly disappeared in 1.3.4 along with the old text-selection menu.
  * Two new bar actions: Copy link (usenet.id) and Copy Message-ID. The first builds a web address pointing at the article you are reading - https://usenet.id/ followed by its Message-ID - so you can send someone to that exact post from a chat, an e-mail or a forum. The second puts the Message-ID itself on the clipboard, in its canonical form, which until now could only be obtained by opening the full headers and selecting it by hand. NeReO NEVER CONTACTS usenet.id: it composes the text and copies it - no connection, no new permission. Requested by ObiWan.
  * Corrected preset: OVH is back on port 119 with STARTTLS, instead of port 563 with implicit TLS. Version 1.3.5 had moved all three account-based servers to 563, but OVH offers no TLS on that port: picking it from the built-in list therefore produced a configuration that could not work. Its administrator enabled STARTTLS on port 119 on 14 September 2026, and that combination has been verified on a real device. Nothing changes for servers you have already configured.

 ***  VERSION 1.3.5  ***

 One thing only, and it is not a feature: the error messages coming from the network layer are now in ENGLISH for everyone.

  * Where it comes from: a user running NeReO with the interface in English was answered IN ITALIAN when his server refused the connection ("483 Connessione non cifrata...") and reasonably wondered what he had done wrong. He had done nothing wrong.
  * Why it happened: NntpClient deliberately has no Android dependencies - that is what keeps it testable from the command line - and therefore has no Context and cannot read strings.xml. Its messages are written in the code, and no system language touches them. This was not a forgotten translation: the three strings.xml files have full key parity.
  * What was chosen, and what it costs: if a single language has to serve everyone, it may as well be the one in values/ (the app's default) and the lingua franca of Usenet. This is NOT localisation: it is a declared stopgap, and the price is now paid by those using the app in Italian, who will read fewer than ten messages in English inside an otherwise translated app. The real fix - passing those texts in from outside that layer - will come when that class is next opened for other reasons.
  * The user name is now trimmed of stray spaces, exactly as the server address already was. A space picked up from a paste or from a keyboard suggestion was sent to the server, which counted it - and the only feedback was "authentication failed". The password is NOT trimmed, deliberately: it may legitimately begin or end with a space, and the app has no right to change it in silence.
  * Password field: a reveal button and a live character count. The count is there because it catches what revealing cannot: a trailing space is invisible even in plain sight. If you know your password is eleven characters and the field says twelve, you have found the problem. And if it really does have spaces at the edges, the app says so in red instead of correcting it for you.
  * The NNTP diagnostic log now records the SHAPE of each credential - how many characters, spaces at the edges, presence of non-ASCII characters - and NEVER the value. It is the difference between diagnosing a rejected login from a log and guessing at it.
  * Choosing a server from the built-in list now fills in port 563 with encryption for the three that require an account (Eternal September, Solani, OVH). Until now the list offered port 119 unencrypted to everyone, those three included - and since NeReO refuses to put a password on an unencrypted connection, the very first attempt ALWAYS failed with a 483. It was not an edge case: it was the normal path, and whoever picks a server from a list does so precisely in order not to have to choose a port. The two open servers stay on 119. Nothing changes for servers you have already set up.
  * One defect that showed up in two different ways has been fixed. "Open the first unread" sometimes jumped back to the opening post - ALREADY READ - even with unread messages waiting; and marking a conversation as read occasionally left it showing. They were the SAME defect: NeReO answered the question "which messages belong to this conversation" in THREE ways, and only one of them was the threading engine that actually draws the screen. The other two compared text in the References header, and disagreed whenever the opening post had never been downloaded or a message listed only its direct parent. Now every part of the app asks the threading engine.
  * A note for whoever reported it: orphan replies were exactly right. Subject changes were not - NeReO's threading engine never looks at the subject, so renaming a message cannot affect grouping.
  * Signatures may now use blank lines between their lines of text. Four lines separated by blanks used to lose the last two, because the limit counted physical lines; it now counts lines of text, with an overall cap so a signature cannot be padded out with empty space. Worth knowing: classic netiquette counts four physical lines, blanks included - this is NeReO being lenient, and another newsreader may still trim it.

 ***  VERSION 1.3.4 - TECHNICAL RELEASE  ***

 Not a single line of the app itself changed since 1.3.3. What changed is everything NeReO stands on.

  * Build toolchain migrated: Android Gradle plugin 8.7.3 -> 9.3.0, Gradle 8.9 -> 9.5.0, Kotlin 2.0.21 -> 2.2.10, KSP moved to the KSP2 numbering. Done with Android Studio's AGP Upgrade Assistant, not by hand: the tool picked the versions that this particular installation can actually support.
  * Jetpack Compose from 1.7.6 to 1.12 - twenty months in one step. compileSdk goes up to 37 because Compose 1.12 requires it; targetSdk STAYS at 35, deliberately: raising it would trigger new operating-system restrictions, and we do not want to mix THOSE with this jump.
  * Why it goes out on its own, with nothing else: twenty months of Compose touch text rendering, scrolling, keyboard and animations
    - that is the whole app. If something misbehaves after this, there has to be exactly ONE suspect. It is the same rule that sent 1.2.14 out alone.
  * The intermediate step was field-tested: new toolchain with the old Compose, built and installed - APK verified, AGP 9.3.0 with Compose 1.7.6, everything normal. So AGP and Gradle are already cleared: if something breaks now, it is Compose.
  * What we are hoping for, stated for what it is: Compose 1.12 contains explicit work on mouse-driven text selection and on the selection toolbar. On a device with no touchscreen that toolbar has never appeared. This is the only way to find out whether the problem was already fixed upstream. It is a FORECAST, not a promise: if it is not fixed, we will know that road was not enough - and twenty months of debt will be gone regardless.
  * One thing was lost along the way, and it is only fair to say so: selecting text in a message no longer offers "Search the web" and "Share". Those two were added in 1.2.8 at Dave Royal's request, by replacing the selection menu with our own. Compose 1.12 no longer goes through that mechanism: the menu is now the platform one, and ours was being ignored in silence.
  * Why they cannot be rebuilt: the new API does let an app ADD items to that menu, but it never says which text is selected - verified in the androidx source, not inferred. Without the text, "Search" and "Share" have nothing to act on. Copy and Select all are unaffected, and the "Copy the message" bar action is still there.
  * SelectionToolbar.kt has been deleted rather than left in place and inert: its comment kept claiming that it worked, and a file that lies is worse than a file that is missing.
  * If you notice anything behaving differently from 1.3.3, report it even if it seems trivial. That is precisely what this release is for.

 ---

 ***  VERSION 1.3.3  ***

 What's new in this version:
  * When an encrypted connection fails, NeReO now says WHICH of the two possible problems it is: the fixes are different, and up to 1.3.2 we treated them as one.
  * SIGNING CHAIN: if the certificate is not signed by an authority Android knows (self-signed, private CA) or has expired, the message says so and states plainly that the security option CANNOT HELP. Up to 1.3.2 we suggested it in every case, sending people to look for a way out that does not exist.
  * HOST NAME: if instead the certificate is valid but issued for a different address, the option really does help and the message points at it.
  * UNDETERMINED: if the failure matches neither pattern, NeReO says so instead of inventing a diagnosis, and shows the raw exception text - the datum whoever reports the problem actually needs.
  * The option has been renamed to what it actually does: "Do not check the host name on the certificate". Its old name, "Allow unverifiable certificates", promised more than it delivered: it NEVER made NeReO accept a self-signed, unknown-authority or expired certificate - those are refused by Android's TrustManager, which that switch does not touch. Its note now says so.
  * No behaviour change: what connected before still connects, what was refused is still refused. Only what NeReO SAYS changes.
  * Reported by Bingo3331, who on Paganini did exactly what the error message told him to do, saw it fail, and concluded - reasonably - that the option was broken. It is not broken: it does something else. The dead end was ours.

 ---

  * New "Copy the message" action in the reading bar, both on the phone and in the multi-pane reading pane: ONE CLICK copies the whole message to the clipboard, with no need to select anything first. It is customizable like every other bar action (you can move it into the three-dots menu or switch it off). Alongside it, a "Paste" button in the compose screen toolbar.
  * Why it was needed, and it was not what we thought: Roxy's test video settles it. On his TV box text SELECTION WORKS - the block highlights and extends as you drag - but the floating Copy/Search/Share bar NEVER APPEARS, not for a single frame in 22 seconds with the selection sitting in the middle of the screen. It is not a popup positioning problem, as we had assumed: the bar is never asked to appear at all.
  * The clue to the mechanism: the selection HANDLES are missing too, and Compose draws those on the touch path - whereas on a phone, with a finger, both handles and bar are there. Two devices, two input paths, consistent signals in opposite directions: it is the MOUSE path that does not raise the bar. And on Android the right button is BACK, so there is no context menu to fall back on either.
  * If you have a keyboard attached, Ctrl+C and Ctrl+V already work, including where the floating bar does not appear - confirmed by Roxy on his TV box. To copy PART of a message that remains the quickest route, and it is worth knowing: the capability was already there, the information was not.
  * The fix goes around the problem rather than fighting it: actions that do not go through the selection at all. One click, identical with touch and with a pointer.
  * Known limit of the action: it copies the WHOLE message. To copy a fragment you still have to select it - with a finger and the bar, or with Ctrl+C.

 ---

 ***  VERSION 1.3.2  ***

 What's new in this version:
  * On the PHONE too you can now go straight to the first unread message. New optional setting in Appearance -> "Tap the thread to open the first unread": tapping the head of a thread opens the first message you have not read yet, instead of the first post (if you have read them all, it opens the first post). It works in the TREE view and in the GROUPED one.
  * Off by default: if you do not turn it on, nothing changes. In the grouped view, if "Tap the message to expand the thread" is on as well, EXPANDING TAKES PRECEDENCE - there the thread opens in place and staying in the list is the sensible thing.
  * Why a new setting and not the 1.3.1 one: that one only acts where a row stands for the whole collapsible thread (grouped view), and in the tree view - the very one this was asked for - that premise does not hold. Reusing the existing switch could not have worked: two different behaviours in two different views, so two switches.
  * The declared User-Agent now follows RFC 5536 (technical note by Dave Royal): NeReO/1.3.2 (Android), with the version attached by a SLASH. Up to 1.3.1 it read "NeReO 1.3.1 (Android)", with a space.
  * Why this was not merely cosmetic: RFC 5536 sect. 3.2.13 defines product = [CFWS] token [ [CFWS] "/" product-version ], so the version attaches to the product with a slash. With a space the header was not ILLEGAL - the grammar allows several products and "1.3.1" is a valid token - but it said something else: TWO PRODUCTS, one named "NeReO" and one named "1.3.1". Anyone collecting newsreader statistics saw a piece of software that does not exist. The "(Android)" comment is CFWS and was already right.
  * Identities already saved are realigned once, and ONLY if they still hold exactly one of the defaults we shipped. A User-Agent you wrote yourself is never touched - an empty one included, which since 1.2.9 means "declare nothing" and is a choice, not an oversight.
  * Fixed: if the newest article in a group was cancelled, that group stopped updating. Every attempt answered "423 No articles in NNNN-NNNN", and it only unblocked itself once somebody posted again.
  * Why it happened: once the last article is cancelled, the group's reported high water mark does NOT come down (RFC 3977 sect. 6.1.1: not all article numbers between the low and high water marks correspond to existing articles), so NeReO asked for the range from its own bookmark up to that number - a range that is now empty. The 423 the server returns is NOT an error: RFC 3977 sect. 8.3 defines it as "no articles in that range", and it is the right answer. NeReO treated it as fatal.
  * What it actually cost, beyond the message on screen: the shared connection was torn down and reopened for the remaining groups (socket + TLS + authentication all over again, on every update), the group was skipped by the anti-loop guard, and the bookmark never moved - so the next update asked for the same dead range. Now 423 yields an empty list, the bookmark moves on, and the dead range is consumed once.
  * This was not a laboratory case: gaps between the low and high water marks are part of the protocol, and spam cancels hitting the newest article of a quiet group are routine. BIG Umberto's deliberate test on alt.test.a, reproduced three times on three different servers (Solani, eternal-september, Paganini), did not invent an impossible scenario: it made reproducible something that happens on its own and that until now could be written off as a "connection error".
  * Under the hood: before adding the FIFTH place in the code that works out "first unread of the thread", the logic was extracted once and all five now share it. 1.3 shipped with a defect exactly because a fix had been applied to two copies out of four of a duplicated block.

 ---

 ***  VERSION 1.3.1  ***

 What's new in this version:
  * In the multi-pane layout, tapping a thread now expands it AND goes there: with "Tap the first post to expand" enabled, the tap opens the first UNREAD message of that thread in the reading pane (or the first post, if you have read them all). Collapsing the thread puts the selection back on the first post, the only row still visible.
  * Why this was serious rather than merely annoying: before, the tap only expanded, and the first post of any thread with replies could not be opened at all. In 1.2.9 we pointed at the long-press menu as the way out - but on a TV box with a remote that gesture DOES NOT EXIST, so for anyone reading there the message was unreachable. Reported by Roxy, who had asked for that option himself back in 1.2.9.
  * Every row in the thread list now carries a "..." (vertical ellipsis) button that opens its context menu - open, plonk, supersede, withdraw, retention, multiple selection - with a SINGLE CLICK. The long press keeps working exactly as before: this adds a way in, it does not replace one. It comes from the same report: on a device driven by a pointer (a mini keyboard with a trackpad) holding a button down is technically possible, but so slow that - in Roxy's words - "it would put you off using it".
  * The reading pane bar in the multi-pane layout is now customizable too, like the phone one (Appearance -> Customize reading bar), with one deliberate difference: here "off" does not mean HIDDEN but MOVED INTO THE three-dots MENU - nothing becomes unreachable. It ships with ROT13 and Headers already in the menu, because on a tablet held upright the pane is narrow and five buttons left almost no room for the subject line. Suggested by Dave Royal, and asked before by Roxy.
  * Multi-pane only, deliberately: on a phone the tap NAVIGATES AWAY, so "expand and go" would be a contradiction - and there the long press is a natural gesture, not an obstacle.

 ---

 ***  VERSION 1.3  ***

 What's new in this version:
  * Fixed a gap in 1.2.17: "Send the password only when the server asks" was NOT applied when publishing a message or downloading the group list - those two paths still authenticated immediately. The cause: the same block of code existed in four copies and only two had been updated. The four places that opened a connection are now merged into one, so the rule holds everywhere by construction.
  * Otherwise 1.3 is 1.2.17 as tested in the field: it is the stable release gathering the whole 1.2.12-1.2.17 line - the hardening pass that came out of the security audit, TLS hostname verification, withdraw and supersede with per-server outcome, and everything learned from two days of real testing on three providers.
  * 1.3 is the FIRST PUBLIC RELEASE since 1.2.11: anyone updating from there also gets, in one go, everything from versions 1.2.12-1.2.17, which were never distributed (see the note just below).

 ---

 ======================================================================
  PLEASE NOTE - VERSIONS 1.2.12 THROUGH 1.2.17 WERE NEVER RELEASED
 ======================================================================

 The last version published on SourceForge was 1.2.11.

 Everything listed below, from 1.2.12 to 1.2.17, was built and tested INSIDE the project only: against three real servers (Astraweb, usenet.ovh, eternal-september) and on a single device. None of those versions ever reached users, and no public APK contains them.

 They are listed because their content is ALL inside 1.3: they are here to explain what changed and why, not to point at an update that never existed. Anyone coming from 1.2.11 goes straight to 1.3 and gets everything at once.

 ---

 ***  VERSION 1.2.17  ***

 What's new in this version:
  * New "Send the password only when the server asks" option (Maintenance -> Security), OFF by default. Normally NeReO authenticates as soon as it connects, on every connection; with this on it waits for the server to ask, authenticates then, and repeats the command. On a server that cannot encrypt the connection this matters a great deal: INN servers let you read without credentials and only require them to publish, so the password crosses the network a few times a month instead of dozens of times a day. Off by default because it changes how EVERY server is contacted: if one stops working, turn it back off and everything is exactly as before.
  * The server list now shows "last connection was NOT encrypted" under any server that is talking in clear. It is based on what actually happened, not on the settings: a server can be configured to encrypt, announce it, and still fail to do so. It is a permanent mark rather than a recurring warning, because that state does not change by itself and a warning that keeps coming back is one you learn to ignore.
  * The diagnostic log no longer counts expected replies as errors: a successful withdrawal used to end its session with "1 error", because the 430 "no such article" - which is precisely the answer being looked for - was counted as a failure.

 ---

 ***  VERSION 1.2.16  ***

 What's new in this version:
  * Withdrawing a message (cancel) now tells you how it went on EVERY configured server, not just the one you posted to. A list appears at the end - withdrawn, still there, could not check - with the server the request was actually sent to marked as such. It is the answer you could previously only get by checking by hand, one server at a time, from another program.
  * The confirmation dialog now says up front what you used to find out afterwards. Four field tests on September 6th and 7th established that a withdrawal acts, in practice, only on the server you posted to: other servers keep the message, and many providers ignore withdrawal requests entirely. Hence the advice now written into the confirmation: if a correction has to be seen by everyone, the right tool is "Supersede", not "Withdraw" - a supersede is an ordinary article and always propagates, while a cancel is a control message that almost nobody forwards.
  * No background work: the servers are queried only at that moment, one connection each, and an unreachable one becomes "could not check" without failing the rest.

 ---

 ***  VERSION 1.2.15  ***

 What's new in this version:
  * "Withdraw from server (cancel)" now tells you WHETHER IT WORKED. Until now the app only said "request sent, the server may ignore it", so anyone withdrawing a message on a server that ignores control messages stayed convinced it was gone. Right after sending, NeReO now asks the server whether that article is still there and reports the answer: withdrawn, still present, or not verifiable. Three outcomes rather than two, because we have seen all three in the field.
  * A message that no longer exists on the server (expired, or withdrawn by its author) is now reported as such instead of as a generic network error, with a "Remove the local copy" button next to the explanation to clear the leftover summary row. Both in the page view and in the reading pane.
  * Supersedes handled on read: when a message replaces another, the replaced one is HIDDEN (not deleted: "Show hidden" brings it back), and only if both come from the same address. Without that check anyone could make someone else's message disappear from a reader's view.
  * Invisible technical fixes: a plain "article not found" reply is no longer mistaken for a dropped connection (a 1.2.13 defect that cost a needless reconnection); a server given as an IP address no longer produces an incomprehensible error; and changing the security switches now immediately closes any connection opened under the previous settings.

 ---

 ***  VERSION 1.2.14  ***

 What's new in this version:
  * TLS HOSTNAME VERIFICATION. Up to 1.2.13 the traffic to the server was encrypted, but nobody checked that the certificate presented really belonged to the address requested: a hostile network could interpose itself and read everything, credentials included, without the app noticing. The check is now in place on both paths (implicit TLS and STARTTLS), the way a browser does it.
  * If a server has a misconfigured certificate the connection now fails with an explicit message saying what happened and where to act, instead of a generic network error.
  * New "Allow unverifiable certificates" switch (Maintenance -> Security), OFF by default and flagged in red: if you know your provider's certificate is set up badly, you can still connect. It is global, not per-server, following the same model already used since 1.2.6 for cleartext authentication.
  * This version ships ON ITS OWN, deliberately: it is the one security-audit fix that can stop a currently working setup from working. If a server no longer connects after the update, the cause is unambiguously this one, and the cure is the switch above.

 ---

 ***  VERSION 1.2.13  ***

 What's new in this version:
  * No more network error shown in place of a message body when moving quickly from one message to the next. The connection kept open for reading was being dropped by the server while you read, and the first command after the pause fell into the void: now, if an I/O error happens on a REUSED connection, that connection is discarded and the message is fetched once more over a fresh one, silently. Diagnosed from the NNTP diagnostic log.
  * User-Agent pre-filled on NEW identities (requested by Merovingio): the field started out empty on a fresh install, so anyone who didn't fill it in posted anonymously without choosing to. It now contains %nereo%, which becomes the app name and its current version when you post (and stays up to date on its own). Empty it if you prefer to declare nothing: EXISTING identities are left exactly as they are.
  * Hostname verification for encrypted connections moves to 1.2.14 accordingly: it remains the one fix that ships on its own.

 ---

 ***  VERSION 1.2.12  ***

 What's new in this version:
  * General hardening pass, following a security audit of the whole codebase. NeReO now copes safely with malformed or deliberately hostile articles and server replies: oversized messages, headers with line breaks smuggled inside them, abnormal References chains, unusual Face images, tampered backup files on restore. Nothing changes in normal use - these are defenses you only notice when something tries to take advantage.
  * "Supersede..." now appears only on YOUR OWN messages, and next to it there is a new "Withdraw from server (cancel)...": it sends a control message asking the server to withdraw an article you posted. Many servers ignore such requests, and propagation to other servers is never guaranteed: the confirmation dialog says so plainly rather than letting you find out afterwards.
  * NOT included here: hostname verification for encrypted connections. It is the one fix that can stop a setup that works today from working (some providers present a certificate whose name doesn't match), so it ships on its own in the next version - that way, if something stops connecting, there is no doubt about what caused it.

 ---

 ***  VERSION 1.2.11  ***

 What's new in this version:
  * Updated the internal database library (Room) from 2.6.1 to 2.8.4: an attempt to fix the rare CursorWindow crash (see version 1.2.10 below) further upstream. 2.8.4 includes two official fixes for races between read Flows and concurrent writes, the same category as that crash - this is not a confirmation of the exact cause, it remains a partly-verified hypothesis, and the defenses already added in 1.2.10 stay active regardless, as an extra safety net. No visible change for the user.
  * Extended the same defense (show the last known good list instead of crashing) to text search as well, which shared the same risk but wasn't protected yet.

 ---

 ***  VERSION 1.2.10  ***

 What's new in this version:
  * More resilient article list: a rare database read error (a Room CursorWindow IllegalStateException) that could happen while leaving a group as a sync was still writing new messages no longer crashes the app - if it happens, it now shows the last known good list instead of closing. First real version to ship this defense: it had been ready since September 5th, waiting for a delivery round.
  * New "Auto-sync on opening a group" switch (Maintenance, reported by a tester via Marco): entering a group - even just coming back from an article you'd just read - always synced it, with no way to turn that off, so getting distracted while reading meant threads you'd left unread got mixed in with new ones. You can now turn it off, keeping everything still until you trigger a manual sync (button/pull-to-refresh, always available either way). Default on: no change for anyone who doesn't need it.

 ---

 ***  VERSION 1.2.9.c  ***

 What's new in this version:
  * More robust syncing: removed a possible race condition between the automatic new-message check and other syncs in progress (manual or timer-driven), which could have contributed to an occasional crash - a report from public testing still under investigation (see 1.2.9.b below for the diagnostic log). This is not a CONFIRMED fix for that specific crash (no direct proof yet), but a real concurrency defect, demonstrated in the code.

 ---

 ***  VERSION 1.2.9.b  ***

 Three fixes from 1.2.9's public testing, plus a new diagnostic tool: the first real new version to compile and distribute to users (the earlier fixes had stayed "version unchanged" because 1.2.9 had not been published yet).

 What's new in this version:
  * Search: the results list no longer resets when reopening a result (reported by Marco): coming back from a message opened from search, the query and results stay as they were instead of starting empty - before, you had to redo the same search every time to reach the next result.
  * "Show filtered" is now a complete audit (reported by Marco: "if they're filtered, why would we ever see them?" -> "to me this is a broken audit"): applying filters to already-downloaded messages, ones matching an "Ignore" rule (not just "Delete completely") are now marked and shown grayed out too - before, only the latter were actually removed, the former stayed invisible to the audit.
  * Blank User-Agent = a truly anonymous post (reported by Marco): leaving the User-Agent field blank in an identity now means the posted message carries no User-Agent header at all, exactly as the help text always promised - before, it was silently replaced with the app's default value.
  * New: last-crash report (Maintenance): if the app ever closes unexpectedly, the last crash is now saved on the device on its own - nothing is sent anywhere automatically. If it happens to you, please copy or export it from the new "Last crash" section and send it over: without this, a crash report with no other details can't actually be diagnosed.

 ---

 ***  VERSION 1.2.9  ***

 Real multi-identity (requested by Marco: "a real multi-user setup, like MesNews and/or Piao Hong's NR MOD17") plus the rest of the NNTP roadmap (item E), in the same release - the full plan, chosen explicitly by Marco instead of a staged one. This closes the gate towards the definitive 1.3, pending only on-device testing.

 What's new in this version:
  * Real multi-identity: create as many identities as you like (name, email, signature, User-Agent, X-No-Archive, custom headers, quote intro, Face) and assign each one to a specific server - post as "MCSM" on one server and as "TAF" on another, each with its own address and Face. New Identities screens (list + editor); a server's identity is assigned by editing that server. If you already had a general identity or a per-server override in an earlier version, it's migrated automatically, nothing to redo.
  * STARTTLS (RFC 4642): servers that stay on a plaintext port (usually 119) can now upgrade to an encrypted connection right after connecting, as an alternative to implicit TLS.
  * CAPABILITIES: read and logged in diagnostics at connection time.
  * Faster reading: while reading through a thread, article bodies now reuse the same connection instead of reconnecting for every single article.
  * Per-server override of plaintext authentication: force it always allowed or always blocked for a single server, regardless of the global setting (Maintenance -> Security).
  * Backup and restore now carry identities and the new per-server options too; older backups still open exactly as before.
  * Very nested quotes - numeric threshold (Appearance, requested by Marco): beyond a certain depth (default 8, configurable to 6/8/10/12) the repeated ">" marks turn into the level number ("9", "10", "11"...) instead of growing forever - display only, with the "chevron"/"both" quote style: the text sent and received always keeps the standard ">" marks, so quote depth still reads correctly everywhere, including in other clients. On by default.
  * Tap the message to expand the thread (Appearance, optional, requested by Roxy): as an alternative to the +/- button (enlarged to a 48dp minimum for anyone aiming from a distance, e.g. a TV box with a remote), tapping the text of a thread's first message can expand or collapse it instead of opening it; the message stays reachable from the menu (long press) -> "Open". Off by default.
  * Drafts moved inside the group (requested by Marco): "Drafts" is no longer in the main menu - it opens from the menu of the group you're writing to, already filtered to that group's drafts (every draft already knows which group it belongs to). A "This group" / "All groups" toggle inside the screen still lets you see every draft together when you need to.
  * Vertical swipe: now also moves across threads (reported by Marco): in reading view, with "unread only" off, swiping past the last/first message used to stop silently at the end/start of the current thread instead of continuing - different from "unread only" on, which already jumped to the next thread. The vertical swipe now behaves the same way in both cases (the single < > arrows still stay bounded to the current thread, as always - the double arrows are for that).

 ---

 ***  VERSION 1.2.8  ***

 An intermediate step between 1.2.7 and the future "complete" 1.3: gathers everything that had been prepared as 1.3/1.3.1/1.3.2 during solo testing, renumbered before any wider release - none of those versions ever shipped officially (those numbers were later reused by the real versions described above: same names, different builds). The real 1.3 will land only once the rest of the NNTP roadmap (CAPABILITIES, STARTTLS, connection pooling, per-server unencrypted-auth override) is done or dropped.

 What's new in this version:
  * Customizable reading bar: under Appearance -> "Customize reading bar" choose which icons the bottom reading bar shows and in what order (show/hide switch + up/down arrows, which also work well with a TV box remote).
  * Selective refresh: the refresh icon now opens a menu - "Refresh everything" (as before) or "Choose which servers to refresh". A similar menu on each server's header lets you refresh just that server or just some of its groups, instead of always downloading everything. The server-level menu is also available in tablet/panel mode.
  * Full-screen reading in panel mode (tablet): while reading, a new icon temporarily hides the groups and threads columns so you can focus on the text; it closes itself again when you leave the article.
  * Search/Share on selected text: selecting text in an article now also offers Search (web search) and Share, next to Copy and Select all. "Translate" has been dropped FOR GOOD (it would depend on a handler not guaranteed on every device).
  * Update check, four modes: under Maintenance -> Updates, choose Never / Once a day / Every 4 hours / On every launch. A "Last check: ..." line always shows below the picker, so you can see it's working even when it finds nothing new (no popup in that case, just that line).
  * Fixed a bug: the old "once a day" limit was per calendar day, not elapsed time - opening the app even once before a new release went out could leave you "stuck" until the next day, no matter how many more times you reopened the app.
  * Beefed-up NNTP diagnostics (Bingo3331 + HAL): with the NNTP log switch on (Maintenance -> Diagnostics), the log now also records a summary of every OVER/XOVER response (rows received, missing numbers, duplicates), per-command timing, and a header/footer for every NNTP session - built to track down gaps in a provider's backfill (e.g. Astraweb).
  * New manual diagnostic "LISTGROUP diagnostic...": asks the server for the real list of articles in a group and compares it with the last overview summary; for numbers missing from the overview, it checks with STAT whether they truly exist - telling apart a gap in the server's overview database from an article that's really gone.

 ---

 ***  VERSION 1.2.7  ***

 What's new in this version:
  * Insecure connection: if a server refuses your password over an unencrypted connection, a clear dialog now offers two choices - "Switch to SSL" (recommended) or "Allow unencrypted" (not recommended). No more silent failure for newcomers. The dialog does not appear during the periodic automatic check, only when you refresh yourself.
  * Reply to crossposts: a new option in Maintenance to reply to ALL the original groups by default instead of just the current one (off by default; you can still choose case by case).
  * Continuous view: a thicker separator now marks where each new sub-thread begins, so they stand out at a glance.

 ---

 ***  VERSION 1.2.6  ***

 What's new in this version:
  * Security: your password is no longer sent over an unencrypted connection (it would travel in clear - RFC 4643 forbids it). If you really have an old server that needs it, you can knowingly allow it under Maintenance -> Security. Prefer the SSL port (usually 563).
  * "Check for updates" is now also in the main menu, next to About and What's new - no more digging into Maintenance for it.
  * Panel view: a three-dot menu on every post, with author plonk. Before you had to switch to full-screen view to plonk.
  * Panel view: you can reorder groups here too (a "reorder" icon at the top of the groups column), not only in full-screen view.
  * Backup now saves the group order: restoring no longer puts everything back in alphabetical order.
  * Cleaner Headers view: the long Face/X-Face base64 code is no longer shown (the little face still appears next to the author).
  * Mute quotes: a new button hides quoted text with one tap, so only the new message stands out (tap the placeholder to show it again). A "quote" icon in the reading bar and in the panel headers.
  * Panel view: in the reading pane, swipe up/down to jump to the next or previous unread post in the thread (if there are none, to the next or previous post) - just like on the phone.

 ---

 ***  VERSION 1.2.5 (fixes)  ***

 What's new and fixed in this version:
  * Continuous view: opening a long thread no longer marks the messages after the selected one as read in bulk. Now only what you actually see is marked read - the chosen message on opening, then each message as you bring it to the top by scrolling.
  * Continuous view: crosspost groups are now shown (in red) on every post, not only in the single-article view.
  * With the "unread only" filter on, the empty list now says "No unread messages in this group" instead of "No articles...".
  * Update check: NeReO can tell you when a newer version is on SourceForge (automatic once a day, can be turned off in Maintenance, plus a manual button). Notification and link only: the app never downloads or installs anything itself.

 ---

 ***  VERSION 1.2.4 (fixes)  ***

 What's new and fixed in this version:
  * Reflow text: now also joins the "orphan" one- or two-word lines left in the middle of a paragraph. Before, even with reflow on, a few short lines (like a lone "is" or "and") stayed isolated because the previous heuristic mistook them for deliberate breaks. Only labels, greetings and short lines that end with punctuation (. , : ; ! ? ...) now keep their own line. Off by default; reading only.

 ---

 ***  VERSION 1.2.3 (fixes)  ***

 What's new and fixed in this version:
  * Tree view: messages that open a discussion are no longer shown as "already read" when they aren't. The colour now follows the post's real state, matching "jump to next unread".
  * Continuous view: replying to a crossposted message now shows the prompt to choose which groups to reply to, like the single-article view. Before, the reply silently went only to the first group.
  * Continuous view: opening several unread messages no longer shows "Error: connection reset" instead of the text. Bodies are fetched one request at a time rather than opening many connections at once.
  * Panel view: refresh now uses the icon at the top of the column by default; the pull-to-refresh swipe no longer steals the first upward scroll. The swipe can be turned back on in Appearance. The phone is unaffected.
  * Reflow text: now also acts on posts wrapped narrow (around 38-40 columns), which were skipped entirely before and kept their orphan words. Still off by default; reading only.
  * Continuous view: opening a thread on a message in the middle no longer marks the messages above it - merely skipped past - as read. Marking now applies from the chosen message downwards, not to what flashes by on opening.

 ---

 ***  VERSION 1.2.2 (fixes)  ***

 What's new and fixed in this version:
  * New "Reflow text" option (Appearance): rejoins paragraphs broken by the classic 72-column wrap, so posts read tidily on narrow screens instead of showing one- or two-word orphan lines. Off by default; reading only.
  * Refreshing is much faster: one connection per server instead of one per group (with many groups the difference is noticeable). The new-messages count now also appears when you tap the refresh icon, not only with pull-to-refresh.
  * Panel view (tablet): in continuous view the third column now follows the same thread order as the tree (not date order). Writing a post opens the editor full-screen, so the keyboard no longer covers the text area in landscape.
  * Panel view (tablet/landscape): picking a message from the tree again positions it correctly at the top and keeps it there (in 1.2 it could land in a random spot).
  * The "refresh" spinner no longer sticks at the top of the tree pane: scrolling stays immediate.
  * SSL/TLS connections: added the missing read timeout, so a stalled encrypted connection no longer leaves the app waiting forever.

 ---

 ***  VERSION 1.2  ***

 What's new in this version:
  * Server compatibility: some providers (Astraweb among them) reject the OVER command used to fetch headers, and syncing would stop. NeReO now falls back to XOVER automatically, so those servers work again.
  * Plonk extended to replies: a rule on "from" can now also hide the replies to the filtered post and the whole branch below it, whoever wrote them (reversible: remove the rule and they come back).
  * Parity on tablets: the panel (three-column) view now has the full headers and ROT13 buttons, and the Drafts button is reachable too. Continuous view opens already positioned on the chosen message.
  * Groups: long-press a group to download, in one go, the missing bodies of the last 300 messages. "Follow the thread" now applies to the whole thread; your own posts are recognised even with per-server identities.
  * After posting: a "Message sent" confirmation and an automatic group refresh, so your own message shows up without a manual reload (as soon as the server makes it available).
  * Complete backup (per-server identities, all settings and followed threads; restoring stays compatible with older backups).
  * NNTP diagnostics under Maintenance: records the dialogue with the server (password always masked) to solve odd cases faster. Off by default.
  * Fixes: bottom icon bar wraps to two rows on narrow screens; a spurious error while scrolling fast is gone.

 Everything else keeps the full feature set of 1.1. The interface is available in Italian and English.

======================================================================


=============
 WHAT NeReO IS
=============

NeReO is a Usenet newsgroup reader for Android, dedicated to text discussions.

No binary groups, no attachments, no ads, no telemetry: just messages and conversations, the way Usenet has always been.

It is free software, released under the GNU GPL v3 or later. The name "NeReO" and its logo are reserved by the author: modified versions must use a different name (see TRADEMARK.txt). Sources also on GitHub: https://github.com/mcsm64/NeReO


===================
 INTERFACE LANGUAGE
===================

NeReO follows the phone language automatically:

  * phone in Italian     -> Italian interface
  * any other language   -> English interface

If you prefer a language different from the phone's, you can set it for NeReO only, without touching the rest of the system, in two ways:

  * inside the app:  Appearance -> Language
  * from the system: Settings -> Apps -> NeReO -> Language (Android 13 and later only)

On Android 13 and later the two paths control the same choice. On Android 12 and earlier the system option does not exist: in that case you change the language from the switch inside the app.

Only these two languages are provided.


==============
 WHAT IT DOES
==============

READING
  * Three ways to view discussions: threaded, sequential, or grouped by thread.
  * Continuous view: all messages of a discussion one below the other, with a direct jump to the message chosen in the tree.
  * Colour-graded quotes by level, which you can shorten.
  * Face (Usenet's historic avatar) shown next to the author.
  * ROT13, full headers, conversation tree.
  * Gesture navigation: scroll to move to the next message or thread.
  * Indication of replies to your own posts, with a direct jump to the first unread one.

WRITING
  * Replies with quoting in four modes, including reduced quoting and line-by-line trimming.
  * Crosspost: replies and new threads across several groups.
  * Signature with rotating variants and automatic placeholders.
  * A different identity per server, if needed.
  * Drafts saved locally.

ORGANISING
  * Filters with clear actions (Ignore, Delete, Highlight, Watch the thread) or score-based for those who want fine control. They apply to already-downloaded messages too.
  * One-tap author plonk.
  * Search by message or by discussion.
  * Automatic retention: by age or by number of discussions, set per group.
  * Backup and restore of the configuration, with optional encryption.

APPEARANCE
  * Light, dark or automatic theme; Material You dynamic colours.
  * Ten interface tints, six text sizes.
  * Panel layout for tablets and wide screens, with adjustable ratios.

CONNECTION
  * NNTP with authentication, SSL/TLS on a dedicated port.
  * Automatic IPv6/IPv4 fallback.


======================
 FIRST-TIME SETUP
======================

1. Open the menu (three dots, top right) and choose "Servers", then the "+" at the bottom. You can start from a known server or enter one by hand. Free example: news.eternal-september.org, port 563, SSL (requires a free registration on their site).

2. From the menu, open "Identity and signature" and enter the name and e-mail you want to appear with in the groups.

3. Go back to the list and tap the "+" at the bottom to browse the groups available on the server and subscribe to the ones you like.

4. Tap a group to download its messages.


======================
 INSTALLATION NOTES
======================

* If you are updating from a previous version, install the APK over the existing one: do NOT uninstall first, or you will lose groups, read state and settings.
* If the phone blocks the installation, allow unknown sources for the app you open the file from.
* Requirements: Android 8.0 or later.


==============
 REPORTS
==============

For a useful report, three things are enough:
  * what you were doing when it happened;
  * a screenshot, if the problem is visible;
  * the block from About -> Technical data (there is a Copy button).


================
 VERSION HISTORY
================

 The versions described in full above are not repeated here: this is the
 tail, that is everything older than 1.2.

1.1        In-app language switch (Appearance -> Language). Database safety net: automatic copy and warning before any reset for an incompatible schema.
1.0        First stable version. Interface in Italian and English.
1.0-RC01   Release candidate: full interface translation.
beta20     Fixed three crashes (very large groups, empty screen on the back button, confirmation dialogs). Filters applicable to already-downloaded messages; delete discussion; jump to replies to your own posts; quit application; fixes to continuous view, keyboard and reduced quoting.
beta19     Reduced quoting fixed; auto-capitalisation; full group list download; retention by number of discussions.
beta18     Replies to your own posts highlighted; continuous thread view; filters with actions instead of scores; crosspost on a new thread.
beta17     Reply to crossposts choosing the groups; IPv6/IPv4 fallback.
beta16     Fixes to the landscape layout and the interface.
beta14-15  Full headers; reliable Face; trim quote.
beta11-13  Search; group reordering; crosspost; splash screen.
beta7-10   From MyNewsReader to NeReO; drafts; multiple identities.
beta1      First release.


===============
 THANKS
===============

NeReO got this far thanks to those who tried it patiently version after version, reporting flaws, proposing features and pushing back when something wasn't right.

Thanks to Roxy, Bingo3331, Big Umberto, rpoggi, Paul, Dave Royal, Alessandro, John (n2n), Macchia, il Merovingio, Adam H. Kerman, ObiWan, Ammammata, Henry The Mole, SiMcarD, cercaparole and Gabx: many of the things that work well today exist because someone took the trouble to explain precisely what was wrong.

SPECIAL THANKS TO ALLEN, for support that was always useful and always CROSS-CUTTING, behind the scenes. He is not listed above next to a feature, and that is exactly the point: his contribution was not a report about one thing, it was being there where it was needed, release after release. It is the kind of help that leaves no trace in a changelog, and without which a changelog like this one would not exist.

Thanks to IVO GANDOLFO, administrator of paganini.bofh.team, who renewed and redeployed the server's certificate, closing a case we had opened: work on a server that is not ours, for the benefit of every client that connects there.

The project started from Piao Hong's MOD 17 of Newsreader, whose interface approach it follows.

Usenet Still Lives!
===================
MCSM (aka Marco)
