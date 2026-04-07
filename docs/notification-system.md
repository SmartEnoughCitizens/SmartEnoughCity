# Notification System

## Overview

The notification system delivers real-time in-app and email alerts to users. It supports SSE push, persistent read/unread state, soft-delete with a 30-day bin, and deep-link navigation from notification to the relevant dashboard view.

---

## What Was There Before

- `NotificationFacade.handleBackendNotification()` existed and could send email + in-app notifications.
- The frontend fetched notifications via `GET /api/notification/v1/{userId}` and displayed them in a basic list.
- Read state was **only optimistic** — marking a notification as read was not persisted to the database. On logout and re-login, all notifications appeared unread again.
- There was no soft-delete, no bin, no badge count seeded from real unread data, and no deep-link support.

---

## What Changed

### Backend

#### 1. Persistent Read State (`NotificationEntity`)

Added `is_read` column persistence. Two endpoints now exist, consolidated into one toggle:

```
PATCH /api/notification/v1/{userId}/{notificationId}/read?read=true|false
```

Both directions (read → unread, unread → read) are now persisted to the DB.

#### 2. Soft Delete / Bin

Added `deleted_at` column to `notifications` table (Hibernate auto-creates on restart).

| Endpoint | Description |
|---|---|
| `DELETE /api/notification/v1/{userId}/{notifId}` | Soft-delete (move to bin) |
| `PATCH /api/notification/v1/{userId}/{notifId}/restore` | Restore from bin |
| `GET /api/notification/v1/{userId}/bin` | Fetch bin contents |

Soft-deleted notifications are excluded from the main inbox query. A scheduled Spring job runs at 3am daily and hard-deletes bin entries older than 30 days:

```java
// NotificationFacade.java
@Scheduled(cron = "0 0 3 * * *")
@Transactional
public void purgeExpiredBin() {
    notificationRepository.hardDeleteExpiredBinEntries(LocalDateTime.now().minusDays(30));
}
```

`@EnableScheduling` is already on `HermesApplication`.

#### 3. `actionUrl` Field

Added `action_url` column to `notifications` table. When a notification is created with an `actionUrl`, it is stored and returned in the API response. The frontend uses it to show a "View in Dashboard" button that navigates the user directly to the relevant view + tab.

#### 4. Unread Count in API Response

`NotificationResponseDTO.totalCount` now returns the **unread count** (not total count), which seeds the bell badge on the frontend.

---

### Frontend

#### Badge

The bell icon in the sidebar shows a numeric badge (capped at `9+`) with the unread count. It is seeded from `totalCount` on login and incremented live via SSE events.

#### New Notification Banner

When an SSE event arrives, a dismissible blue banner appears at the top of the dashboard:

> You have new notifications. **View now** ✕

Clicking "View now" switches to the Notifications view. The banner only shows for live SSE events — not on page load.

#### Notifications Page

Full-width layout with two tabs:

**Inbox tab:**
- Unread notifications highlighted with a blue left dot and bold subject
- Per-row actions: toggle read/unread, move to bin
- "Mark all as read" button in header when unread items exist
- Clicking a row marks it read and opens a detail dialog

**Bin tab:**
- Info banner: *"Items in the bin are permanently deleted after 30 days"*
- Per-row restore button
- No delete-forever button needed — purge is automatic

**Detail dialog:**

- Renders body as **GitHub-flavoured markdown** via `react-markdown` + `remark-gfm` — tables, bold, bullet lists and horizontal rules all render correctly
- "View in Dashboard" button appears if `actionUrl` is set — navigates directly to the relevant view

---

## How Developers Onboard

Any backend service can send a notification by calling `NotificationFacade.handleBackendNotification()`. No other setup is required.

### Minimal example

```java
@RequiredArgsConstructor
public class YourService {

  private final NotificationFacade notificationFacade;

  public void notifyUser(String userId, String subject, String body) {
    BackendNotificationRequestDTO n = new BackendNotificationRequestDTO();
    n.setUserId(userId);       // Keycloak username
    n.setUserName(userId);
    n.setSubject(subject);
    n.setBody(body);
    n.setChannel(Channel.EMAIL_AND_NOTIFICATION); // or EMAIL, or NOTIFICATION
    notificationFacade.handleBackendNotification(n);
  }
}
```

### With a deep-link

Set `actionUrl` to a path that `DashboardLayout` understands (`?view=<view>&tab=<tab>`):

```java
n.setActionUrl("/dashboard?view=bus&tab=approvals");
```

The frontend will show a "View in Dashboard" button in the notification detail dialog. Clicking it dispatches a Redux `requestNavigation` action — no page reload, no URL routing needed.

### Sending to all users with a given role

