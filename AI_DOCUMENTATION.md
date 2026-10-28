# Trello App: AI Bot Integration Documentation

This document explains exactly how the AI Assistant was built, configured, and integrated into this Android app. It covers every detail of the dependencies, the system prompt engineering, how the AI parses JSON to take actions, and the solution to the API key errors encountered during development.

---

## 1. Dependencies and Setup
To communicate with Google's Gemini AI natively on Android, we added specific dependencies to our Gradle files.

### 1.1 `build.gradle.kts` (Project Level)
```kotlin
buildscript {
    dependencies {
        // We added this to safely manage API keys.
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
    }
}
```

### 1.2 `app/build.gradle.kts` (App Level)
```kotlin
plugins {
    // Applying the secrets plugin
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    buildFeatures {
        buildConfig = true // Enabled so we can access BuildConfig.GEMINI_API_KEY
    }
}

dependencies {
    // The official Google AI SDK for Android. This provides the `GenerativeModel` class.
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}
```
**Why we used the Secrets Plugin:** We added this so that you can put your `GEMINI_API_KEY=...` inside the `local.properties` file (which is ignored by Git). The plugin automatically reads that file and injects the key into a secure `BuildConfig` variable that the app can read.

---

## 2. Why the Model Didn't Work Initially (The 404 Error)

When we first integrated the AI, you received a persistent `404 Not Found` error stating:
`models/gemini-1.5-flash is not found for API version v1beta...`

### The Problem:
1. Older Google AI Studio API keys usually start with `AIzaSy...`. 
2. You generated a brand new key that started with `AQ...`. This is a newer Google API key format.
3. Google recently updated their backend routing. The new `AQ.` format keys **do not** have access to the old model alias `gemini-1.5-flash` or `gemini-pro`. 

### The Solution:
By running direct curl/PowerShell tests against Google's servers, we discovered that new keys starting with `AQ.` are specifically bound to a new model alias: **`gemini-flash-latest`**.
Changing the model string inside the repository fixed the 404 error instantly.

---

## 3. The Architecture (Code Flow)

We used **Clean Architecture** to build this feature. The code is separated into three layers.

### Step 1: The Data Layer (`AiRepositoryImpl.kt`)
This is where we define the exact model we are talking to.
```kotlin
class AiRepositoryImpl @Inject constructor() : AiRepository {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-flash-latest", // <-- The exact model string we discovered fixes the AQ. key issue
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    
    override suspend fun getGeneralAssistantResponse(prompt: String): Result<String> {
        // This is the actual network call to Google
        val response = generativeModel.generateContent(prompt) 
        return Result.success(response.text ?: "")
    }
}
```

### Step 2: The Domain Layer (`InteractWithAiUseCase.kt`)
This is the "Brain" of the AI. Instead of just sending the user's text straight to Google, this UseCase intercepts it, injects context, and forces the AI to output JSON if an action is required.

**How we add Context (Parameters):**
Before we talk to the AI, we talk to the database (`trelloRepository`) to see what boards exist.
```kotlin
val boards = trelloRepository.observeBoards("").firstOrNull() ?: emptyList()
val boardsContext = "The user currently has the following boards: " + boards.joinToString(", ") { it.title }
```

**How we force JSON Output (System Prompt):**
We combine the context and the user's prompt into a master "System Prompt". We literally tell the AI exactly how it must behave.
```kotlin
val systemPrompt = """
    You are a helpful project management AI assistant in a Trello-like app.
    $boardsContext // <-- We inject the context parameter here!
    
    The user says: "$prompt"
    
    If the user explicitly asks to CREATE a new board, you MUST reply with a JSON object in this exact format:
    {
       "action": "CREATE_BOARD",
       "title": "The Board Title",
       "description": "The Board Description",
       "lists": ["List 1 Name", "List 2 Name"]
    }
    
    If the user is NOT asking to create something, reply with normal text.
    DO NOT return JSON if they just want advice.
""".trimIndent()
```
*Note: To add more params in the future, you simply fetch the data (like tasks or cards) and append it to this `systemPrompt` string using standard Kotlin string interpolation (`$variable`).*

