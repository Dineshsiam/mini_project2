# Government Scheme Recommendation System for Farmers

This is an AI-powered hybrid recommendation system designed to match farmers to the most relevant government schemes.

The system uses:

1. **Offline Preprocessing (Python)**: Cleans 794 raw scraped scheme JSON files, extracts relevant fields, and generates semantic vector embeddings using `sentence-transformers/all-MiniLM-L6-v2`.
2. **Database (PostgreSQL + `pgvector`)**: Stores clean scheme metadata and 384-dimensional vector embeddings, enabling fast vector cosine similarity search.
3. **Backend API (Java / Spring Boot 3)**: Implements CRUD operations, keyword text search, and a **Hybrid Recommendation Engine** using Java local in-process embedding inference via **LangChain4j**. **No Python runtime dependencies are required.**

---

## Final Project Architecture

```
                    OFFLINE (Run Once)

          Raw JSON Files (862 Schemes)
                     │
                     ▼
          Python Data Preprocessor (preprocessing/preprocess.py)
                     │
                     ▼
          Clean dataset/schemes.json
                     │
                     ▼
          Python Embedding Generator (preprocessing/embedding.py)
                     │
                     ▼
          dataset/schemes_with_embeddings.json
                     │
                     ▼
     Java Spring Boot Startup Loader (Seeds Database)
                     │
                     ▼
           PostgreSQL + pgvector (farmers_db container)
───────────────────────────────────────────────────────────────────

             ONLINE APPLICATION (Spring Boot Backend)

 Farmer Profile ──> REST Controller (POST /api/recommend)
                         │
                         ▼
               Recommendation Engine
                         │
        Local Java Embedding Generation (LangChain4j ONNX)
                         │
                         ▼
        Top 50 Cosine Nearest Neighbors (pgvector)
                         │
                         ▼
        Rule-Based Filtering & Hybrid Scoring
        (0.7 * RuleScore + 0.3 * SemanticScore)
                         │
                         ▼
               Top 5 Recommended Schemes
```

---

## Folder Structure

```
miniProject2/
├── dataset/                        # Cleaned dataset outputs
│   ├── schemes.json
│   └── schemes_with_embeddings.json
├── preprocessing/                  # Data preparation components
│   ├── preprocess.py               # Preprocessing script
│   └── embedding.py                # Embedding generator
├── database/                       # Database schema scripts
│   └── schema.sql                  # PostgreSQL pgvector schema
├── backend/                        # Java Spring Boot 3 app
│   ├── pom.xml                     # Maven dependencies
│   ├── src/main/java/...           # Java controllers, services, etc.
│   └── src/test/java/...           # JUnit tests
├── frontend/                       # React frontend application
│   ├── src/                        # UI source (App.jsx, index.css)
│   ├── index.html                  # HTML template
│   └── package.json                # NPM packages configuration
├── docker-compose.yml              # PostgreSQL + pgvector Docker compose
└── README.md                       # Setup and run instructions
```

---

## Setup and Running Instructions

### Prerequisite

- Docker Desktop (or Docker Engine) with Docker Compose v2, running
- `dataset/schemes_with_embeddings.json` generated and present in the repository

### Run the full application

From the repository root, build and start PostgreSQL, the Spring Boot API, and the React frontend:

```bash
docker compose up --build -d
```

Open **http://localhost:5173**. The API is available at **http://localhost:8080**, and Swagger UI at **http://localhost:8080/swagger-ui/index.html**. PostgreSQL is exposed on port `5433` for local tools.

The backend image builds with Java 21, Compose waits for PostgreSQL to become healthy, and the frontend's Nginx server forwards `/api` requests to the backend container. On first boot, the backend seeds an empty database from the precomputed embeddings dataset.

To inspect startup output or stop the services:

```bash
docker compose logs -f
docker compose down
```

To generate the dataset from source, install Python 3.10+ with `sentence-transformers` and `numpy`, then run `preprocessing/preprocess.py` followed by `preprocessing/embedding.py` from the repository root.

---

## API Documentation

The Spring Boot backend comes integrated with Swagger OpenAPI UI for interactive testing.
Access it at: **`http://localhost:8080/swagger-ui/index.html`**

### User REST Endpoints

#### 1. Get Recommendations

- **Endpoint**: `POST /api/recommend`
- **Request Payload**:
  ```json
  {
    "state": "Tamil Nadu",
    "gender": "Male",
    "age": 42,
    "occupation": "Farmer",
    "category": "BC",
    "income": 180000,
    "landHolding": 3.5,
    "disability": false,
    "education": "10th",
    "keywords": "drip irrigation"
  }
  ```
- **Response Payload (Top 5 Hybrid recommendations)**:
  ```json
  [
    {
      "score": 87,
      "scheme": "Tamil Nadu Drip Irrigation Subsidy",
      "reason": "Matches state residency requirements (Tamil Nadu). Eligible as a small landholder (<= 5 acres). Directly matches keywords for: drip irrigation",
      "schemeDetails": {
        "id": "2db4e81a-7b3f-42cb-b1b0-9cb5587f7112",
        "slug": "tn-drip-irrigation",
        ...
      }
    }
  ]
  ```

#### 2. Get All Schemes

- **Endpoint**: `GET /api/schemes`
- **Description**: Returns all 794 schemes.

#### 3. Get Scheme Details

- **Endpoint**: `GET /api/schemes/{id}`
- **Description**: Retrieves details for a specific scheme by UUID.

#### 4. Keyword Text Search

- **Endpoint**: `GET /api/schemes/search?keyword=goat`
- **Description**: Searches scheme name, description, benefits, and eligibility fields for the keyword.

---

### Admin REST Endpoints

#### 1. Create a Scheme (Recalculates Embedding dynamically in Java)

- **Endpoint**: `POST /api/admin/scheme`
- **Description**: Admin creates a new scheme. Embedding is generated on-the-fly in Spring Boot.

#### 2. Update a Scheme

- **Endpoint**: `PUT /api/admin/scheme/{id}`

#### 3. Delete a Scheme

- **Endpoint**: `DELETE /api/admin/scheme/{id}`

#### 4. Reload Schemes from Seeding File

- **Endpoint**: `POST /api/admin/reload`
- **Description**: Clears the database and re-seeds it from `dataset/schemes_with_embeddings.json`.

#### 5. Get Database Statistics

- **Endpoint**: `GET /api/admin/statistics`
- **Description**: Returns aggregated numbers for central vs state level, category breakdowns, and states.

---

## Recommendation Algorithm Details

The **Recommendation Engine** (`RecommendationEngine.java`) utilizes a **Hybrid Scoring Approach** combining rule-based eligibility verification and vector semantic search.

1. **Farmer Profile Embedding**: Converts the input profile into a text document and generates a 384-d vector embedding using LangChain4j.
2. **Vector Candidate Fetching**: Queries PostgreSQL's `pgvector` table using Cosine similarity (`<=>`) to fetch the top 50 closest semantic matches.
3. **Rule-Based Checking**: Checks hard constraints (State, Gender) and soft constraints (Income, Land limits, SC/ST categories, Disability, Keyword matches) for each candidate.
   - State mismatch or gender mismatch immediately rejects the candidate (Rule Score = 0.0).
   - SC/ST categories, BPL status, small land holding, and search query keywords give rule boosts.
4. **Hybrid Score Calculation**:
   $$\text{Final Score} = 0.7 \times (\text{Rule Score} \times 100) + 0.3 \times (\text{Cosine Similarity} \times 100)$$
5. **Ranking**: Results are sorted in descending order of final score, returning the Top 5 recommended schemes.
