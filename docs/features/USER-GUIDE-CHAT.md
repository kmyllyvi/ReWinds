# ReWinds Chat - User Guide

**Feature Status**: ✅ Available on Android and iOS
**Last Updated**: March 2, 2026

---

## What is the Chat Feature?

The **ReWinds Chat** is an AI-powered assistant that helps you analyze your saved weather locations and find ideal conditions for wind sports. Ask Claude (an AI assistant) questions about your saved weather data, and it will provide intelligent insights about wind patterns, best days, and conditions for kitesurfing or windsurfing.

---

## Getting Started

### Opening Chat

1. **On the Home Screen** - Look for the 💬 **Chat** button in the top-right header
2. **Tap it** - The chat screen opens
3. **Start typing** - Enter your question in the text field at the bottom
4. **Tap Send** - Watch Claude analyze your data and respond

### Closing Chat

**Tap the Back Button** (← at top-left) to return to the home screen. Your chat history is cleared when you navigate away (for now).

---

## What Can You Ask?

### Example Questions

**About Wind Conditions**:
- "What was the wind like in Tarifa last week?"
- "Show me days with wind speeds between 15 and 25 knots in November"
- "When did we get the strongest gusts in October?"
- "Was November good for kitesurfing at Tarifa?"

**About Multiple Locations**:
- "Compare wind conditions between Cabarete and Tarifa in December"
- "Which of my saved locations had the best wind last month?"

**About Specific Analysis**:
- "Find days with no rain and wind above 20 knots in the last month"
- "What's the average wind speed for December in Praia?"
- "How many good wind days did we have in March?"

**About Available Data**:
- "What places do I have saved?"
- "How much data do you have for my locations?"

### What Claude Knows