**How we execute the JSON Action:**
When the AI returns the JSON, it often wraps it in markdown code blocks like ````json ... ````. We must strip those out, parse the JSON, and then call our local database to actually create the board!
```kotlin
// 1. Clean the markdown
val cleanResponse = rawResponse.replace("```json", "").replace("```", "").trim()

// 2. Parse JSON
val json = JSONObject(cleanResponse)
if (json.optString("action") == "CREATE_BOARD") {
    // 3. Take Action!
    val title = json.optString("title", "New Board")
    val boardId = trelloRepository.createBoard(title = title, description = "", colorHex = "#4CAF50")
    
    // 4. Create Dynamic lists!
    val listsArray = json.optJSONArray("lists")
    for (i in 0 until listsArray.length()) {
        trelloRepository.createList(boardId = boardId, title = listsArray.getString(i))
    }
}
```

### Step 3: The Presentation Layer (`AiAssistantViewModel.kt`)
The ViewModel simply takes the user's text from the UI, passes it to the UseCase, and updates the screen state based on the result. It has no idea about JSON or API keys; it just says "Loading..." and then shows the "Success" string that the UseCase returns.

```kotlin
class AiAssistantViewModel @Inject constructor(
    private val interactWithAiUseCase: InteractWithAiUseCase
) : ViewModel() {
    fun sendMessage(prompt: String) {
        _uiState.value = AiAssistantUiState.Loading
        val result = interactWithAiUseCase(prompt) // Calls the brain
        // ... updates state with result
    }
}
```

---

