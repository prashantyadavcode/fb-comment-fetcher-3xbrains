# 📘 Facebook Comment Fetcher

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A robust Spring Boot application that automatically fetches Facebook comments and stores them in Google Sheets. Perfect for social media monitoring, customer service tracking, and community engagement analysis.

## ✨ Features

- 🔄 **Automated Comment Fetching**: Scheduled retrieval of Facebook post comments
- 📊 **Google Sheets Integration**: Direct data storage in Google Sheets with real-time updates
- 🔐 **Secure Authentication**: Uses Google Cloud service accounts with Application Default Credentials
- 📱 **Phone Number Extraction**: Automatically extracts phone numbers from comment messages
- ⏰ **Timestamp Management**: Converts Facebook timestamps to ISO 8601 format
- 🚀 **RESTful API**: Built-in endpoints for testing and health monitoring
- 🔧 **Configurable**: Environment-based configuration with sensible defaults
- 📈 **Scalable**: Built with Spring Boot for enterprise-grade performance

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Facebook API  │───▶│  Spring Boot App │───▶│  Google Sheets  │
│                 │    │                  │    │                 │
│ • Graph API     │    │ • Scheduler      │    │ • Real-time     │
│ • Comments      │    │ • REST Controllers│   │ • Structured    │
│ • Posts         │    │ • Services       │    │ • Data Storage  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.9+**
- **Google Cloud Project** with Sheets API enabled
- **Facebook App** with Graph API access
- **Google Sheet** for data storage

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/fb-comment-fetcher.git
cd fb-comment-fetcher
```

### 2. Set Up Google Cloud

1. **Create a Google Cloud Project** (or use existing)
2. **Enable Google Sheets API**:
   ```bash
   gcloud services enable sheets.googleapis.com
   ```
3. **Create a Service Account**:
   ```bash
   gcloud iam service-accounts create fb-comment-fetcher \
     --display-name="Facebook Comment Fetcher"
   ```
4. **Download Service Account Key**:
   ```bash
   gcloud iam service-accounts keys create ~/Downloads/fb-comment-fetcher.json \
     --iam-account=fb-comment-fetcher@your-project.iam.gserviceaccount.com
   ```

### 3. Configure Environment Variables

```bash
# Google Cloud credentials
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/service-account.json"

# Google Sheet configuration
export GOOGLE_SHEET_ID="your-google-sheet-id"
export GOOGLE_SHEET_RANGE="Sheet1!A:G"  # Optional, defaults to Sheet1!A:G

# Facebook configuration
export FACEBOOK_ACCESS_TOKEN="your-facebook-access-token"
export FACEBOOK_PAGE_ID="your-facebook-page-id"
```

### 4. Share Google Sheet

Share your target Google Sheet with the service account email (found in the JSON key) with **Editor** permissions.

### 5. Run the Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or using Maven directly
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📊 Data Structure

Comments are automatically fetched and stored in Google Sheets with the following columns:

| Column | Description | Example |
|--------|-------------|---------|
| **A** | Timestamp | `2025-08-31T15:51:58.041+05:30` |
| **B** | Page ID | `681857498348124` |
| **C** | Comment ID | `122125978220938735_1475521886916656` |
| **D** | User Name | `John Doe` |
| **E** | User ID | `123456789` |
| **F** | Message | `Great product! Contact me at 555-1234` |
| **G** | Phone Number | `555-1234` |

## 🔧 Configuration

### Application Properties

```yaml
# application.yml
google:
  sheetId: ${GOOGLE_SHEET_ID:}
  range: ${GOOGLE_SHEET_RANGE:Sheet1!A:G}

facebook:
  accessToken: ${FACEBOOK_ACCESS_TOKEN:}
  pageId: ${FACEBOOK_PAGE_ID:}
  syncInterval: ${FACEBOOK_SYNC_INTERVAL:300000}  # 5 minutes
```

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to service account JSON | ✅ | - |
| `GOOGLE_SHEET_ID` | Target Google Sheet ID | ✅ | - |
| `GOOGLE_SHEET_RANGE` | Sheet range for data | ❌ | `Sheet1!A:G` |
| `FACEBOOK_ACCESS_TOKEN` | Facebook Graph API token | ✅ | - |
| `FACEBOOK_PAGE_ID` | Facebook page ID to monitor | ✅ | - |
| `FACEBOOK_SYNC_INTERVAL` | Sync interval in milliseconds | ❌ | `300000` (5 min) |

## 🧪 Testing

### Health Check

```bash
# Check Google Sheets connectivity
curl http://localhost:8080/sheets/health
```

### Manual Data Append

```bash
# Test appending a row
curl -X POST http://localhost:8080/sheets/append \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2025-08-31T15:51:58.041+05:30",
    "pageId": "test-page",
    "commentId": "test-comment",
    "name": "Test User",
    "fromId": "123456",
    "message": "Test message with phone 555-1234",
    "phone": "555-1234"
  }'
```

### Unit Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SheetsAppenderTest
```

## 📁 Project Structure

```
fb-comment-fetcher/
├── src/
│   ├── main/
│   │   ├── java/com/webhook_wrapper/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── facebook/         # Facebook API integration
│   │   │   ├── scheduler/        # Comment fetching scheduler
│   │   │   ├── sheets/           # Google Sheets integration
│   │   │   └── DemoApplication.java
│   │   └── resources/
│   │       └── application.yml   # Application configuration
│   └── test/                     # Unit tests
├── pom.xml                       # Maven dependencies
└── README.md                     # This file
```

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/sheets/health` | Google Sheets connectivity check |
| `POST` | `/sheets/append` | Manually append a row to Google Sheets |
| `GET` | `/actuator/health` | Application health status |

## 🚨 Troubleshooting

### Common Issues

1. **"Missing credentials" Error**
   - Ensure `GOOGLE_APPLICATION_CREDENTIALS` is set correctly
   - Verify the service account JSON file exists and is readable

2. **"Sheet not found" Error**
   - Check `GOOGLE_SHEET_ID` is correct
   - Ensure the service account has Editor access to the sheet

3. **"Facebook API error"**
   - Verify `FACEBOOK_ACCESS_TOKEN` is valid and not expired
   - Check `FACEBOOK_PAGE_ID` is correct

4. **Port 8080 already in use**
   ```bash
   # Kill process using port 8080
   kill -9 $(lsof -t -i:8080)
   ```

### Debug Mode

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    com.webhook_wrapper: DEBUG
    com.google.api: DEBUG
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - The web framework used
- [Google Sheets API](https://developers.google.com/sheets/api) - Data storage solution
- [Facebook Graph API](https://developers.facebook.com/docs/graph-api) - Social media data source

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/fb-comment-fetcher/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/fb-comment-fetcher/discussions)
- **Wiki**: [Project Wiki](https://github.com/yourusername/fb-comment-fetcher/wiki)

---

⭐ **Star this repository if you find it helpful!**
