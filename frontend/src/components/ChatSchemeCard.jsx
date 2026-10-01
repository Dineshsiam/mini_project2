import { ArrowRight, Tag, Info, ExternalLink } from "lucide-react";

export default function ChatSchemeCard({ scheme }) {
  if (!scheme) return null;

  return (
    <div className="flex flex-col overflow-hidden rounded-xl border border-green-100 bg-green-50/50 shadow-sm transition hover:border-green-200 hover:shadow-md">
      <div className="p-4 flex-1">
        
        <div className="flex items-start gap-2 mb-2">
          <div className="mt-0.5 rounded-full bg-green-200/50 p-1 text-green-700">
            <Tag size={12} />
          </div>
          <h4 className="font-bold text-green-950 text-sm leading-tight line-clamp-2">
            {scheme.name}
          </h4>
        </div>

        {scheme.reason && (
          <p className="text-xs text-green-800/80 line-clamp-3 mb-3 leading-relaxed border-l-2 border-green-200 pl-2 ml-1">
            {scheme.reason}
          </p>
        )}

      </div>
      
      {scheme.sourceUrl && (
        <a 
          href={scheme.sourceUrl} 
          target="_blank" 
          rel="noopener noreferrer"
          className="flex items-center justify-center gap-1.5 border-t border-green-100 bg-white py-2.5 text-xs font-semibold text-green-700 hover:bg-green-50/80 transition"
        >
          View Source
          <ExternalLink size={13} />
        </a>
      )}
    </div>
  );
}
