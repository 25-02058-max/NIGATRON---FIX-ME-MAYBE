<h1 align="center">🔧 𝓕𝓲𝔁 𝓜𝓮 𝓜𝓪𝔂𝓫𝓮 🏫✨</h1>
<p align="center">OOP 101: Object-oriented Programming
<br/><i>Team NIGATRON (IT-2107)</i><br/><br/>
A Java-based School Facility Repair System where students, faculty, and maintenance staff report and track repair requests. Built with no external libraries to demonstrate fundamental Object-Oriented Programming (OOP) concepts.
</p>

<h2>‧₊˚ ┊ Overview</h2>
Students and faculty report broken or damaged facilities, with a photo taken on a phone or uploaded from a gallery. Maintenance staff review each report and move it through three statuses: Reported, In Progress, and Fixed. The reporter is notified whenever the status changes. The backend is written in Java, and the web interface works on both laptops and phones.

<h2>‧₊˚ ┊ OOP Concepts Demonstrated</h2>
<table>
	<tr>
		<th>OOP Concept</th>
		<th>How it was demonstrated</th>
	</tr>
	<tr>
		<td>𝐂𝐥𝐚𝐬𝐬𝐞𝐬 & 𝐎𝐛𝐣𝐞𝐜𝐭𝐬</td>
		<td>Real-world entities are modeled as classes such as <code>RepairRequest</code>, <code>Facility</code>, and <code>Student</code>. Each request, room, and user account exists as an object created at runtime.</td>
	</tr>
	<tr>
		<td>𝐄𝐧𝐜𝐚𝐩𝐬𝐮𝐥𝐚𝐭𝐢𝐨𝐧</td>
		<td>Every model class declares its fields as <code>private</code> and exposes them through getters and setters. Changes such as moving a request to a new status go through methods like <code>updateStatus()</code>, which keeps the data consistent.</td>
	</tr>
	<tr>
		<td>𝐈𝐧𝐡𝐞𝐫𝐢𝐭𝐚𝐧𝐜𝐞</td>
		<td><code>Student</code>, <code>Faculty</code>, and <code>MaintenanceStaff</code> extend the abstract <code>User</code> class. They reuse its shared account fields and add role-specific behavior.</td>
	</tr>
	<tr>
		<td>𝐏𝐨𝐥𝐲𝐦𝐨𝐫𝐩𝐡𝐢𝐬𝐦</td>
		<td>The methods <code>getDashboardPage()</code>, <code>getRole()</code>, and <code>displayInfo()</code> return different results for each user type. A <code>User</code> reference can point to any of the three subclasses, and the correct version runs at runtime.</td>
	</tr>
	<tr>
		<td>𝐀𝐛𝐬𝐭𝐫𝐚𝐜𝐭𝐢𝐨𝐧</td>
		<td><code>User</code> is an abstract class that defines the common structure of all accounts. It cannot be created directly, so only the concrete roles (<code>Student</code>, <code>Faculty</code>, <code>MaintenanceStaff</code>) are instantiated.</td>
	</tr>
	<tr>
		<td>𝐄𝐱𝐜𝐞𝐩𝐭𝐢𝐨𝐧 𝐇𝐚𝐧𝐝𝐥𝐢𝐧𝐠</td>
		<td>A custom <code>InvalidInputException</code> reports bad form data. <code>ApiHandler</code> catches it with try/catch and returns a clear error message to the browser.</td>
	</tr>
	<tr>
		<td>𝐀𝐫𝐫𝐚𝐲𝐬 / 𝐂𝐨𝐥𝐥𝐞𝐜𝐭𝐢𝐨𝐧𝐬</td>
		<td><code>DataStore</code> keeps users and requests in <code>ArrayList&lt;User&gt;</code> and <code>ArrayList&lt;RepairRequest&gt;</code>, and saves them to JSON files.</td>
	</tr>
</table>

<h2>‧₊˚ ┊ Project Structure</h2>

```
📂FixMeMaybe/
├── 📂config/
│   └── 📄app.properties          settings: admin emails, branding, colors
├── 📂src/fixmemaybe/
│   ├── 📄Main.java               program start
│   ├── 📂model/                  User, Student, Faculty, MaintenanceStaff,
│   │                             RepairRequest, Facility, Status, Notification...
│   ├── 📂data/                   DataStore, PhotoStorage, AppConfig, SessionManager
│   ├── 📂service/                AuthService, RequestService, FacilityService,
│   │                             NotificationService, UserService
│   ├── 📂exception/              InvalidInputException
│   └── 📂server/                 web server and API
├── 📂web/                        pages, css, js, and assets (pictures)
├── 📜run.bat                     start on Windows
├── 📜run.sh                      start on Mac / Linux
├── 📜ALLOW-PHONE.bat             opens Windows Firewall for phones
└── 📜.gitignore
```