```java
userManagementService.getUsersByRole("City_Manager").forEach(u -> {
    BackendNotificationRequestDTO n = new BackendNotificationRequestDTO();
    n.setUserId(u.getUsername());
    n.setUserName(u.getUsername());
    n.setSubject("...");
    n.setBody("...");
    n.setChannel(Channel.EMAIL_AND_NOTIFICATION);
    n.setActionUrl("/dashboard?view=train&tab=approvals");
    notificationFacade.handleBackendNotification(n);
});
```

`UserManagementService.getUsersByRole(roleName)` queries Keycloak's admin API and returns all users with that realm role.

### Channel options

| `Channel` | Effect |
|---|---|
| `NOTIFICATION` | In-app only (SSE + persisted to DB) |
| `EMAIL` | Email only (via SES) |
| `EMAIL_AND_NOTIFICATION` | Both |

### Frontend hooks (React)

If you add a new dashboard page that needs to display notifications or navigate from them:

```ts
// Read notifications
const { data } = useUserNotifications(username, true);

// Mark read/unread
const setReadState = useSetReadState(username);
setReadState(notificationId, true);  // or false

// Soft-delete
const softDelete = useSoftDeleteNotification(username);
softDelete(notificationId);

// Restore from bin
const restore = useRestoreNotification(username);
restore(notificationId);

// Navigate from a notification actionUrl
const params = new URLSearchParams(actionUrl.split("?")[1] ?? "");
dispatch(requestNavigation({ view: params.get("view") ?? "", tab: params.get("tab") ?? undefined }));
```

All hooks use React Query with optimistic updates — UI responds instantly, backend is updated in the background.

---

## Approval Service

The approval system is built on top of the notification system and is fully indicator-agnostic. Any indicator (train, bus, cycle, etc.) can raise an approval request with a single call.

### How it works

1. An indicator admin (e.g. `Train_Admin`) submits a request via `POST /api/v1/approvals`.
2. All `City_Manager` users receive an email + in-app notification with a direct link to the Approvals tab.
3. A `City_Manager` approves or denies via `PATCH /api/v1/approvals/{id}/review`.
4. The original requester receives an email + in-app notification with the decision.

### Backend onboarding

Inject `ApprovalService` and call `create()`:

```java
@RequiredArgsConstructor
public class YourIndicatorService {

  private final ApprovalService approvalService;

  public void submitForApproval(String userId, String payloadJson, String summary) {
    CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO();
    dto.setIndicator("bus");          // your indicator tag
    dto.setSummary(summary);          // shown to reviewers in notifications + table
    dto.setPayloadJson(payloadJson);  // arbitrary JSON — corridors, recommendations, etc.
    dto.setActionUrl("/dashboard?view=bus&tab=approvals"); // deep-link for notifications
    approvalService.create(userId, dto);
  }
}
```

That's it. The service handles:

- Persisting the request with status `PENDING`
- Notifying all `City_Manager` / `Government_Admin` users
- Role-aware listing (admins see own, managers see all, providers see approved only)
- Notifying the requester on decision

### Frontend onboarding

```ts
import { approvalApi } from "@/api/approval.api";

approvalApi.create({
  indicator: "bus",
  summary: "Reduce frequency on Route 46A during off-peak hours.",
  payloadJson: JSON.stringify({ route: "46A", change: "reduce_frequency" }),
  actionUrl: "/dashboard?view=bus&tab=approvals",
});
```

To display approvals in your dashboard, fetch with the indicator filter:

```ts
const { data: approvals } = useQuery({
  queryKey: ["approvals", "bus"],
  queryFn: () => approvalApi.list("bus"),
  enabled: onApprovalsTab,
});
```

### Receipt reference

Every approval gets a human-readable receipt number formatted as `APR-000042` (zero-padded to 6 digits from the DB id). Display it on the frontend:

```ts
`APR-${String(req.id).padStart(6, "0")}`
```

### Role matrix

| Role | Can submit | Can list | Can review |
| --- | --- | --- | --- |
| `Train_Admin`, `Bus_Admin`, etc. | yes (own indicator) | yes (own requests) | no |
| `City_Manager`, `Government_Admin` | yes | yes (all indicators) | yes |
| `*_Provider` | no | yes (APPROVED only) | no |

### API reference

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/approvals` | Submit a new request |
| `GET` | `/api/v1/approvals?indicator=train` | List (role-filtered) |
| `PATCH` | `/api/v1/approvals/{id}/review` | Approve or deny (managers only) |

---

## Architecture Summary

```
Backend service
    └── NotificationFacade.handleBackendNotification(dto)
            ├── Persist to notifications table (with action_url, is_read=false)
            ├── SSE push via SseManager → frontend bell badge + banner
            └── Email dispatch via SES (if channel includes EMAIL)

Frontend
    ├── DashboardLayout
    │     ├── useUserNotifications() → seeds badge from totalCount (unread)
    │     └── sseService.subscribe() → increments badge + shows banner
    └── NotificationsPage
          ├── Inbox tab  (active notifications, read/unread/delete)
          └── Bin tab    (soft-deleted, restore, 30-day purge warning)
```
