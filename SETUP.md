# 🔧 Environment Setup Guide

## Required Environment Variables

Set these environment variables before running the application:

### Google Cloud Setup
```bash
# Path to your service account JSON file
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/service-account.json"

# Your Google Sheet ID (found in the URL)
export GOOGLE_SHEET_ID="your-google-sheet-id-here"

# Optional: Sheet range (defaults to Sheet1!A:G)
export GOOGLE_SHEET_RANGE="Sheet1!A:G"
```

### Facebook API Setup
```bash
# Your Facebook Graph API access token
export FACEBOOK_ACCESS_TOKEN="your-facebook-access-token-here"

# Facebook page ID to monitor
export FACEBOOK_PAGE_ID="your-facebook-page-id-here"

# Optional: Sync interval in milliseconds (defaults to 5 minutes)
export FACEBOOK_SYNC_INTERVAL="300000"
```

## Configuration Files

1. **Copy the template**: `cp application.yml.template application.yml`
2. **Edit application.yml**: Replace placeholder values with your actual configuration
3. **Never commit**: `application.yml` (contains your actual tokens)
4. **Always commit**: `application.yml.template` (safe template)

## Security Notes

- ✅ **Safe to commit**: `application.yml.template`, `README.md`, `SETUP.md`
- ❌ **Never commit**: `application.yml`, `*.json` files, `.env` files
- 🔐 **Keep private**: Service account keys, API tokens, access keys

## Quick Setup Script

Create a `setup-env.sh` file (don't commit this):
```bash
#!/bin/bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/service-account.json"
export GOOGLE_SHEET_ID="your-google-sheet-id"
export FACEBOOK_ACCESS_TOKEN="your-facebook-access-token"
export FACEBOOK_PAGE_ID="your-facebook-page-id"

echo "Environment variables set successfully!"
```

Run with: `source setup-env.sh`
