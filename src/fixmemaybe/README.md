# Fix Me Maybe: School Facility Repair System

OOP 101 project, Team NIGATRON (IT-2107). The backend is Java with no extra libraries. The web interface works on laptops and phones.

## How to run

1. Install **JDK 17 or newer**. To check, type `java -version` in a terminal.
2. **Windows:** double-click `run.bat`. **Mac/Linux:** run `./run.sh`.
3. Open **http://localhost:8080** on the laptop.
4. **Phones:** connect to the **same Wi-Fi** as the laptop. Then open the `https://...:8443` address that the server prints.
   The first time, the browser shows a security warning. Tap **Advanced → Proceed**. The certificate is your own laptop's, so this is safe. HTTPS is needed so the phone camera works.

**Easiest way:** open **http://localhost:8080/connect.html** on the laptop and scan the QR code with the phone.

> **Phone says "refused to connect" or "can't be reached"?**
> 1. Double-click **ALLOW-PHONE.bat** once and click **Yes**. It opens the Windows Firewall for ports 8080 and 8443.
> 2. Restart `run.bat`.
> 3. If it still fails, the Wi-Fi blocks phone-to-laptop connections (common on school Wi-Fi). Connect the laptop to your **phone's hotspot**, restart `run.bat`, and use the new address.

## Default admin login

| Email | Password |
|---|---|
| admin@fixmemaybe.com | admin123 |

## Easy editing: everything is in `config/app.properties`

| What | Key |
|---|---|
| Admin (authorized) emails | `admin.emails=a@x.com, b@y.com` |
| App name / tagline / motto | `app.name`, `app.tagline`, `app.motto` |
| School name | `school.name`, `school.subtitle` |
| Logo (white backgrounds) | `brand.logo` |
| Logo (maroon backgrounds) | `brand.logoLight` |
| School seal | `brand.schoolLogo` |
| Login background photo | `brand.loginBackground` |
| Phone splash background | `brand.splashBackground` |
| Colors | `brand.primaryColor`, `brand.primaryDark` |

**To use your own picture:** copy it into `web/assets/` (for example `campus.jpg`). Then set
`brand.loginBackground=assets/campus.jpg`. Save the file and refresh the browser. You don't need to restart.

You can change more colors and sizes in `web/css/theme.css`.

## How admin vs user works

* When someone logs in, the server checks their email against `admin.emails`.
* If the email is listed, the user becomes a `MaintenanceStaff` object and opens `admin.html`.
* Any other email becomes a `Student` or `Faculty` object and opens `user.html`.
* If an admin email has no account yet, the server creates one automatically with `admin.defaultPassword`.
* Adding or removing an email takes effect on the next login. You don't need to restart.

## Features

**Students and faculty (user dashboard)**
- Report a problem with the **phone camera** (live camera with flash, switch camera, retake) or from the **gallery**. Photos are compressed before upload.
- Report form fields: problem, location, facility type, date reported, and details.
- **My Requests**: search, filter by status, location and type, and page through results.
- **Request details**: photo viewer, info, and a **status history timeline**. A request can be cancelled while it is still "Reported".
- **Notifications** when the admin changes a request's status.
- **Profile**: edit info, change the profile photo, and change the password.

**Admin / maintenance staff (admin dashboard)**
- **Dashboard**: stat cards, requests per month chart, status donut, latest report with its photo, top locations and types.
- **All requests**: search and filters, plus **Update Status** (Reported → In Progress → Fixed) with notes. The reporter is notified.
- **Facilities**: add, edit or delete rooms. Each room's condition is calculated automatically.
- **Users**: list and filter users, **reset forgotten passwords**, and delete accounts.
- **Settings**: shows the admin emails, the data folder location, branding, **Export CSV** (opens in Excel), and Connect a Phone.

## Where the data is saved (local storage on the laptop)

```
data/users.json          accounts (passwords are hashed)
data/requests.json       repair requests + status history
data/facilities.json     rooms / places
data/notifications.json  bell notifications
data/uploads/            photos from phones (only logged-in users can view them)
data/keystore.p12        HTTPS certificate (created automatically)
```
To back up, copy the `data` folder. To reset everything, delete it.

## Project structure (OOP)

```
src/fixmemaybe/
  Main.java                         program start
  model/User.java                   ABSTRACT parent class
  model/Student.java                extends User
  model/Faculty.java                extends User
  model/MaintenanceStaff.java       extends User (admin)
  model/RepairRequest.java          submitRequest(), updateStatus(), displayRequest()
  model/Facility.java               displayFacility(), updateCondition()
  model/Status.java                 enum: Reported / In Progress / Fixed
  model/StatusUpdate.java           one history entry
  model/Notification.java
  model/UserFactory.java            builds the right subclass
  data/DataStore.java               saves/loads JSON files (ArrayLists)
  data/PhotoStorage.java            saves photos to data/uploads
  data/AppConfig.java               reads config/app.properties
  data/SessionManager.java          who is logged in
  service/AuthService.java          register / login / validation
  service/RequestService.java       submit, search/filter, status, stats, CSV
  service/FacilityService.java
  service/NotificationService.java
  service/UserService.java          profile, passwords, user management
  exception/InvalidInputException.java   custom exception
  server/...                        web server + API
web/                                pages, css, js, assets (pictures)
```

## OOP concepts used (for the presentation)

| Concept | Where |
|---|---|
| Classes & Objects | `RepairRequest`, `Facility`, `Student`... |
| Encapsulation | private fields + getters/setters in every model |
| Inheritance | `Student`, `Faculty`, `MaintenanceStaff` extend `User` |
| Polymorphism | `getDashboardPage()`, `getRole()`, `displayInfo()` are different for each user type |
| Abstraction | `abstract class User` |
| Exception Handling | `InvalidInputException`, try/catch in `ApiHandler` |
| Arrays / Collections | `ArrayList<User>`, `ArrayList<RepairRequest>` in `DataStore` |
