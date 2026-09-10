# Android Log Management System

A robust and feature-rich log management application for Android, designed for developers and power users who need to track, organize, and analyze logs efficiently. This project features both a native Android interface and an embedded web dashboard for remote access.

## 🚀 Features

### Native Android App
- **Modern UI**: Built entirely with **Jetpack Compose** and Material Design 3.
- **Log Management**: Create, edit, and delete logs with ease. Supports pinning important logs.
- **Categorization**: Organize logs into custom categories with unique colors.
- **Advanced Filtering**: Filter logs by search queries, levels (INFO, DEBUG, WARNING, ERROR, CRITICAL), categories, and date ranges.
- **Statistics**: Detailed analytics dashboard showing log distribution by level, category, and date.
- **Offline First**: All data is stored locally using **Room Database**.
- **Security**: Local user authentication with password hashing (BCrypt).

### Embedded Web Server (Remote Access)
- **Built-in Ktor Server**: Start a web server directly from your Android device.
- **Web Dashboard**: Access and manage your logs from any web browser on the same network.
- **Real-time Streaming**: Watch logs appear instantly via **Server-Sent Events (SSE)**.
- **Import/Export**: Easily import or export logs in **JSON** or **CSV** formats via the web interface.
- **File Attachments**: Upload and download attachments associated with log entries.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Local Storage**: Room Persistence Library
- **Networking/Server**: Ktor (CIO engine)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Security**: BCrypt for password hashing
- **Serialization**: Kotlinx Serialization & Gson
- **Concurrency**: Kotlin Coroutines & Flow

## 📸 Screenshots

*(Add your screenshots here)*

## 🚦 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/CodeNameAmir/android-log-management-system.git
   ```
2. **Open in Android Studio**: Load the project and wait for Gradle sync to complete.
3. **Build and Run**: Deploy the app to your Android device or emulator.
4. **Using the Web Server**:
   - Open the app and navigate to the server settings.
   - Start the server.
   - Note the IP address and port (e.g., `http://192.168.1.10:8080`).
   - Open that URL in your PC's browser to access the dashboard.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Developed with ❤️ by [CodeNameAmir](https://github.com/CodeNameAmir)
