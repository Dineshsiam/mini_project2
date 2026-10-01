import { Send, Mic, MicOff } from "lucide-react";
import { useState, useEffect, useRef } from "react";

export default function ChatInput({ onSendMessage, disabled, language }) {
  const [message, setMessage] = useState("");
  const [isListening, setIsListening] = useState(false);
  const recognitionRef = useRef(null);
  const textareaRef = useRef(null);

  useEffect(() => {
    // Setup Speech Recognition if available
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = true;
      
      recognition.onresult = (event) => {
        let interimTranscript = '';
        let finalTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTranscript += event.results[i][0].transcript;
          } else {
            interimTranscript += event.results[i][0].transcript;
          }
        }
        
        if (finalTranscript) {
          setMessage(prev => prev + (prev ? " " : "") + finalTranscript);
        }
      };

      recognition.onerror = (event) => {
        console.error("Speech recognition error:", event.error);
        setIsListening(false);
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognition;
    }

    return () => {
      if (recognitionRef.current) {
        recognitionRef.current.abort();
      }
    };
  }, []);

  const toggleListening = () => {
    if (!recognitionRef.current) {
      alert("Speech recognition is not supported in this browser.");
      return;
    }

    if (isListening) {
      recognitionRef.current.stop();
      setIsListening(false);
    } else {
      // Map en to en-IN, ta to ta-IN
      recognitionRef.current.lang = language === "ta" ? "ta-IN" : "en-IN";
      try {
        recognitionRef.current.start();
        setIsListening(true);
      } catch (e) {
        console.error("Failed to start speech recognition", e);
      }
    }
  };

  const handleSend = () => {
    if (message.trim() && !disabled) {
      onSendMessage(message.trim());
      setMessage("");
      // Reset textarea height
      if (textareaRef.current) {
        textareaRef.current.style.height = "auto";
      }
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleInput = (e) => {
    setMessage(e.target.value);
    e.target.style.height = "auto";
    e.target.style.height = `${Math.min(e.target.scrollHeight, 120)}px`;
  };

  const placeholderText = language === "ta" 
    ? (isListening ? "கேட்கிறது..." : "உங்கள் செய்தியை தட்டச்சு செய்யவும்...")
    : (isListening ? "Listening..." : "Type your message...");

  return (
    <div className="relative mt-2 flex items-end gap-2 rounded-2xl border border-gray-200 bg-white p-2 shadow-sm transition-colors focus-within:border-green-400 focus-within:ring-2 focus-within:ring-green-100">
      
      {/* Microphone button */}
      <button
        onClick={toggleListening}
        disabled={disabled}
        className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition ${
          isListening 
            ? "bg-red-100 text-red-600 animate-pulse" 
            : "bg-gray-50 text-gray-400 hover:bg-green-50 hover:text-green-600"
        } ${disabled ? "opacity-50 cursor-not-allowed" : ""}`}
        title={language === "ta" ? "குரல் உள்ளீடு" : "Voice input"}
      >
        {isListening ? <MicOff size={20} /> : <Mic size={20} />}
      </button>

      {/* Text input container */}
      <div className="flex-1 overflow-hidden">
        <textarea
          ref={textareaRef}
          value={message}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          placeholder={placeholderText}
          rows={1}
          className="w-full resize-none bg-transparent py-2.5 text-[15px] outline-none placeholder:text-gray-400 disabled:opacity-50"
          style={{ minHeight: "44px", maxHeight: "120px" }}
        />
      </div>

      {/* Send button */}
      <button
        onClick={handleSend}
        disabled={!message.trim() || disabled}
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-green-600 text-white transition hover:bg-green-700 disabled:bg-gray-200 disabled:text-gray-400"
      >
        <Send size={18} className="translate-x-[1px]" />
      </button>

    </div>
  );
}