Claude has access to:
- ✅ All your **saved locations** (places you've downloaded weather for)
- ✅ **Wind data** (speed, gusts, sustained wind)
- ✅ **Temperature and precipitation** data
- ✅ **Monthly statistics** (min/max/average conditions)
- ✅ **Historical data** (whatever you've downloaded in ReWinds)

Claude does **NOT** have:
- ❌ Real-time weather (current conditions)
- ❌ Forecasts (future weather)
- ❌ Data you haven't downloaded yet (use "Download More Days" first)
- ❌ Private information or account details

---

## How It Works

### Behind the Scenes

When you ask a question:

1. **You send a message** - Your question goes to Claude's AI
2. **Claude thinks** - The AI reads your question and decides what data it needs
3. **Claude asks for data** - It automatically requests specific weather data from ReWinds
4. **ReWinds responds** - The app provides the requested data
5. **Claude analyzes** - The AI analyzes the data to answer your question
6. **You see the response** - Claude's answer appears in the chat

**You don't do any of this manually** - it all happens automatically. Claude figures out what data it needs and gets it for you.

### Real Example

**You**: "What was the wind like in Tarifa last week?"

**Claude**:
1. Recognizes you're asking about Tarifa
2. Requests wind data for "Tarifa" for the last 7 days
3. Gets daily wind speeds, gusts, and conditions
4. Analyzes the data
5. **Responds**: "Based on the data, Tarifa had consistent winds last week with an average of 18 knots and gusts up to 28 knots. Monday and Wednesday were the best days for kitesurfing with sustained 15-25 knot winds and no rain."

---

## Tips & Tricks

### Be Specific
- ✅ Good: "Show me days with wind 15-25 knots and no rain in November for Tarifa"
- ❌ Vague: "What about wind conditions?"

### Mention Dates
- ✅ Good: "Wind in Cabarete during December"
- ❌ Unclear: "Was it windy?" (which month/place?)

### Ask Follow-Up Questions
- You can ask follow-up questions in the same chat
- Claude remembers your previous questions
- Example: "What about October?" (after asking about November)

### Provide Context
- Mention the **place name** you're asking about
- Mention the **time period** (month, week, last 30 days, etc.)
- Claude is smart, but being specific helps

### Request Specific Analysis
- "Find days with..." - Claude will filter by your criteria
- "Compare..." - Claude will analyze multiple locations
- "What's the average..." - Claude will calculate statistics
- "When did..." - Claude will find specific dates

---

## What Happens When Things Go Wrong

### Message Shows "Error" or Times Out

**Possible reasons**:
1. **No internet connection** - Check your WiFi or cellular data
2. **API key not set** - The app doesn't have permission to use Claude
3. **API service down** - Anthropic's servers might be temporarily unavailable
4. **Invalid question** - Claude couldn't understand what you're asking

**What to do**:
1. **Tap the X** to dismiss the error
2. **Try again** - Ask a clearer question or try simpler wording
3. **Check internet** - Ensure you're connected
4. **Restart the app** - Sometimes a fresh start helps

### Claude Says "I Don't Have That Data"

**Why**: You haven't downloaded data for that location or time period yet.

**What to do**:
1. **Go to that location** in ReWinds
2. **Use "Download More Days"** to get the data you need
3. **Come back to Chat** and ask again

### Claude Seems Confused

**Why**:
- You asked about a location that doesn't exist in your saved list
- The time period doesn't have data
- The question is ambiguous

**What to do**:
1. **Check your saved locations** - Make sure you saved the place you're asking about
2. **Be more specific** - Include the exact location name and dates
3. **Ask what data is available** - "What places do I have?" or "How much data do you have for Tarifa?"

---

## Features & Limitations

### What Works Great
✅ Answering questions about your saved locations
✅ Finding days that match specific criteria
✅ Comparing conditions between locations
✅ Analyzing wind patterns and statistics
✅ Following up with related questions

### Current Limitations
⚠️ **No chat history saved** - Conversations are cleared when you leave the chat screen
⚠️ **Text only** - No images, files, or voice
⚠️ **Single source** - Only knows about data you've downloaded in ReWinds
⚠️ **No forecasts** - Can't predict future weather
⚠️ **20 turn limit** - Very long conversations may hit a limit (usually not an issue)

---

## FAQ

### Is the Chat Actually AI?
**Yes!** The chat uses Claude, an AI assistant made by Anthropic. It's the same technology used in popular AI assistants. It actually understands your questions and thinks through answers.

### Will My Data Be Private?
The chat system sends your location names and weather data to Anthropic's API. For production use, you should review Anthropic's [privacy policy](https://www.anthropic.com/privacy).

### Can I Save My Conversation?
Not yet. Chat history is currently cleared when you navigate away. Future updates may add persistent chat history.

### How Much Does This Cost?
The chat feature uses Anthropic's API, which is a paid service. The API charges per request. ReWinds does not charge for using chat, but you may incur Anthropic API costs depending on usage volume.

### Can I Ask About Other Topics?
The AI is optimized for analyzing your wind sports data, but you *can* ask other questions. It might not have the best answers outside of wind/weather analysis.

### What If I Don't Want to Use Chat?
No problem! The chat feature is optional. You can ignore it and use ReWinds normally. All core features (saved locations, weather download, statistics) work without chat.

### How Do I Get an API Key?
To use chat, your app needs an Anthropic API key. You can get one by:
1. Visiting [Anthropic's console](https://console.anthropic.com/)
2. Signing up or logging in
3. Creating an API key
4. Providing it to the app (ask the developer how to configure it)

---

## Troubleshooting

### Chat Button Doesn't Appear
- Ensure you're on the **Home Screen**
- Look in the **top-right header** area
- If still missing, try restarting the app

### Can't Send Messages
Possible reasons:
1. Text field is empty - Type something first
2. App is loading - Wait for the spinner to finish
3. Network issue - Check your connection
4. No API key - Ask your administrator/developer to set up the API key

### Responses Are Cut Off
This shouldn't happen, but if it does:
- The response might be too long
- Try asking a more specific question
- Future versions will handle long responses better

### Same Question Gets Different Answers
Claude is non-deterministic - it can give slightly different answers to the same question. This is normal AI behavior. All answers should be based on the same underlying data.

---

## Tips for Best Results

### 1. Download Your Data First
Before asking, make sure you've downloaded weather data for the locations you want to ask about. Chat can only analyze data you've already downloaded.

### 2. Use Location Names Exactly
If a place is saved as "Tarifa, Spain", use that exact name. If you're unsure, ask "What places do I have?"

### 3. Specify Time Periods
Instead of "Was it windy?", ask "Was December windy in Tarifa?" Time periods help Claude find the right data.

### 4. Ask Natural Questions
You don't need special syntax. Just ask like you're talking to a person:
- "Find me good kitesurfing days in January"
- "When was the windiest day in November?"
- "Compare conditions between Tarifa and Cabarete"

### 5. Follow Up with Details
If Claude's answer doesn't have enough detail, ask a follow-up:
- You: "What was the wind like last week?"
- Claude: *gives general answer*
- You: "Can you tell me the specific speeds for each day?"

### 6. Set the Context First
For long analysis, start by asking what data is available:
- "What places do I have data for?"
- "How much data do you have for Tarifa?"
- *Then* ask your actual questions

---

## What's Next?

### Planned Features
- 💾 **Save chat history** - Preserve conversations between sessions
- 🎬 **Streaming responses** - See Claude's answer as it's typed
- 📊 **Charts and visualization** - Visual analysis of wind patterns
- ⏰ **Scheduled suggestions** - Get notifications when conditions are ideal
- 🤖 **More specialized tools** - Dedicated analysis for forecasting and comparisons
- 🔐 **Secure API key storage** - Easier configuration without environment variables

### Give Feedback
Found a bug? Have a feature request? Share your feedback with the ReWinds team!

---

## Still Have Questions?

### For App Users
Ask the ReWinds support team or community for help.

### For Developers
See `/docs/AI-CHAT-FEATURE.md` for technical implementation details.

### For Anthropic API Issues
Check [Anthropic's documentation](https://docs.anthropic.com/) or support.

---

**Enjoy exploring your wind sports data with Claude! ⛵🪁**
