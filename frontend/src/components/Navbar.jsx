import { Sprout } from "lucide-react";

export default function Navbar({ setPage, activePage }) {
	return (
		<header className="sticky top-0 z-50 border-b border-green-100/80 bg-white/90 backdrop-blur">
			<div className="section-container flex items-center justify-between py-4">
				<button onClick={() => setPage("home")} aria-label="Go to FarmScheme home" className="flex items-center gap-3 text-left">
					<span className="flex h-10 w-10 items-center justify-center rounded-xl bg-green-700 text-white">
						<Sprout size={21} />
					</span>
					<span>
						<span className="block font-bold text-green-800">FarmScheme</span>
						<span className="block text-xs text-gray-500">Smart Scheme Finder</span>
					</span>
				</button>
				<nav className="flex gap-1 text-sm font-semibold text-gray-600">
					<NavButton page="schemes" label="Explore schemes" activePage={activePage} setPage={setPage} />
					<NavButton page="recommend" label="Find my schemes" activePage={activePage} setPage={setPage} />
					<NavButton page="chat" label="AI Chat" activePage={activePage} setPage={setPage} primary />
					<NavButton page="about" label="About" activePage={activePage} setPage={setPage} />
				</nav>
			</div>
		</header>
	);
}

function NavButton({ page, label, activePage, setPage, primary }) {
	return (
		<button
			onClick={() => setPage(page)}
			className={`rounded-lg px-3 py-2 transition ${primary ? "bg-green-700 text-white shadow-sm hover:bg-green-800" : activePage === page ? "bg-green-50 text-green-700" : "hover:bg-green-50 hover:text-green-700"}`}
		>
			{label}
		</button>
	);
}