<h2>‧₊˚ ┊ Requirements</h2>
<ul type = "circle">
	<li>Java Development Kit (JDK) 17 or newer</li>
	<li>A web browser on the laptop, and a phone on the same Wi-Fi for phone access</li>
</ul>

<h2>‧₊˚ ┊ How to Run the Program</h2>
<ol type = "1">
<li>Clone or download this repository.</li>
<li>Check your Java version by typing <code>java -version</code> in a terminal.</li>
<li>Start the server:
	<ul>
		<li><b>Windows:</b> double-click <code>run.bat</code></li>
		<li><b>Mac / Linux:</b> run <code>./run.sh</code></li>
	</ul>
</li>
<li>Open <code>http://localhost:8080</code> on the laptop.</li>
</ol>

<h2>‧₊˚ ┊ Using a Phone</h2>
1. Connect the phone to the **same Wi-Fi** as the laptop.
2. Open the `https://...:8443` address that the server prints.
3. The first time, the browser shows a security warning. Tap **Advanced → Proceed**. The certificate belongs to your own laptop, so this is safe. HTTPS is required so the phone camera works.

**Easiest way:** open `http://localhost:8080/connect.html` on the laptop and scan the QR code with the phone.

> **Phone says "refused to connect" or "can't be reached"?**
> 1. Double-click **ALLOW-PHONE.bat** once and click **Yes**. It opens ports 8080 and 8443 in the Windows Firewall.
> 2. Restart `run.bat`.
> 3. If it still fails, the Wi-Fi may block phone-to-laptop connections (common on school Wi-Fi). Connect the laptop to a phone hotspot, restart `run.bat`, and use the new address.

<h2>‧₊˚ ┊ Features</h2>

**Students and faculty**
- Report a problem with the phone camera (flash, switch camera, retake) or choose a photo from the gallery. Photos are compressed before upload.
- Fill in the problem, location, facility type, date reported, and details.
- **My Requests:** search, filter by status, location, and type, and page through results.
- **Request details:** photo viewer, request info, and a status history timeline. A request can be cancelled while it is still "Reported."
- **Notifications** when the status changes.
- **Profile:** edit info, change the profile photo, and change the password.

**Admin / maintenance staff**
- **Dashboard:** stat cards, requests per month chart, status donut, latest report, and top locations and types.
- **All requests:** search, filters, and **Update Status** (Reported → In Progress → Fixed) with notes.
- **Facilities:** add, edit, or delete rooms. Each room's condition is calculated automatically.
- **Users:** list and filter users, reset forgotten passwords, and delete accounts.
- **Settings:** admin emails, data folder location, branding, **Export CSV** (opens in Excel), and Connect a Phone.

<h2>‧₊˚ ┊ Default Admin Login</h2>

| Email | Password |
|---|---|
| admin@fixmemaybe.com | admin123 |

<h2>‧₊˚ ┊ Configuration</h2>

Most settings live in `config/app.properties`. Changes take effect without restarting the server, except where noted.

| What | Key |
|---|---|
| Admin (authorized) emails | `admin.emails=a@x.com, b@y.com` |
| App name, tagline, motto | `app.name`, `app.tagline`, `app.motto` |
| School name | `school.name`, `school.subtitle` |
| Logo (white backgrounds) | `brand.logo` |
| Logo (maroon backgrounds) | `brand.logoLight` |
| School seal | `brand.schoolLogo` |
| Login background photo | `brand.loginBackground` |
| Phone splash background | `brand.splashBackground` |
| Colors | `brand.primaryColor`, `brand.primaryDark` |

**To use your own picture:** copy it into `web/assets/` (for example `campus.jpg`), set `brand.loginBackground=assets/campus.jpg`, save, and refresh the browser.

More colors and sizes can be changed in `web/css/theme.css`.

**How admin vs. user is decided:** when someone logs in, the server checks their email against `admin.emails`. Listed emails become `MaintenanceStaff` accounts and open `admin.html`. All other emails become `Student` or `Faculty` accounts and open `user.html`.

<h2>‧₊˚ ┊ Where the Data Is Saved</h2>

```
data/users.json          accounts (passwords are hashed)
data/requests.json       repair requests and status history
data/facilities.json     rooms and places
data/notifications.json  bell notifications
data/uploads/            photos from phones (logged-in users only)
data/keystore.p12        HTTPS certificate (created automatically)
```

To back up, copy the `data` folder. To reset everything, delete it.

<h2>‧₊˚ ┊ Learning Purpose</h2>
This project was built for OOP 101 to show how Java OOP concepts apply to a practical problem: reporting and tracking repairs in a school. It uses no external libraries, so every concept is written out in plain Java.

<h2>‧₊˚ ┊ Team Members</h2>

***Jaryll Cortez***

***Giuseppe Palad***

***Nathaniel Villegas***
