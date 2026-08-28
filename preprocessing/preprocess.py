import os
import json
import re
import html
import uuid

def clean_text(text):
    if not text:
        return ""
    # Convert list/dict to string if necessary
    if not isinstance(text, str):
        text = str(text)
    
    # Recursive HTML unescape (handles nested entities like &amp;quot;)
    prev = ""
    while prev != text:
        prev = text
        text = html.unescape(text)
    
    # Strip HTML tags
    text = re.sub(r'<[^>]+>', ' ', text)
    
    # Clean Markdown styling: bold/italic indicators, headers, links, bullets
    text = re.sub(r'\*\*|__|\*|_|`|#+', ' ', text)
    # Convert [text](url) to "text (url)" or just "text"
    text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'\1 (\2)', text)
    
    # Clean list identifiers at the start of lines/phrases
    text = re.sub(r'^\s*[-*+]\s+', ' ', text, flags=re.MULTILINE)
    text = re.sub(r'^\s*\d+[\s\.)\-]+\s*', ' ', text, flags=re.MULTILINE)
    
    # Normalize whitespace (newlines, tabs, spaces)
    text = re.sub(r'\s+', ' ', text)
    return text.strip()

def extract_list_from_rich_text(elements):
    """
    Helper to extract plain text from the structured rich text fields (e.g. detailedDescription list)
    if markdown fields are empty.
    """
    texts = []
    if not elements or not isinstance(elements, list):
        return ""
    
    def traverse(node):
        if not node:
            return
        if isinstance(node, dict):
            if "text" in node:
                texts.append(node["text"])
            if "children" in node:
                traverse(node["children"])
        elif isinstance(node, list):
            for child in node:
                traverse(child)
                
    traverse(elements)
    return " ".join(texts)

def extract_documents(doc_data):
    """
    Extract list of documents from documents section.
    """
    if not doc_data:
        return []
    
    # Try parsing from raw list first
    docs = []
    en_data = doc_data.get("data", {}).get("en", {}) if doc_data.get("data") else {}
    if not en_data:
        return []
        
    rich_docs = en_data.get("documents_required", [])
    if rich_docs:
        extracted = extract_list_from_rich_text(rich_docs)
        # Split by typical list item patterns
        items = re.split(r'\d+\.|\*|-', extracted)
        for item in items:
            cleaned = clean_text(item)
            if cleaned and cleaned not in docs:
                docs.append(cleaned)
    
    # Fallback/complement with documentsRequired_md
    md_docs = en_data.get("documentsRequired_md", "")
    if md_docs:
        # Split by newlines
        lines = md_docs.split("\n")
        for line in lines:
            line_clean = clean_text(line)
            # Remove list numbers or bullets
            line_clean = re.sub(r'^\d+\.?\s*', '', line_clean)
            if line_clean and line_clean not in docs:
                docs.append(line_clean)
                
    return [d for d in docs if len(d) > 2]

def extract_faqs(faq_data):
    """
    Extract clean list of FAQs.
    """
    faqs = []
    if not faq_data or not faq_data.get("data"):
        return faqs
    
    raw_faqs = faq_data.get("data", {}).get("en", {}).get("faqs", [])
    if not raw_faqs:
        return faqs
        
    for item in raw_faqs:
        q = clean_text(item.get("question", ""))
        a_md = item.get("answer_md", "")
        if a_md:
            a = clean_text(a_md)
        else:
            a = clean_text(extract_list_from_rich_text(item.get("answer", [])))
            
        if q and a:
            faqs.append({
                "question": q,
                "answer": a
            })
    return faqs

def extract_application_details(app_process, app_data):
    """
    Extract application mode and URL links.
    """
    modes = []
    links = []
    
    if isinstance(app_process, list):
        for proc in app_process:
            mode = proc.get("mode")
            if mode:
                mode_clean = clean_text(mode)
                if mode_clean and mode_clean not in modes:
                    modes.append(mode_clean)
            
            # Look for URLs in process
            url = proc.get("url")
            if url and url.startswith("http"):
                url_clean = url.strip()
                if url_clean not in links:
                    links.append(url_clean)
                    
            # Try to extract from process_md or process list text
            proc_md = proc.get("process_md", "")
            if proc_md:
                found_urls = re.findall(r'https?://[^\s\)\"]+', proc_md)
                for f_url in found_urls:
                    f_url_clean = f_url.rstrip(".,;)>")
                    if f_url_clean not in links:
                        links.append(f_url_clean)
                        
    # Check application block
    if app_data and app_data.get("data"):
        channels = app_data.get("data", {}).get("applicationChannel", [])
        if channels:
            for chan in channels:
                url = chan.get("applicationUrl")
                if url and url.startswith("http"):
                    url_clean = url.strip()
                    if url_clean not in links:
                        links.append(url_clean)
                        
    app_mode = ", ".join(modes) if modes else "Offline/Online"
    app_link = links[0] if links else ""
    return app_mode, app_link

