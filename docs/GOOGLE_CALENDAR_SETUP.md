# Google Calendar integration

Momento integrates with the Android Calendar Provider, not a bundled client secret or embedded OAuth flow. On a device with the Google Calendar app/account configured, selecting **Settings → Google Calendar → Connect Calendar** asks Android for `READ_CALENDAR` and `WRITE_CALENDAR`, then selects a writable calendar. The app stores only the selected calendar ID/name and a Room mapping between Momento event IDs and provider event IDs.

This makes export/update idempotent: subsequent syncs update only events created or imported by Momento. Disconnect retains Momento data and never deletes unrelated provider events. Local notifications do not depend on Calendar permission or connectivity.

There is no backend in this repository. Therefore Google Calendar webhooks and inactive-app server push are deliberately not claimed or implemented. For server-driven change delivery, deploy an authenticated HTTPS webhook, enable the Google Calendar API in a Google Cloud project, use OAuth consent/client configuration owned by the project, and deliver a trusted sync signal (for example FCM) to the app. Do not put client secrets, refresh tokens, or webhook URLs in the Android application.
