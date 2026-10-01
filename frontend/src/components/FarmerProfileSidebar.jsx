import { UserCheck, MapPin, Tent, Sprout, HandCoins, Users, Contact, UserRound, Briefcase } from "lucide-react";

export default function FarmerProfileSidebar({ profile, language }) {
  if (!profile) return null;

  const t = (en, ta) => language === "ta" ? ta : en;

  const unknown = t("Not provided", "வழங்கப்படவில்லை");

  const fields = [
    { key: "state", label: t("State", "மாநிலம்"), icon: <MapPin size={16} />, value: profile.state },
    { key: "district", label: t("District", "மாவட்டம்"), icon: <MapPin size={16} />, value: profile.district },
    { key: "landHolding", label: t("Land (Acres)", "நிலம் (ஏக்கர்)"), icon: <Tent size={16} />, value: profile.landHolding },
    { key: "crop", label: t("Main Crop", "பயிர்"), icon: <Sprout size={16} />, value: profile.crop },
    { key: "annualIncome", label: t("Annual Income", "வருமானம்"), icon: <HandCoins size={16} />, value: profile.annualIncome ? `₹${profile.annualIncome}` : null },
    { key: "category", label: t("Category", "பிரிவு"), icon: <Users size={16} />, value: profile.category },
    { key: "farmerType", label: t("Farmer Type", "விவசாயி வகை"), icon: <UserCheck size={16} />, value: profile.farmerType },
    { key: "age", label: t("Age", "வயது"), icon: <Contact size={16} />, value: profile.age },
    { key: "gender", label: t("Gender", "பாலினம்"), icon: <UserRound size={16} />, value: profile.gender },
    { key: "occupation", label: t("Occupation", "தொழில்"), icon: <Briefcase size={16} />, value: profile.occupation },
  ];

  return (
    <div className="flex h-full flex-col overflow-y-auto rounded-3xl bg-green-50/50 p-6 border border-green-100">
      
      <div className="mb-6 flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-green-200 text-green-800">
          <UserCheck size={20} />
        </div>
        <div>
          <h3 className="font-bold text-gray-900 leading-tight">
            {t("Farmer Profile", "விவசாயி விவரங்கள்")}
          </h3>
          <p className="text-xs text-gray-500">
            {t("Updated from chat", "உரையாடலில் இருந்து")}
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        {fields.map(field => {
          const hasValue = field.value !== null && field.value !== undefined && field.value !== "";
          return (
            <div 
              key={field.key} 
              className={`flex items-center gap-3 rounded-xl border p-3 transition-colors ${
                hasValue 
                  ? "bg-white border-green-200 shadow-sm" 
                  : "bg-transparent border-dashed border-gray-200 opacity-60"
              }`}
            >
              <div className={hasValue ? "text-green-600" : "text-gray-400"}>
                {field.icon}
              </div>
              <div className="flex flex-col min-w-0">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-gray-500">
                  {field.label}
                </span>
                <span className={`truncate text-sm font-medium ${hasValue ? "text-gray-900" : "text-gray-400 italic"}`}>
                  {hasValue ? field.value : unknown}
                </span>
              </div>
            </div>
          );
        })}
      </div>
      
    </div>
  );
}