def clean_eligibility_criteria(criteria_text):
    """
    Deduplicates and cleans eligibility sentences.
    """
    if not criteria_text:
        return ""
    
    # Split eligibility by sentence or typical bullet separators
    sentences = re.split(r'\. |\n|; ', criteria_text)
    seen = set()
    unique_sentences = []
    
    for s in sentences:
        s_clean = clean_text(s)
        if not s_clean or len(s_clean) < 5:
            continue
        
        # Lowercase normalized check for duplicates
        norm = re.sub(r'[^a-zA-Z0-9]', '', s_clean).lower()
        if norm not in seen:
            seen.add(norm)
            unique_sentences.append(s_clean)
            
    return ". ".join(unique_sentences) + "." if unique_sentences else ""

def process_scheme_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
        except Exception as e:
            print(f"Error loading {file_path}: {e}")
            return None
            
    if not isinstance(data, dict):
        return None
        
    slug = data.get("slug")
    scheme_section = data.get("scheme", {})
    if not slug or not scheme_section:
        return None
        
    en_section = scheme_section.get("en", {})
    basic_details = en_section.get("basicDetails", {})
    scheme_content = en_section.get("schemeContent", {})
    
    if not basic_details:
        return None
        
    name = clean_text(basic_details.get("schemeName", ""))
    short_title = clean_text(basic_details.get("schemeShortTitle", ""))
    
    # State mapping
    level_dict = basic_details.get("level") or {}
    level_label = level_dict.get("label", "Central") if isinstance(level_dict, dict) else "Central"
    level_val = level_dict.get("value", "central") if isinstance(level_dict, dict) else "central"
    
    state_dict = basic_details.get("state") or {}
    state = clean_text(state_dict.get("label", "Central")) if isinstance(state_dict, dict) else "Central"
    if not state:
        state = "Central"
        
    if level_val == "central" or level_label.lower() == "central":
        state = "Central"
        
    # Category and subcategory
    scheme_category = basic_details.get("schemeCategory") or []
    categories = [clean_text(cat.get("label", "")) for cat in scheme_category if isinstance(cat, dict) and cat.get("label")]
    
    scheme_subcategory = basic_details.get("schemeSubCategory") or []
    subcategories = [clean_text(sub.get("label", "")) for sub in scheme_subcategory if isinstance(sub, dict) and sub.get("label")]
    
    beneficiary = clean_text(basic_details.get("schemeFor", ""))
    if not beneficiary:
        target_beneficiaries = basic_details.get("targetBeneficiaries") or []
        beneficiaries = [clean_text(b.get("label", "")) for b in target_beneficiaries if isinstance(b, dict) and b.get("label")]
        beneficiary = ", ".join(beneficiaries)
        
    dept_dict = basic_details.get("nodalDepartmentName") or {}
    department = clean_text(dept_dict.get("label", "")) if isinstance(dept_dict, dict) else ""
    if not department:
        min_dict = basic_details.get("nodalMinistryName") or {}
        department = clean_text(min_dict.get("label", "")) if isinstance(min_dict, dict) else ""
        
    # Content fields
    description = clean_text(scheme_content.get("detailedDescription_md", ""))
    if not description:
        description = clean_text(extract_list_from_rich_text(scheme_content.get("detailedDescription", [])))
    if not description:
        description = clean_text(scheme_content.get("briefDescription", ""))
        
    benefits = clean_text(scheme_content.get("benefits_md", ""))
    if not benefits:
        benefits = clean_text(extract_list_from_rich_text(scheme_content.get("benefits", [])))
        
    eligibility_raw = en_section.get("eligibilityCriteria", {}).get("eligibilityDescription_md", "")
    if not eligibility_raw:
        eligibility_raw = extract_list_from_rich_text(en_section.get("eligibilityCriteria", {}).get("eligibilityDescription", []))
    eligibility = clean_eligibility_criteria(eligibility_raw)
    
    # Documents, Application, FAQs
    documents = extract_documents(data.get("documents"))
    app_mode, app_link = extract_application_details(en_section.get("applicationProcess", []), data.get("application"))
    faqs = extract_faqs(data.get("faq"))
    
    return {
        "id": str(uuid.uuid4()),
        "slug": slug,
        "name": name,
        "shortTitle": short_title,
        "state": state,
        "level": level_label,
        "category": categories,
        "subcategory": subcategories,
        "beneficiary": beneficiary,
        "department": department,
        "description": description,
        "benefits": benefits,
        "eligibility": eligibility,
        "documents": documents,
        "applicationMode": app_mode,
        "applicationLink": app_link,
        "faqs": faqs
    }

def main():
    schemes_dir = "d:/miniProject2/dataExtraction/data/schemes"
    output_dir = "d:/miniProject2/dataset"
    os.makedirs(output_dir, exist_ok=True)
    
    cleaned_schemes = []
    
    if not os.path.exists(schemes_dir):
        print(f"Directory {schemes_dir} does not exist!")
        return
        
    for file_name in os.listdir(schemes_dir):
        if file_name.endswith('.json') and file_name != 'failed.json':
            file_path = os.path.join(schemes_dir, file_name)
            processed = process_scheme_file(file_path)
            if processed and processed.get("name"):
                cleaned_schemes.append(processed)
                
    output_path = os.path.join(output_dir, "schemes.json")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(cleaned_schemes, f, indent=2, ensure_ascii=False)
        
    print(f"Successfully preprocessed {len(cleaned_schemes)} schemes.")
    print(f"Saved dataset to {output_path}")

if __name__ == "__main__":
    main()
