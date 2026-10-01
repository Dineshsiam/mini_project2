export async function getSessionProfile(sessionId) {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";
  const response = await fetch(`${API_BASE_URL}/chat/session/${sessionId}/profile`);
  if (!response.ok) {
    if (response.status === 404) return null;
    throw new Error(`API returned ${response.status}`);
  }
  return response.json();
}

/**
 * Handles Server-Sent Events (SSE) streaming for the chat model
 */
export async function streamChatMessage(sessionId, message, language, onChunk, onDone, onError) {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";
  try {
    const response = await fetch(`${API_BASE_URL}/chat/message/stream`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ sessionId, message, language })
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `API error ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    
    // Buffer for chunking SSE lines
    let buffer = "";

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      
      // Keep the last partial line in the buffer
      buffer = lines.pop();

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        if (line.startsWith("event:")) {
          const eventType = line.substring(6);
          // Assuming data immediately follows the event in our Spring Boot implementation
          const dataLine = lines[i + 1] || "";
          
          if (dataLine.startsWith("data:")) {
            const dataStr = dataLine.substring(5);
            i++; // skip the data line we just processed
            
            if (eventType === "chunk") {
              onChunk(dataStr);
            } else if (eventType === "profile") {
              try {
                // Profile comes as JSON payload
                const parsed = JSON.parse(dataStr);
                // Call a special chunk handler or just ignore if onChunk doesn't handle it
                // We'll pass it to an optional param if supported
                if (typeof onChunk.onProfile === 'function') {
                  onChunk.onProfile(parsed);
                }
              } catch (e) {
                console.error("Failed to parse profile payload", e);
              }
            } else if (eventType === "done") {
              onDone();
              return;
            } else if (eventType === "error") {
              onError(new Error(dataStr));
              return;
            }
          }
        }
      }
    }
  } catch (err) {
    console.error("Chat Stream error:", err);
    onError(err);
  }
}
