import { User, Bot } from "lucide-react";
import ChatSchemeCard from "./ChatSchemeCard";

export default function ChatMessage({ message, isStreaming }) {
  const isUser = message.role === "USER";

  return (
    <div className={`flex w-full mb-6 ${isUser ? "justify-end" : "justify-start"}`}>
      <div className={`flex max-w-[85%] sm:max-w-[75%] gap-4 ${isUser ? "flex-row-reverse" : "flex-row"}`}>
        
        {/* Avatar */}
        <div className={`flex shrink-0 h-10 w-10 items-center justify-center rounded-full border ${
          isUser 
            ? "border-green-200 bg-green-100 text-green-700" 
            : "border-gray-200 bg-white text-gray-600 shadow-sm"
        }`}>
          {isUser ? <User size={20} /> : <Bot size={20} />}
        </div>

        {/* Content */}
        <div className="flex flex-col gap-2 min-w-0">
          <div className={`rounded-2xl px-5 py-3.5 text-[15px] leading-relaxed shadow-sm ${
            isUser 
              ? "bg-green-700 text-white rounded-tr-sm" 
              : "bg-white border border-gray-100 text-gray-800 rounded-tl-sm"
          }`}>
            <span className="whitespace-pre-wrap break-words">{message.content}</span>
            {isStreaming && (
              <span className="ml-1 inline-block h-4 w-2 animate-pulse bg-green-600 align-middle"></span>
            )}
          </div>

          {/* Scheme Cards (only for ASSISTANT) */}
          {!isUser && message.recommendations && message.recommendations.length > 0 && (
            <div className="mt-2 grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
              {message.recommendations.map((scheme, idx) => (
                <ChatSchemeCard key={`${scheme.schemeId}-${idx}`} scheme={scheme} />
              ))}
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
