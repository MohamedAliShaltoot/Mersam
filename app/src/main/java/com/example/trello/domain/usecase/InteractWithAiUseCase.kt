package com.example.trello.domain.usecase

import com.example.trello.domain.repository.AiRepository
import com.example.trello.domain.repository.TrelloRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import org.json.JSONObject

class InteractWithAiUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val trelloRepository: TrelloRepository
) {
    suspend operator fun invoke(prompt: String): Result<String> {
        return try {
            // Context Awareness: Fetch all boards to provide context to the AI
            val boards = trelloRepository.observeBoards("").firstOrNull() ?: emptyList()
            val boardsContext = if (boards.isEmpty()) {
                "The user currently has no boards."
            } else {
                "The user currently has the following boards: " + boards.joinToString(", ") { it.title }
            }

            // Action Detection (Structured Output)
            // We give the AI a strict instruction to create a board if the user asks for it.
            val systemPrompt = """
                You are a helpful project management AI assistant in Mersam, an intuitive Kanban board application.
                $boardsContext
                
                The user says: "$prompt"
                
                If the user explicitly asks to CREATE a new board or task, you MUST reply with a JSON object in this exact format:
                {
                   "action": "CREATE_BOARD",
                   "title": "The Board Title",
                   "description": "The Board Description",
                   "lists": ["List 1 Name", "List 2 Name"] // Optional: Include lists if the user asks for them
                }
                
                If the user is NOT asking to create something, reply with normal text, answering their question or giving them the advice they asked for (e.g., task breakdown).
                DO NOT return JSON if they just want advice.
            """.trimIndent()

            val aiResponseResult = aiRepository.getGeneralAssistantResponse(systemPrompt)

            if (aiResponseResult.isSuccess) {
                val rawResponse = aiResponseResult.getOrNull() ?: ""
                
                // The AI often wraps JSON in markdown blocks (e.g., ```json ... ```)
                val cleanResponse = rawResponse.replace("```json", "").replace("```", "").trim()
                
                // Attempt to parse JSON action
                if (cleanResponse.startsWith("{") && cleanResponse.endsWith("}")) {
                    try {
                        val json = JSONObject(cleanResponse)
                        if (json.optString("action") == "CREATE_BOARD") {
                            val title = json.optString("title", "New Board")
                            val description = json.optString("description", "")
                            
                            // Create the board
                            val boardId = trelloRepository.createBoard(title = title, description = description, colorHex = "#4CAF50")
                            
                            // Check if the AI suggested lists to create
                            val listsArray = json.optJSONArray("lists")
                            if (listsArray != null && listsArray.length() > 0) {
                                for (i in 0 until listsArray.length()) {
                                    val listTitle = listsArray.getString(i)
                                    trelloRepository.createList(boardId = boardId, title = listTitle)
                                }
                                return Result.success("I have created the board '$title' along with the lists you requested!")
                            }
                            
                            return Result.success("I have successfully created the board '$title' for you!")
                        }
                    } catch (e: Exception) {
                        // Not valid JSON, just return the text
                    }
                }
                
                Result.success(rawResponse)
            } else {
                aiResponseResult
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Unknown Error"
            if (msg.contains("404") || msg.contains("API_KEY_INVALID") || msg.contains("MissingFieldException")) {
                Result.failure(Exception("API Key Error: Your API key is invalid or missing permissions. It must start with 'AIzaSy' and be generated in Google AI Studio."))
            } else {
                Result.failure(Exception("AI Error: $msg"))
            }
        }
    }
}
