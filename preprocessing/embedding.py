import os
import json
import numpy as np
from sentence_transformers import SentenceTransformer

def main():
    input_path = "d:/miniProject2/dataset/schemes.json"
    output_path = "d:/miniProject2/dataset/schemes_with_embeddings.json"
    
    if not os.path.exists(input_path):
        print(f"Error: {input_path} does not exist. Run preprocess.py first.")
        return
        
    print("Loading preprocessed schemes...")
    with open(input_path, 'r', encoding='utf-8') as f:
        schemes = json.load(f)
        
    print(f"Loaded {len(schemes)} schemes. Initializing SentenceTransformer model ('all-MiniLM-L6-v2')...")
    # This will download the model to the local cache if not already present
    model = SentenceTransformer('all-MiniLM-L6-v2')
    
    print("Preparing text for embedding generation...")
    texts_to_embed = []
    for scheme in schemes:
        name = scheme.get("name", "")
        desc = scheme.get("description", "")
        benefits = scheme.get("benefits", "")
        eligibility = scheme.get("eligibility", "")
        categories = " ".join(scheme.get("category", []))
        state = scheme.get("state", "")
        
        # Combine fields into a single text document
        combined_text = f"{name} {desc} {benefits} {eligibility} {categories} {state}"
        # Normalize spaces
        combined_text = " ".join(combined_text.split())
        texts_to_embed.append(combined_text)
        
    print("Generating embeddings (this may take a minute)...")
    embeddings = model.encode(texts_to_embed, show_progress_bar=True, batch_size=32)
    
    print("Appending embeddings to schemes...")
    for i, scheme in enumerate(schemes):
        # Convert numpy array to list of floats
        scheme["embedding"] = embeddings[i].tolist()
        
    print(f"Saving embedded schemes to {output_path}...")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(schemes, f, indent=2, ensure_ascii=False)
        
    print("Done! Schemes with embeddings generated successfully.")

if __name__ == "__main__":
    main()
