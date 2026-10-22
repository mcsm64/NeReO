# NeReO

[![Download NeReO](https://a.fsdn.com/con/app/sf-download-button)](https://sourceforge.net/projects/nereo/files/latest/download)

[![Download NeReO](https://img.shields.io/sourceforge/dw/nereo.svg)](https://sourceforge.net/projects/nereo/files/latest/download)

**Ne(ws) Re(ader) O(pen-source)** — a free, open source, **text-only** Usenet (NNTP) newsreader for Android. No binaries, no ads, no tracking. GPL-3.0-or-later.

Written from scratch in Kotlin + Jetpack Compose. Its interface follows Piao Hong's *Newsreader* (MOD 17); its goal is to combine that familiarity with the depth of configuration of MesNews.

**Latest version: 1.5.7** (versionCode 175) — 22 October 2026 · Android 8.0 or later

## Download

Official APKs are published on SourceForge: **<https://sourceforge.net/projects/nereo/files/>**

Each version folder also contains the full README in English and Italian, the user manual and the filters guide.

If you are updating, install the new APK over the existing one: do **not** uninstall first, or you will lose groups, read state and settings.

## Features

- **Multiple NNTP servers** with implicit SSL/TLS **or STARTTLS** (RFC 3977, RFC 4643, RFC 4642) and authentication, **certificate host name verification** (RFC 6125), automatic IPv6/IPv4 fallback; an **alarm if a server that used to offer STARTTLS stops advertising it** (RFC 4642 §5)
- Search in subject, author and body, with an excerpt when the match is in the text
- Group search and subscription; incremental header sync with `OVER`, falling back to `XOVER` on servers that do not recognise `OVER`
- Thread views: tree (JWZ algorithm on References), sequential, grouped; continuous thread view
- **Real multiple identities**: name, e-mail, signature, User-Agent, X-No-Archive, custom headers, quote introduction and Face, each one assignable to a specific server
- Writing: replies with quoting (four modes), **"Quote" and "Quote and reply" from a text selection**, crossposting, signatures, local drafts **that save themselves**
- RFC 2047 headers, quoted-printable, base64, multiple charsets; **Face** (PNG, as in MesNews) for reading and sending, black-and-white **X-Face** for reading; coloured initials when an author has neither; **Format=Flowed** (RFC 3676) for reading, optional for sending
- **Customisable reading bars** in both views: twenty-one actions, each movable between bar and menu
- **"Keep this message"**: writes a message as an `.eml` file into a folder you choose once
- **Score-based filter engine** in the slrn/Gnus style: contains / wildcard / regex on From, Subject, Message-ID, Xpost, Body, newsreader and any header, plus "nick = address" against sockpuppets; extension to replies (hidden or highlighted); per-group scope (wildmat), expiry, kill/hide/highlight/watch thresholds; removing a rule undoes its effect; text kill-file import/export
- Configuration backup and restore, with optional encryption
- Material 3, light/dark/system theme, dynamic colour, text tints and sizes, panel layout for wide screens
- Interface in **English and Italian**, with an in-app language switch

## Documentation in this repository

- [`README_EN.txt`](https://github.com/mcsm64/NeReO/blob/main/README_EN.txt) — full English README, with the notes of every version
- [`README_IT.txt`](https://github.com/mcsm64/NeReO/blob/main/README_IT.txt) — the same in Italian
- [`README.md`](https://github.com/mcsm64/NeReO/blob/main/README.md) — developer notes (in Italian)

## Licence and name

The source code is licensed under the **GNU General Public License v3 or later** — see [`LICENSE`](https://github.com/mcsm64/NeReO/blob/main/LICENSE).

The name **"NeReO"** and its logo are reserved by the author (MCSM): modified versions must use a different name and a different Android applicationId (the original is `org.openusenet.reader`). See [`TRADEMARK.txt`](https://github.com/mcsm64/NeReO/blob/main/TRADEMARK.txt).

## Reports

For a useful report, three things are enough: what you were doing when it happened; a screenshot, if the problem is visible; the block from **About → Technical data** (there is a Copy button).
