# Google Sheets Integration

This module provides Google Sheets integration for the Facebook Comment Fetcher application, allowing you to append comment data to a Google Sheet.

## Features

- **SheetsAppender Service**: Core service for appending rows to Google Sheets
- **Test Controller**: REST endpoints for testing and health checks
- **Application Default Credentials (ADC)**: Secure authentication using service account JSON
- **Configurable**: Sheet ID and range configurable via environment variables

## Configuration

### Environment Variables

Set these environment variables before running the application:

```bash
# Path to your service account JSON file
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/service-account.json"

# Google Sheet ID (found in the sheet URL)
export GOOGLE_SHEET_ID="your-sheet-id-here"

# Optional: Sheet range (defaults to Sheet1!A:G)
export GOOGLE_SHEET_RANGE="Sheet1!A:G"
```

### Application Properties

The following properties are automatically loaded from `application.yml`:

```yaml
google:
  sheetId: ${GOOGLE_SHEET_ID:}
  range: ${GOOGLE_SHEET_RANGE:Sheet1!A:G}
```

## Usage

### From Your Code

Inject the `SheetsAppender` service and call the `appendRow` method:

```java
@Autowired
private SheetsAppender sheetsAppender;

// Append a comment row
sheetsAppender.appendRow(
    "2025-08-30T10:00:00Z",  // timestamp
    "PAGE_123",               // pageId
    "COMMENT_456",            // commentId
    "John Doe",               // name
    "USER_789",               // fromId
    "This is a comment",      // message
    "+1234567890"             // phone
);
```

### Testing Endpoints

The application provides test endpoints for local testing:

#### Health Check
```bash
GET /sheets/health
```
Returns 200 OK if the Google Sheets API is accessible and the sheet is shared with your service account.

#### Append Test Row
```bash
POST /sheets/append
Content-Type: application/json

{
  "timestamp": "2025-08-30T10:12:00Z",
  "pageId": "TEST_PAGE",
  "commentId": "CMT_1",
  "name": "Alice",
  "fromId": "123",
  "message": "Hello +91 9876543210",
  "phone": "+919876543210"
}
```

## Setup Requirements

1. **Google Cloud Project**: Enable the Google Sheets API
2. **Service Account**: Create a service account and download the JSON key file
3. **Sheet Sharing**: Share your target Google Sheet with the service account's `client_email` as an Editor
4. **Environment Variables**: Set `GOOGLE_APPLICATION_CREDENTIALS` and `GOOGLE_SHEET_ID`

## Data Structure

The service appends rows with the following columns:

| Column | Description | Example |
|--------|-------------|---------|
| A | timestamp | 2025-08-30T10:00:00Z |
| B | pageId | PAGE_123 |
| C | commentId | COMMENT_456 |
| D | name | John Doe |
| E | fromId | USER_789 |
| F | message | This is a comment |
| G | phone | +1234567890 |

## Error Handling

The service includes comprehensive logging and error handling:
- Logs all operations with appropriate log levels
- Throws `IOException` for API failures
- Provides clear error messages for troubleshooting

## Dependencies

The following Google API dependencies are included:
- `google-api-services-sheets` - Google Sheets API v4
- `google-api-client` - Google API client core
- `google-auth-library-oauth2-http` - Authentication library
- `google-http-client-gson` - JSON factory (Gson)

## Testing

Run the unit tests:
```bash
mvn test -Dtest=SheetsAppenderTest
```

## Security Notes

- **Never commit** the service account JSON file to version control
- Use environment variables for sensitive configuration
- The service account should have minimal required permissions (Editor on the specific sheet only)
- Consider using Google Cloud IAM for production environments
