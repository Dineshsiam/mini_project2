import { useState, useEffect, useRef } from "react";
import ChatMessage from "./ChatMessage";
import ChatInput from "./ChatInput";
import LanguageSelector from "./LanguageSelector";
import FarmerProfileSidebar from "./FarmerProfileSidebar";
import { streamChatMessage, getSessionProfile } from "../services/chatApi";
import { AlertCircle, RotateCcw } from "lucide-react";

export default function ChatAssistant() {
  const [sessionId, setSessionId] = useState("");
  const [language, setLanguage] = useState("en"); // en | ta
  const [messages, setMessages] = useState([]);
  const [streamingText, setStreamingText] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [profile, setProfile] = useState({});
  const [error, setError] = useState(null);

  const messagesEndRef = useRef(null);

  // Initialize session ID from sessionStorage or create new
  useEffect(() => {
    const existingSession = sessionStorage.getItem("chatSessionId");
    if (existingSession) {
      setSessionId(existingSession);
      // Fetch existing profile if available
      getSessionProfile(existingSession)
        .then(res => { if (res) setProfile(res); })
        .catch(err => console.debug("No existing profile found."));
    } else {
      const newSession = crypto.randomUUID();
      sessionStorage.setItem("chatSessionId", newSession);
      setSessionId(newSession);
    }

    // Add a welcome message if none exist
    setMessages([{ 
      id: "welcome", 
      role: "ASSISTANT", 
      content: "Hello! I am Kisan Setu AI. I can help you find agricultural schemes. Please tell me about your farm, location, and what you grow.",
      recommendations: [] 
    }]);
  }, []);

  // Set Tamil welcome message if language changed and no user interaction yet
  useEffect(() => {
    if (messages.length === 1 && messages[0].id === "welcome") {
      setMessages([{
        id: "welcome",
        role: "ASSISTANT",
        content: language === "ta" 
          ? "வணக்கம்! நான் கிசான் சேது AI. தங்களுக்கு ஏற்ற விவசாய திட்டங்களை கண்டறிய நான் உதவ முடியும். உங்கள் விவசாய நிலம், இடம், மற்றும் பயிர்களை பற்றி கூறவும்." 
          : "Hello! I am Kisan Setu AI. I can help you find agricultural schemes. Please tell me about your farm, location, and what you grow."
      }]);
    }
  }, [language]);

  // Auto-scroll to bottom of chat
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, streamingText]);

  const handleSendMessage = async (text) => {
    if (!text.trim() || isStreaming) return;
    setError(null);

    // Add user message to UI
    const tempUserMsg = { id: Date.now().toString(), role: "USER", content: text };
    setMessages(prev => [...prev, tempUserMsg]);
    setIsStreaming(true);
    setStreamingText("");

    let currentResponseRef = { recommendations: [], text: "" };

    const onChunk = (chunkStr) => {
      currentResponseRef.text += chunkStr;
      setStreamingText(currentResponseRef.text);
    };

    onChunk.onProfile = (payload) => {
      if (payload.farmerProfile) setProfile(payload.farmerProfile);
      if (payload.recommendations) currentResponseRef.recommendations = payload.recommendations;
    };

    const onDone = () => {
      setMessages(prev => [
        ...prev, 
        { 
          id: (Date.now()+1).toString(), 
          role: "ASSISTANT", 
          content: currentResponseRef.text,
          recommendations: currentResponseRef.recommendations
        }
      ]);
      setStreamingText("");
      setIsStreaming(false);
    };

    const onError = (err) => {
      setError(language === "ta" ? "மன்னிக்கவும், பிழை ஏற்பட்டுள்ளது." : "Sorry, an error occurred communicating with the AI.");
      setIsStreaming(false);
      setStreamingText("");
    };

    await streamChatMessage(sessionId, text, language, onChunk, onDone, onError);
  };

  const handleReset = () => {
    if (window.confirm(language === "ta" ? "புதிய உரையாடலை தொடங்க வேண்டுமா?" : "Start a new conversation?")) {
      const newSession = crypto.randomUUID();
      sessionStorage.setItem("chatSessionId", newSession);
      setSessionId(newSession);
      setProfile({});
      setMessages([{ 
        id: "welcome", 
        role: "ASSISTANT", 
        content: language === "ta" 
          ? "வணக்கம்! நான் கிசான் சேது AI..." 
          : "Hello! I am Kisan Setu AI. I can help you find agricultural schemes. Please tell me about your farm, location, and what you grow."
      }]);
      setError(null);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col xl:flex-row gap-6 p-4 h-[calc(100vh-80px)] min-h-[600px]">
      
      {/* Sidebar - farmer profile */}
      <div className="hidden xl:block w-80 shrink-0 h-full">
        <FarmerProfileSidebar profile={profile} language={language} />
      </div>

      {/* Main chat area */}
      <div className="flex flex-1 flex-col overflow-hidden rounded-3xl border border-gray-200 bg-white shadow-sm">
        
        {/* Header */}
        <div className="flex h-16 shrink-0 items-center justify-between border-b border-gray-100 px-6">
          <div className="flex items-center gap-3">
            <div className="h-8 w-8 rounded-lg bg-green-600 flex items-center justify-center text-white font-bold">AI</div>
            <h2 className="font-bold text-gray-900">
              Kisan Setu Assistant
            </h2>
          </div>
          <div className="flex items-center gap-3">
            <LanguageSelector language={language} setLanguage={setLanguage} />
            <button onClick={handleReset} className="text-gray-400 hover:text-red-500 transition tooltip" title="Reset Chat">
              <RotateCcw size={18} />
            </button>
          </div>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 bg-slate-50">
          
          {error && (
            <div className="mb-6 mx-auto max-w-md flex items-center gap-3 rounded-xl bg-red-50 p-4 text-red-600 border border-red-100">
              <AlertCircle size={20} />
              <p className="text-sm">{error}</p>
            </div>
          )}

          {messages.map((msg, i) => (
            <ChatMessage key={msg.id || i} message={msg} isStreaming={false} />
          ))}

          {isStreaming && streamingText && (
            <ChatMessage 
              message={{ role: "ASSISTANT", content: streamingText }} 
              isStreaming={true} 
            />
          )}
          
          <div ref={messagesEndRef} className="h-4" />
        </div>

        {/* Input */}
        <div className="shrink-0 border-t border-gray-100 bg-white p-4 sm:px-6 sm:pb-6">
          <ChatInput 
            onSendMessage={handleSendMessage} 
            disabled={isStreaming} 
            language={language}
          />
        </div>

      </div>

    </div>
  );
}
