# Facebook Comment Fetcher

A Spring Boot application that fetches Facebook post comments and saves them to Google Sheets with intelligent tracking and optimization.

## 🚀 Features

- 🔍 **Facebook API Integration**: Fetches posts and comments from Facebook pages
- 📊 **Smart Comment Tracking**: Only processes new comments to avoid duplicates
- 💾 **Google Sheets Integration**: Saves combined post and comment data
- ⏰ **Automated Scheduling**: Configurable intervals for data synchronization
- 🎯 **Phone Number Extraction**: Automatically detects phone numbers in comments

## 🛠️ Tech Stack

- **Spring Boot 3.5.5**
- **Java 17**
- **Maven**
- **Google Sheets API v4**
- **Facebook Graph API**

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Facebook Developer Account
- Google Cloud Project with Sheets API enabled

## ⚙️ Configuration

### 1. Facebook API Configuration
```yaml
app:
  fb:
    page-id: YOUR_FACEBOOK_PAGE_ID
    access-token: YOUR_FACEBOOK_ACCESS_TOKEN
    api-version: v21.0
    fetch-interval-seconds: 60
```

### 2. Google Sheets Configuration
```yaml
google:
  sheetId: ${GOOGLE_SHEET_ID:}
  range: ${GOOGLE_SHEET_RANGE:Sheet1!A:G}
```

### 3. Environment Variables
```bash
# Facebook Configuration
export FB_PAGE_ID="your-page-id"
export FB_ACCESS_TOKEN="your-access-token"
export FB_API_VERSION="v21.0"
export FB_FETCH_INTERVAL="60"

# Google Sheets Configuration
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"
export GOOGLE_SHEET_ID="your-sheet-id"
export GOOGLE_SHEET_RANGE="Sheet1!A:G"
```

## 🚀 Setup Instructions

### 1. Facebook Setup
1. Create a Facebook app at [developers.facebook.com](https://developers.facebook.com)
2. Generate a page access token with required permissions
3. Note your page ID and access token

### 2. Google Sheets Setup
1. Create a Google Cloud project
2. Enable Google Sheets API
3. Create a service account and download JSON key
4. Share your target sheet with the service account email
5. Set environment variables

### 3. Application Setup
1. Clone the repository
2. Configure environment variables
3. Run: `./mvnw spring-boot:run`

## 📊 Data Structure

The application saves the following data to Google Sheets:

| Column | Description | Example |
|--------|-------------|---------|
| A | timestamp | 2025-08-30T10:00:00Z |
| B | pageId | 681857498348124 |
| C | commentId | 123456789_987654321 |
| D | name | John Doe |
| E | fromId | 123456789 |
| F | message | Hello, this is a comment |
| G | phone | +1234567890 |

## 🔄 How It Works

1. **Scheduled Execution**: Runs every configured interval (default: 60 seconds)
2. **Facebook API Call**: Fetches posts and comments from configured page
3. **Comment Tracking**: Identifies new comments using local tracking
4. **Data Processing**: Extracts relevant information including phone numbers
5. **Google Sheets**: Appends new comment data to configured sheet

## 🧪 Testing

### Health Check
```bash
curl http://localhost:8080/sheets/health
```

### Test Append
```bash
curl -X POST http://localhost:8080/sheets/append \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2025-08-30T10:12:00Z",
    "pageId": "TEST_PAGE",
    "commentId": "CMT_1",
    "name": "Alice",
    "fromId": "123",
    "message": "Hello +91 9876543210",
    "phone": "+919876543210"
  }'
```

## 📝 Notes

- **Phone Number Detection**: Uses regex to find phone numbers in comment messages
- **Timestamp Formatting**: Converts Facebook timestamps to ISO format
- **Error Handling**: Gracefully handles API failures and continues processing
- **Duplicate Prevention**: Tracks processed comments to avoid duplicates

## 🔒 Security

- Never commit service account JSON files
- Use environment variables for sensitive configuration
- Facebook access tokens should have minimal required permissions
- Google service account should only have access to specific sheets

## 📚 Additional Documentation

See [GOOGLE_SHEETS_README.md](GOOGLE_SHEETS_README.md) for detailed Google Sheets setup instructions.
