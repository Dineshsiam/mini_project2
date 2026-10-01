import { useEffect, useState } from "react";

import Navbar from "./components/Navbar";
import Footer from "./components/Footer";

import Home from "./pages/Home";
import Recommend from "./pages/Recommend";
import Schemes from "./pages/Schemes";
import ChatPage from "./pages/ChatPage";

import {
  Target,
  BrainCircuit,
  Database,
  Sparkles
} from "lucide-react";


export default function App() {
  const [page, setPageState] = useState(() => window.location.hash.slice(1) || "home");

  const setPage = (nextPage) => {
    setPageState(nextPage);
    window.location.hash = nextPage === "home" ? "" : nextPage;
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  useEffect(() => {
    const handleHashChange = () => setPageState(window.location.hash.slice(1) || "home");
    window.addEventListener("hashchange", handleHashChange);
    return () => window.removeEventListener("hashchange", handleHashChange);
  }, []);


  const renderPage = () => {

    switch (page) {

      case "recommend":
        return <Recommend />;

      case "schemes":
        return <Schemes />;

      case "chat":
        return <ChatPage />;

      case "about":
        return <About />;

      default:
        return (
          <Home
            setPage={setPage}
          />
        );

    }

  };


  return (

    <div className="min-h-screen bg-[#f7faf7]">

      <Navbar
        setPage={setPage}
        activePage={page}
      />

      {renderPage()}

      <Footer setPage={setPage} />

    </div>

  );
}


function About() {

  return (

    <main className="min-h-screen bg-[#f7faf7] py-16">

      <div className="section-container">

        <div className="mx-auto max-w-5xl">


          <div className="text-center">

            <p className="text-sm font-bold uppercase tracking-widest text-green-700">
              About the project
            </p>

            <h1 className="mt-3 text-4xl font-extrabold text-gray-900 sm:text-5xl">
              Government Scheme Recommendation System
            </h1>

            <p className="mx-auto mt-5 max-w-2xl leading-7 text-gray-500">
              Helping farmers discover relevant government
              schemes through intelligent profile-based
              recommendations.
            </p>

          </div>


          <div className="mt-12 rounded-3xl border border-green-100 bg-white p-7 shadow-sm sm:p-10">

            <h2 className="text-2xl font-bold text-gray-900">
              Why this project?
            </h2>

            <p className="mt-4 leading-8 text-gray-600">
              Farmers may have difficulty identifying suitable
              government schemes because information is spread
              across different sources and eligibility conditions
              can vary between schemes.
            </p>

            <p className="mt-4 leading-8 text-gray-600">
              This system simplifies the process by allowing a
              farmer to provide basic information and receive
              personalized scheme recommendations.
            </p>


            <div className="mt-10 grid gap-5 md:grid-cols-3">

              <AboutCard
                icon={<Target size={22} />}
                title="Personalized"
                text="Recommendations are based on farmer-specific information."
              />

              <AboutCard
                icon={<BrainCircuit size={22} />}
                title="Intelligent"
                text="Uses eligibility and semantic matching techniques."
              />

              <AboutCard
                icon={<Database size={22} />}
                title="Centralized"
                text="Makes scheme information easier to discover in one place."
              />

            </div>

          </div>


          <div className="mt-8 rounded-2xl bg-green-800 p-7 text-white">

            <div className="flex items-start gap-4">

              <Sparkles
                size={24}
                className="mt-1 shrink-0"
              />

              <div>

                <h3 className="text-lg font-bold">
                  Technology Stack
                </h3>

                <p className="mt-2 text-sm leading-7 text-green-100">
                  React · Tailwind CSS · Spring Boot ·
                  PostgreSQL · pgvector · LangChain4j ·
                  AI-powered recommendation
                </p>

              </div>

            </div>

          </div>

        </div>

      </div>

    </main>

  );
}


function AboutCard({
  icon,
  title,
  text
}) {

  return (

    <div className="rounded-2xl bg-green-50 p-6">

      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-100 text-green-700">

        {icon}

      </div>

      <h3 className="mt-4 font-bold text-green-950">
        {title}
      </h3>

      <p className="mt-2 text-sm leading-6 text-green-900/60">
        {text}
      </p>

    </div>

  );
}