#!/usr/bin/env python3
"""
Simple script to create 100 test posts via API
Uses only standard library - no external dependencies
"""

import urllib.request
import urllib.error
import json
import time

# Configuration
API_URL = "http://localhost:8080/api/posts"  # Adjust if needed
AUTH_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTc2Njc0NjYxMCwiZXhwIjoxNzY3MzUxNDEwfQ.V1Ler9a1t1CgQqgBueMSxIEPX1L8ltz8fHohX1v9Zti9ACk-qgCQMsafQ2im4qsbgFU8S4D5-2B8prE57k84AA"
SUB_NAME = "news"  # The community name to post to

# Number of posts to create
NUM_POSTS = 100

def create_post(index):
    """Create a single post via API"""
    post_data = {
        "title": f"Test Post #{index} - Automated Testing",
        "content": f"This is test post number {index} created for testing purposes.",
        "subName": SUB_NAME,
        "postType": "TEXT",
        "nsfw": False,
        "spoiler": False
    }

    data = json.dumps(post_data).encode('utf-8')

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {AUTH_TOKEN}"
    }

    req = urllib.request.Request(API_URL, data=data, headers=headers, method='POST')

    try:
        with urllib.request.urlopen(req) as response:
            return True, response.status
    except urllib.error.HTTPError as e:
        return False, f"HTTP {e.code}: {e.reason}"
    except urllib.error.URLError as e:
        return False, str(e.reason)

def main():
    print(f"Creating {NUM_POSTS} posts...")
    print("-" * 40)

    success_count = 0
    fail_count = 0

    for i in range(1, NUM_POSTS + 1):
        success, status = create_post(i)

        if success:
            success_count += 1
            print(f"[{i}/{NUM_POSTS}] Created post #{i} - OK")
        else:
            fail_count += 1
            print(f"[{i}/{NUM_POSTS}] Failed post #{i} - {status}")

        # Small delay to avoid overwhelming the server
        time.sleep(0.1)

    print("-" * 40)
    print(f"Done! Success: {success_count}, Failed: {fail_count}")

if __name__ == "__main__":
    main()
