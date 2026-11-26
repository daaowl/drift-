# Google Play store listing

**App name (30):** Sukoon: Duas for Every Moment

**Short description (80 max, this one is 72):**
Duas from Seeking Serenity, sorted by how you feel. Offline. No ads, ever.

**Full description:**

Sukoon puts every dua from the book *Seeking Serenity* in your pocket, sorted by what you're going through.

Feeling anxious? Grieving? Unwell? Grateful? Facing a big decision? Open the section for the moment and the right dua is one tap away, in Arabic, transliteration and English.

FIND THE RIGHT DUA, FAST
• Ten sections: Tranquility, Solace, Success, Knowledge, Love, Forgiveness, Protection, Health, Sustenance and Straight Path
• Search that understands spelling variants: "hasbunallah", "Hasbunallahu" and "anxiety" all find what you mean
• Search by reference too, like 2:201 or Bukhari 6682

READ IT YOUR WAY
• Beautiful Quranic Arabic typography with full harakat
• Turn Arabic, transliteration or translation on or off
• Adjust each text size separately, up to 200%
• Light, dark and true-black AMOLED themes, with an accent colour you choose

RECITE WITH A COUNT
• Built-in counter for duas recited 3 or 7 times
• Tap anywhere and feel each count; a gentle buzz when you finish a round

KEEP YOUR DUAS CLOSE
• Favourites and your own collections, like "Morning" or "For Ammi"
• Dua of the day, morning and evening reminders at times you pick
• Home screen widget showing today's dua or one you pin

PRIVATE BY DESIGN
• Works completely offline; the app doesn't even ask for internet access
• No account, no ads, no analytics, no tracking
• Optional app lock with fingerprint, face or PIN
• Back up and restore to a file you control

Available in English and Urdu.

The duas in Sukoon are from *Seeking Serenity*, compiled from the duas of the late Zubeida Mohamed. Proceeds from the book support the Nanima Foundation.

## Screenshot captions (5)

1. The right dua for how you feel, in one tap
2. Arabic, transliteration and translation, your way
3. Search that understands how you spell it
4. Count your recitations; feel every tap
5. Today's dua on your home screen. Fully offline.

## Feature graphic concept (1024 × 500)

Deep ink background (`#0E1320`) with the faint eight-point star lattice at about 6% gold. Left third: the app mark (gold eight-point star with a crescent) at about 220px. Right two-thirds: "Sukoon" in Fraunces at 96px in cream (`#ECE6D8`), with "Duas for every moment" beneath in Inter Medium at 36px in antique gold (`#D4AF6A`). A single line of Arabic, رَبِّ زِدْنِي عِلْمًا, set in Amiri Quran at 44px, 50% cream, along the bottom right. No device frames, no screenshots, no badges.

## Category and tags

Category: Lifestyle (alternative: Books & Reference). Tags: Religion, Spirituality, Islam.

## Content rating (IARC questionnaire) notes

• Violence, sexuality, language, controlled substances, gambling: none.
• User-generated content or user-to-user communication: none. Collection names stay on the device and are never shared.
• Location sharing, purchases, ads: none.
• Religious content: reference texts (supplications from the Quran and hadith). The questionnaire has no religious-content question; it doesn't affect the rating.
• Expected result: Everyone / PEGI 3 / USK 0.
• "Bodily Pain" and "All Diseases" mention illness in a supplication context only; nothing graphic.

## Permission justifications

| Permission | Why | Notes for review |
|---|---|---|
| `POST_NOTIFICATIONS` | Dua of the day and optional morning/evening reminders | Requested only after a rationale sheet, and only when the user turns a reminder on |
| `VIBRATE` | Counter haptics and round-completion feedback | Normal permission |
| `USE_BIOMETRIC` | Optional fingerprint/face unlock for App lock | Normal permission; Android handles biometric data |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `FOREGROUND_SERVICE` (merged from WorkManager) | Keep scheduled reminders working across reboots | Added by the AndroidX WorkManager library |

No exact-alarm, location, contacts, storage, camera, microphone or internet permission is used. `INTERNET` is explicitly removed in the manifest.

## Target audience

13+ and adults (not designed for children, but contains nothing unsuitable). Not part of the Families programme.