## Summary of the Flow
1. **User types:** "Create a marketing board with a To Do list."
2. **ViewModel** passes text to **InteractWithAiUseCase**.
3. **UseCase** fetches current boards from the DB.
4. **UseCase** builds a giant prompt with instructions, context, and the user's text.
5. **AiRepository** sends the giant prompt to the `gemini-flash-latest` model at Google.
6. **Google** returns: ````json { "action": "CREATE_BOARD", "title": "Marketing", "lists": ["To Do"] } ````.
7. **UseCase** strips the ````json` text, parses it, and fires `trelloRepository.createBoard()` and `trelloRepository.createList()`.
8. **UseCase** returns a string to the **ViewModel**: "Board created!"
9. **UI** displays: "Board created!" and the new board appears on the screen.

---

## 4. Line-by-Line Code Breakdown

Below is a detailed list of every new file added to the project for the AI Assistant, along with an explanation of every function (starting with `fun`) and what its exact purpose is.

### File: `AiRepository.kt`
This is the interface (contract) for our AI layer.
- `fun getGeneralAssistantResponse(prompt: String): Result<String>`
  **Purpose**: Defines the method signature that takes a `prompt` string and returns a Kotlin `Result` wrapped around a `String`. A `Result` is used so we can safely catch failures (like network errors) without crashing the app.

### File: `AiRepositoryImpl.kt`
This is the concrete implementation of the repository that actually talks to Google using the SDK.
- `override suspend fun getGeneralAssistantResponse(prompt: String): Result<String>`
  **Purpose**: This `suspend` function switches to a background thread. It takes the string prompt, passes it directly to `generativeModel.generateContent(prompt)`, and returns the AI's response text. It is wrapped in a try/catch to return `Result.success()` or `Result.failure()` depending on if Google's servers accept the request.

### File: `InteractWithAiUseCase.kt`
This is the brain (Domain layer) that intercepts the user's input, attaches database context, constructs the massive System Prompt, and handles the resulting JSON action.
- `suspend operator fun invoke(prompt: String): Result<String>`
  **Purpose**: The `invoke` operator allows us to call the UseCase class exactly like a function (e.g. `interactWithAiUseCase("my text")`). 
  - **Line purpose (Fetch boards)**: It first calls `trelloRepository.observeBoards("").firstOrNull()` to get the current state of the database.
  - **Line purpose (System Prompt)**: It creates a raw string `""" ... """` containing strict instructions, the database context, and the user's prompt.
  - **Line purpose (Network Call)**: It calls `aiRepository.getGeneralAssistantResponse(systemPrompt)` to get the AI's answer.
  - **Line purpose (Markdown Cleaning)**: It runs `.replace("```json", "")` to strip away any formatting the AI might have accidentally added, ensuring it's pure text.
  - **Line purpose (JSON Parsing)**: It checks if the string `startsWith("{")` and parses it using `JSONObject(cleanResponse)`.
  - **Line purpose (Database Action)**: If the action is `CREATE_BOARD`, it extracts the `title` and `lists` array, then calls `trelloRepository.createBoard` and `trelloRepository.createList` to insert them into the local database, returning a success message to the user instead of the raw JSON.
  - **Line purpose (Exception Handling)**: The massive `catch (e: Exception)` block looks at `e.localizedMessage`. If it sees "404" or "API_KEY_INVALID", it returns a highly specific, readable error string to the user explaining that their API key is blocked, rather than showing an ugly red crash log.

### File: `AiAssistantViewModel.kt`
This manages the state (UI State) for the screen. It doesn't know anything about Trello or JSON, it just holds the state (Loading, Success, Error).
- `fun sendMessage(prompt: String)`
  **Purpose**: This is called by the UI when the user clicks the send button. 
  - It first checks if the prompt is blank. 
  - Then it launches a coroutine (`viewModelScope.launch`) and immediately sets `_uiState.value = AiAssistantUiState.Loading` to show a progress spinner. 
  - It calls `interactWithAiUseCase(prompt)` and waits for the result.
  - If successful, it sets the state to `Success(response)`. If failed, it sets the state to `Error(error.message)`.
- `fun reset()`
  **Purpose**: Sets the UI state back to `Idle`. This is used to clear the chat box when the user closes and reopens the dialog.

### File: `AiAssistantDialog.kt`
This contains all the Jetpack Compose UI code for the bottom sheet popup.
- `@Composable fun AiAssistantDialog(...)`
  **Purpose**: This is the main Compose function that draws the popup on the screen. It takes the ViewModel as a parameter, observes `viewModel.uiState.collectAsState()`, and draws the text field, the send button, and the response box (or loading spinner) depending on the state.
- `@Composable fun ChatMessage(message: String, isUser: Boolean, isError: Boolean = false)`
  **Purpose**: A helper Compose function that draws a single chat bubble. It takes a boolean `isUser` to determine if it should align the bubble to the right (blue) or left (gray). If `isError` is true, it colors the text red so the user clearly sees there is an API problem.

---

## 5. How to Expand the AI's Capabilities (Adding Colors, Cards, etc.)

If you want the AI to do more advanced things (like picking a custom board color, or creating cards inside specific lists), you only need to edit **ONE** file: `InteractWithAiUseCase.kt`.

To add new parameters or actions, follow these 3 exact steps inside `InteractWithAiUseCase.kt`:

### Step 1: Give the AI more context
If the AI needs to know about existing Lists or Cards in order to modify them, you must fetch them from the database before you create the system prompt, and add them to the context string.
```kotlin
// Example: Fetching lists for context
// val allLists = trelloRepository.observeAllLists().firstOrNull() ?: emptyList()
// val listsContext = "The user has these lists: " + allLists.joinToString { it.title }
```
Then, you inject that `$listsContext` variable directly into the `systemPrompt` string right next to `$boardsContext`.

### Step 2: Update the JSON Instructions
Inside the `systemPrompt` string, you must update the rigid JSON format you demand from the AI. Add your new parameters as JSON keys, and leave comments so the AI understands them.

```kotlin
// Example of an updated prompt instructing the AI how to return colors and cards:
"""
If the user explicitly asks to CREATE a new board, you MUST reply with a JSON object in this exact format:
{
   "action": "CREATE_BOARD",
   "title": "The Board Title",
   "colorHex": "#FF0000", // Choose a hex color based on the topic!
   "lists": [
       {
           "title": "To Do",
           "cards": ["Task 1", "Task 2"] // Optional array of cards for this list
       }
   ]
}
"""
```

### Step 3: Write the Code to Execute the Action
Scroll down to the bottom of the file where we parse the JSON (inside `if (json.optString("action") == "CREATE_BOARD")`). You need to add the Kotlin code to extract your new JSON fields and actually fire the database commands.

```kotlin
// Example of extracting the new color and cards:
val colorHex = json.optString("colorHex", "#4CAF50") // Fallback to default green
val boardId = trelloRepository.createBoard(title = title, description = "", colorHex = colorHex)

val listsArray = json.optJSONArray("lists")
if (listsArray != null) {
    for (i in 0 until listsArray.length()) {
        val listObj = listsArray.getJSONObject(i)
        val listTitle = listObj.optString("title")
        
        // 1. Create the list in the DB
        val listId = trelloRepository.createList(boardId = boardId, title = listTitle)
        
        // 2. Loop through cards inside this list and create them in the DB!
        val cardsArray = listObj.optJSONArray("cards")
        if (cardsArray != null) {
            for (j in 0 until cardsArray.length()) {
                val cardTitle = cardsArray.getString(j)
                trelloRepository.createCard(listId = listId, title = cardTitle)
            }
        }
    }
}
```
By following this exact pattern in `InteractWithAiUseCase.kt`, you can teach the AI to execute *any* command in your app—moving cards, deleting boards, or setting favorites!
