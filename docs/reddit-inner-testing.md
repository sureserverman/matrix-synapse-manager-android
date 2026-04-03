# Reddit post — Inner testing on Google Play (volunteers needed)

Use the text below to announce **internal/closed testing** of Matrix Synapse Manager on Google Play and ask for volunteer testers. Target: [r/androidapps](https://www.reddit.com/r/androidapps/) and [r/matrixprotocol](https://www.reddit.com/r/matrixprotocol/).

**Before posting**

1. **Get your testing link**  
   In Play Console: **Release** → **Testing** → **Internal testing** (or **Closed testing**) → open the track → copy the **opt-in link** (the one testers use to join and install the app). Paste it into the body where indicated below.

2. **Subreddit rules**  
   Check each sub’s rules and sticky posts (e.g. self-promo, beta/feedback posts). r/androidapps and r/matrixprotocol often allow “looking for testers” if it’s clear and not spammy.

**Reddit etiquette (same as other project posts)**

- Use a **different title** per sub so it doesn’t look like the same post copied everywhere.
- **90/10 rule:** Most of your activity should be normal participation; a small share can be your project.
- Reply to comments and be upfront that you’re the developer.

---

## For r/androidapps

**Audience:** Android users, people who try new apps, power users. Focus: Android app on Play, need testers for internal/closed testing.

**Title (pick one or tweak):**

- `[Looking for testers] Matrix Synapse Manager — Android app for Synapse admins, now in internal testing on Play`
- `Android app to manage Matrix/Synapse servers from your phone — need volunteers for Play Store testing`
- `Synapse admin app for Android (users, rooms, media, federation) — internal test on Google Play, testers welcome`

**Body:**

I’m running **internal testing** of **Matrix Synapse Manager** on Google Play and I’m looking for volunteers to try it and give feedback.

**What it is:** An Android app for people who run a Matrix/Synapse homeserver. You can manage users (list, create, lock/unlock, deactivate), devices and sessions, rooms (block, delete, members), media (quarantine, delete, bulk actions), federation, server dashboard, background jobs, and event reports. Multi-server: add several Synapse instances and switch between them. Auth is per server; tokens are stored in Android Keystore, passwords are never stored. No telemetry.

**What I need:** If you have (or can get) admin access to a Synapse server and an Android device, you can join the test, install from the Play Store link, and report any bugs or UX issues. You’ll need a server that exposes the Admin API (and your admin credentials) to use the app.

**Join the test (opt-in link):**  
**[[https://play.google.com/apps/internaltest/4701408080466460136](https://play.google.com/apps/internaltest/4701408080466460136)]**

Source (BSD-2-Clause): https://github.com/sureserverman/matrix-synapse-manager-android

Thanks in advance to anyone who tries it — happy to answer questions here or on GitHub.

---

## For r/matrixprotocol

**Audience:** Matrix users, Synapse admins, server operators. Focus: Matrix/Synapse ecosystem, admin tool, need testers before wider release.

**Title (pick one or tweak):**

- `Android app for Synapse admins (users, rooms, media, federation, jobs) — internal testing on Play, need volunteer testers`
- `[Testers wanted] Matrix Synapse Manager — mobile admin app for Synapse, now in closed testing on Google Play`
- `Synapse admin panel on Android — looking for testers for Play Store internal track`

**Body:**

I’ve built an Android app for administering Synapse homeservers and I’m doing **internal testing** on Google Play. I’m looking for volunteer testers who run (or have access to) a Synapse server.

**What it does:** Full admin from your phone: users (list, search, create, lock/unlock, suspend, deactivate with optional media wipe), devices and sessions (view, revoke, whois), rooms (list, block, delete, members, set admin), media (list by room/user, quarantine, delete, bulk purge), federation (list, reset), server dashboard (version, metrics), background jobs (updates status, pause/resume, start), and event reports (list, dismiss). Multi-server via `.well-known` or manual URL; tokens in Keystore, no password storage, no telemetry. Uses the Synapse Admin API only.

**What I need:** If you’re a Synapse admin with an Android device, you can join the test via the link below, install from Play, and report bugs or UX feedback. You’ll need a server with the Admin API available and your admin credentials.

**Join the test (opt-in link):**  
**[[https://play.google.com/apps/internaltest/4701408080466460136](https://play.google.com/apps/internaltest/4701408080466460136)]**

Source (BSD-2-Clause): https://github.com/sureserverman/matrix-synapse-manager-android

Thanks to anyone who helps test — questions welcome here or on GitHub.

---

## Short version (for rooms / social)

**Matrix Synapse Manager** — Android app for Synapse admins (users, rooms, media, federation, jobs, moderation). Internal testing on Google Play; need volunteer testers. Opt-in: [your Play testing link]. Source: https://github.com/sureserverman/matrix-synapse-manager-android

---

## Checklist before posting

- [ ] Play Console internal or closed test is set up and the app is uploaded.
- [ ] Opt-in link copied from Play Console and pasted into the post body (replace the placeholder).
- [ ] Subreddit rules checked for r/androidapps and r/matrixprotocol.
- [ ] Title chosen (and slightly different per sub).
- [ ] Optional: one screenshot (e.g. dashboard or user list) if the sub allows images.
