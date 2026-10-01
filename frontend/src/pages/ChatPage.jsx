import ChatAssistant from "../components/ChatAssistant";

export default function ChatPage() {
  return (
    <main className="min-h-screen bg-[#f7faf7] py-6 sm:py-8">
      <div className="section-container flex justify-center">
        <ChatAssistant />
      </div>
    </main>
  );
}
