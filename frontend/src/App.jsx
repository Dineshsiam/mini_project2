import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080/api';

export default function App() {
  const [activeTab, setActiveTab] = useState('recommend'); // recommend, search, admin
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  
  // Recommendation state
  const [farmerProfile, setFarmerProfile] = useState({
    state: 'Tamil Nadu',
    gender: 'Male',
    age: 42,
    occupation: 'Farmer',
    category: 'BC',
    income: 180000,
    landHolding: 3.5,
    disability: false,
    education: '10th',
    keywords: ''
  });
  const [recommendations, setRecommendations] = useState([]);
  const [expandedScheme, setExpandedScheme] = useState(null);

  // Search state
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searched, setSearched] = useState(false);

  // Admin state
  const [stats, setStats] = useState(null);
  const [schemes, setSchemes] = useState([]);
  const [adminSearch, setAdminSearch] = useState('');
  const [editingScheme, setEditingScheme] = useState(null); // null means list view or new form
  const [showAddForm, setShowAddForm] = useState(false);
  
  // Admin Form State
  const [schemeForm, setSchemeForm] = useState({
    id: '',
    slug: '',
    name: '',
    shortTitle: '',
    state: 'Central',
    level: 'Central',
    department: '',
    beneficiary: 'Farmer',
    description: '',
    benefits: '',
    eligibility: '',
    category: '',
    subcategory: '',
    documents: '',
    applicationMode: 'Online',
    applicationLink: '',
    faq: []
  });

  const [newFaq, setNewFaq] = useState({ question: '', answer: '' });

  // Fetch initial schemes and stats for admin on tab change
  useEffect(() => {
    if (activeTab === 'admin') {
      fetchAdminData();
    }
  }, [activeTab]);

  const fetchAdminData = async () => {
    setLoading(true);
    setError('');
    try {
      // Fetch stats
      const statsRes = await fetch(`${API_BASE_URL}/admin/statistics`);
      if (statsRes.ok) {
        const statsData = await statsRes.json();
        setStats(statsData);
      }
      
      // Fetch all schemes
      const schemesRes = await fetch(`${API_BASE_URL}/schemes`);
      if (schemesRes.ok) {
        const schemesData = await schemesRes.json();
        setSchemes(schemesData);
      }
    } catch (err) {
      setError('Could not fetch admin dashboard data. Is the backend running?');
    } finally {
      setLoading(false);
    }
  };

  // Trigger Hybrid Recommendation
  const handleGetRecommendations = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setRecommendations([]);
    setExpandedScheme(null);

    try {
      const res = await fetch(`${API_BASE_URL}/recommend`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(farmerProfile)
      });
      if (res.ok) {
        const data = await res.json();
        setRecommendations(data);
        if (data.length === 0) {
          setError('No eligible schemes found for this profile. Try relaxing income or land holding criteria.');
        }
      } else {
        const errData = await res.json();
        setError(errData.message || 'Failed to retrieve recommendations.');
      }
    } catch (err) {
      setError('Error communicating with backend service. Please check connection.');
    } finally {
      setLoading(false);
    }
  };

  // Keyword search schemes
  const handleSearch = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSearchResults([]);
    setSearched(true);
    setExpandedScheme(null);

    try {
      const res = await fetch(`${API_BASE_URL}/schemes/search?keyword=${encodeURIComponent(searchKeyword)}`);
      if (res.ok) {
        const data = await res.json();
        setSearchResults(data);
      } else {
        setError('Failed to fetch search results.');
      }
    } catch (err) {
      setError('Error communicating with backend service.');
    } finally {
      setLoading(false);
    }
  };

  // Reload Database
  const handleReloadDatabase = async () => {
    if (!window.confirm('Are you sure you want to clear and reload the schemes database from dataset?')) return;
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const res = await fetch(`${API_BASE_URL}/admin/reload`, { method: 'POST' });
      if (res.ok) {
        const data = await res.json();
        setSuccess(`Successfully reloaded! Ingested ${data.totalLoaded} schemes.`);
        fetchAdminData();
      } else {
        setError('Failed to reload database.');
      }
    } catch (err) {
      setError('Error communication with backend.');
    } finally {
      setLoading(false);
    }
  };

  // Delete Scheme
  const handleDeleteScheme = async (id) => {
    if (!window.confirm('Are you sure you want to delete this scheme?')) return;
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const res = await fetch(`${API_BASE_URL}/admin/scheme/${id}`, { method: 'DELETE' });
      if (res.ok) {
        setSuccess('Scheme deleted successfully.');
        fetchAdminData();
      } else {
        setError('Failed to delete scheme.');
      }
    } catch (err) {
      setError('Error deleting scheme.');
    } finally {
      setLoading(false);
    }
  };

  // Init Save Scheme form
  const handleInitAdd = () => {
    setSchemeForm({
      id: '',
      slug: '',
      name: '',
      shortTitle: '',
      state: 'Central',
      level: 'Central',
      department: '',
      beneficiary: 'Farmer',
      description: '',
      benefits: '',
      eligibility: '',
      category: '',
      subcategory: '',
      documents: '',
      applicationMode: 'Online',
      applicationLink: '',
      faq: []
    });
    setNewFaq({ question: '', answer: '' });
    setEditingScheme(null);
    setShowAddForm(true);
  };

  // Init edit form
  const handleInitEdit = (scheme) => {
    setSchemeForm({
      id: scheme.id,
      slug: scheme.slug,
      name: scheme.name,
      shortTitle: scheme.shortTitle || '',
      state: scheme.state,
      level: scheme.level || 'Central',
      department: scheme.department || '',
      beneficiary: scheme.beneficiary || 'Farmer',
      description: scheme.description || '',
      benefits: scheme.benefits || '',
      eligibility: scheme.eligibility || '',
      category: scheme.category ? scheme.category.join(', ') : '',
      subcategory: scheme.subcategory ? scheme.subcategory.join(', ') : '',
      documents: scheme.documents ? scheme.documents.join(', ') : '',
      applicationMode: scheme.applicationMode || 'Online',
      applicationLink: scheme.applicationLink || '',
      faq: scheme.faq || []
    });
    setNewFaq({ question: '', answer: '' });
    setEditingScheme(scheme.id);
    setShowAddForm(true);
  };

  // Add FAQ Item to Form
  const addFaqItem = () => {
    if (!newFaq.question.trim() || !newFaq.answer.trim()) return;
    setSchemeForm(prev => ({
      ...prev,
      faq: [...prev.faq, { ...newFaq }]
    }));
    setNewFaq({ question: '', answer: '' });
  };

  // Remove FAQ Item
  const removeFaqItem = (index) => {
    setSchemeForm(prev => ({
      ...prev,
      faq: prev.faq.filter((_, idx) => idx !== index)
    }));
  };

  // Submit Admin Form (Create/Update)
  const handleSubmitSchemeForm = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    // Format fields
    const parsedCategory = schemeForm.category ? schemeForm.category.split(',').map(s => s.trim()).filter(Boolean) : [];
    const parsedSubcategory = schemeForm.subcategory ? schemeForm.subcategory.split(',').map(s => s.trim()).filter(Boolean) : [];
    const parsedDocuments = schemeForm.documents ? schemeForm.documents.split(',').map(s => s.trim()).filter(Boolean) : [];
    
    const payload = {
      ...schemeForm,
      category: parsedCategory,
      subcategory: parsedSubcategory,
      documents: parsedDocuments
    };

    if (!payload.id) delete payload.id; // Let backend generate UUID for new schemes

    try {
      const url = editingScheme ? `${API_BASE_URL}/admin/scheme/${editingScheme}` : `${API_BASE_URL}/admin/scheme`;
      const method = editingScheme ? 'PUT' : 'POST';
      
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        setSuccess(editingScheme ? 'Scheme updated successfully!' : 'Scheme created successfully! Embedding calculated.');
        setShowAddForm(false);
        setEditingScheme(null);
        fetchAdminData();
      } else {
        const errData = await res.json();
        setError(errData.message || 'Failed to save scheme.');
      }
    } catch (err) {
      setError('Error communicating with backend.');
    } finally {
      setLoading(false);
    }
  };

  // Filter schemes listed in admin panel
  const filteredSchemes = schemes.filter(s => 
    s.name.toLowerCase().includes(adminSearch.toLowerCase()) || 
    (s.slug && s.slug.toLowerCase().includes(adminSearch.toLowerCase())) ||
    s.state.toLowerCase().includes(adminSearch.toLowerCase())
  );

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <div className="logo-section">
          <div className="logo-icon">🌾</div>
          <div className="logo-text">
            <h1>KISAN SETU</h1>
            <p>Smart Government Scheme Recommender</p>
          </div>
        </div>
        
        {/* Navigation Tabs */}
        <nav className="tabs-nav">
          <button 
            className={`tab-btn ${activeTab === 'recommend' ? 'active' : ''}`}
            onClick={() => setActiveTab('recommend')}
          >
            📋 Recommender
          </button>
          <button 
            className={`tab-btn ${activeTab === 'search' ? 'active' : ''}`}
            onClick={() => setActiveTab('search')}
          >
            🔍 Search Schemes
          </button>
          <button 
            className={`tab-btn ${activeTab === 'admin' ? 'active' : ''}`}
            onClick={() => { setActiveTab('admin'); setShowAddForm(false); }}
          >
            ⚙️ Admin Panel
          </button>
        </nav>
      </header>

      {/* Messages */}
      {error && <div className="reason-box" style={{ background: '#fef2f2', borderLeftColor: '#ef4444', color: '#991b1b', marginBottom: '24px' }}>⚠️ {error}</div>}
      {success && <div className="reason-box" style={{ background: '#f0fdf4', borderLeftColor: '#10b981', color: '#166534', marginBottom: '24px' }}>✅ {success}</div>}

      {/* LOADING SPINNER */}
      {loading && (
        <div className="loading-container">
          <div className="loader-spinner"></div>
          <p style={{ fontWeight: 600, color: 'var(--brand-hover)' }}>Processing request, loading AI embeddings...</p>
        </div>
      )}

      {/* TAB 1: RECOMMENDATIONS */}
      {activeTab === 'recommend' && !loading && (
        <div className="tab-content">
          <div className="glass-card" style={{ marginBottom: '30px' }}>
            <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--brand-dark)', marginBottom: '20px' }}>Farmer Demographic Profile</h2>
            <form onSubmit={handleGetRecommendations}>
              <div className="form-grid">
                <div className="form-group">
                  <label>State Residence</label>
                  <select 
                    value={farmerProfile.state}
                    onChange={(e) => setFarmerProfile({...farmerProfile, state: e.target.value})}
                  >
                    <option>Tamil Nadu</option>
                    <option>Maharashtra</option>
                    <option>Karnataka</option>
                    <option>Andhra Pradesh</option>
                    <option>Gujarat</option>
                    <option>Central</option>
                  </select>
                </div>
                
                <div className="form-group">
                  <label>Gender</label>
                  <select 
                    value={farmerProfile.gender}
                    onChange={(e) => setFarmerProfile({...farmerProfile, gender: e.target.value})}
                  >
                    <option>Male</option>
                    <option>Female</option>
                    <option>Other</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Age (years)</label>
                  <input 
                    type="number" 
                    value={farmerProfile.age} 
                    onChange={(e) => setFarmerProfile({...farmerProfile, age: parseInt(e.target.value) || 0})}
                  />
                </div>

                <div className="form-group">
                  <label>Social Category</label>
                  <select 
                    value={farmerProfile.category}
                    onChange={(e) => setFarmerProfile({...farmerProfile, category: e.target.value})}
                  >
                    <option>General</option>
                    <option>OBC</option>
                    <option>BC</option>
                    <option>SC</option>
                    <option>ST</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Annual Income (₹)</label>
                  <input 
                    type="number" 
                    value={farmerProfile.income} 
                    onChange={(e) => setFarmerProfile({...farmerProfile, income: parseFloat(e.target.value) || 0})}
                  />
                </div>

                <div className="form-group">
                  <label>Land Holding (acres)</label>
                  <input 
                    type="number" 
                    step="0.1" 
                    value={farmerProfile.landHolding} 
                    onChange={(e) => setFarmerProfile({...farmerProfile, landHolding: parseFloat(e.target.value) || 0})}
                  />
                </div>

                <div className="form-group">
                  <label>Education</label>
                  <input 
                    type="text" 
                    placeholder="e.g. 10th pass, graduate"
                    value={farmerProfile.education} 
                    onChange={(e) => setFarmerProfile({...farmerProfile, education: e.target.value})}
                  />
                </div>

                <div className="form-group checkbox-group">
                  <input 
                    type="checkbox" 
                    id="disability"
                    checked={farmerProfile.disability} 
                    onChange={(e) => setFarmerProfile({...farmerProfile, disability: e.target.checked})}
                  />
                  <label htmlFor="disability" style={{ cursor: 'pointer' }}>Farmer has Physical Disability</label>
                </div>

                <div className="form-group full-width">
                  <label>What support are you looking for? (AI Semantic Search Keywords)</label>
                  <input 
                    type="text" 
                    placeholder="e.g. drip irrigation subsidy, organic seeds, goat farming assistance, crop insurance"
                    value={farmerProfile.keywords} 
                    onChange={(e) => setFarmerProfile({...farmerProfile, keywords: e.target.value})}
                  />
                </div>
              </div>

              <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
                🔍 Find Eligible Recommended Schemes
              </button>
            </form>
          </div>

          {/* Results List */}
          {recommendations.length > 0 && (
            <div>
              <h2 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--brand-dark)', margin: '20px 0 10px 0' }}>Top Recommended Matches</h2>
              <div className="results-container">
                {recommendations.map((rec, index) => (
                  <div key={index} className="recommendation-card">
                    <div className="score-badge">
                      {rec.score}%
                      <span>Match</span>
                    </div>
                    <div className="card-details">
                      <div className="card-title-section">
                        <div>
                          <h3>{rec.scheme}</h3>
                          <span style={{ fontSize: '13px', color: 'var(--text-muted)', fontWeight: 600 }}>{rec.schemeDetails.shortTitle || ''}</span>
                        </div>
                        <span className="card-badge">{rec.schemeDetails.level}</span>
                      </div>
                      
                      <div className="reason-box">
                        💡 {rec.reason}
                      </div>

                      <div className="scheme-metadata">
                        <div className="meta-item">📍 State: {rec.schemeDetails.state}</div>
                        <div className="meta-item">🏢 Department: {rec.schemeDetails.department || 'N/A'}</div>
                        <div className="meta-item">👥 Beneficiary: {rec.schemeDetails.beneficiary || 'N/A'}</div>
                        <div className="meta-item">📝 Mode: {rec.schemeDetails.applicationMode}</div>
                      </div>

                      <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
                        <button 
                          className="btn btn-secondary btn-sm"
                          onClick={() => setExpandedScheme(expandedScheme === index ? null : index)}
                        >
                          {expandedScheme === index ? '🔼 Hide Scheme Details' : '🔽 View Scheme Details'}
                        </button>
                        {rec.schemeDetails.applicationLink && (
                          <a 
                            href={rec.schemeDetails.applicationLink} 
                            target="_blank" 
                            rel="noopener noreferrer" 
                            className="btn btn-primary btn-sm"
                          >
                            🔗 Apply Link
                          </a>
                        )}
                      </div>

                      {/* Expansion details */}
                      {expandedScheme === index && (
                        <div className="details-panel">
                          <div className="details-section">
                            <h4>Description</h4>
                            <p>{rec.schemeDetails.description}</p>
                          </div>
                          
                          <div className="details-section">
                            <h4>Benefits Provided</h4>
                            <p>{rec.schemeDetails.benefits}</p>
                          </div>

                          <div className="details-grid">
                            <div className="details-section">
                              <h4>Eligibility Criteria</h4>
                              <p>{rec.schemeDetails.eligibility}</p>
                            </div>
                            
                            <div className="details-section">
                              <h4>Documents Required</h4>
                              <div className="pill-list">
                                {rec.schemeDetails.documents.map((doc, idx) => (
                                  <span key={idx} className="pill-item">{doc}</span>
                                ))}
                              </div>
                            </div>
                          </div>

                          {rec.schemeDetails.faq && rec.schemeDetails.faq.length > 0 && (
                            <div className="details-section">
                              <h4>Frequently Asked Questions</h4>
                              <div className="faq-list">
                                {rec.schemeDetails.faq.map((f, idx) => (
                                  <div key={idx} className="faq-item">
                                    <div className="faq-q">Q: {f.question}</div>
                                    <div className="faq-a">A: {f.answer}</div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: SEARCH SCHEMES */}
      {activeTab === 'search' && !loading && (
        <div className="tab-content">
          <div className="glass-card" style={{ marginBottom: '30px' }}>
            <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--brand-dark)', marginBottom: '15px' }}>Search Government Schemes</h2>
            <form onSubmit={handleSearch} style={{ display: 'flex', gap: '12px' }}>
              <input 
                type="text" 
                placeholder="Enter query words (e.g. goat farming, machinery subsidy, drip)..."
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                style={{ flex: 1 }}
              />
              <button type="submit" className="btn btn-primary">
                🔍 Search
              </button>
            </form>
          </div>

          {searchResults.length > 0 && (
            <div className="results-container">
              <h3 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--brand-dark)' }}>Found {searchResults.length} matched schemes</h3>
              {searchResults.map((scheme, index) => (
                <div key={index} className="recommendation-card">
                  <div className="card-details">
                    <div className="card-title-section">
                      <div>
                        <h3>{scheme.name}</h3>
                        <span style={{ fontSize: '13px', color: 'var(--text-muted)', fontWeight: 600 }}>{scheme.shortTitle || ''}</span>
                      </div>
                      <span className="card-badge" style={{ background: '#dbeafe', color: '#1e40af' }}>{scheme.level}</span>
                    </div>

                    <div className="scheme-metadata">
                      <div className="meta-item">📍 State: {scheme.state}</div>
                      <div className="meta-item">🏢 Department: {scheme.department || 'N/A'}</div>
                      <div className="meta-item">👥 Beneficiary: {scheme.beneficiary || 'N/A'}</div>
                      <div className="meta-item">📝 Mode: {scheme.applicationMode}</div>
                    </div>

                    <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
                      <button 
                        className="btn btn-secondary btn-sm"
                        onClick={() => setExpandedScheme(expandedScheme === index ? null : index)}
                      >
                        {expandedScheme === index ? '🔼 Hide Details' : '🔽 View Details'}
                      </button>
                      {scheme.applicationLink && (
                        <a 
                          href={scheme.applicationLink} 
                          target="_blank" 
                          rel="noopener noreferrer" 
                          className="btn btn-primary btn-sm"
                        >
                          🔗 Apply Link
                        </a>
                      )}
                    </div>

                    {/* Expansion details */}
                    {expandedScheme === index && (
                      <div className="details-panel">
                        <div className="details-section">
                          <h4>Description</h4>
                          <p>{scheme.description}</p>
                        </div>
                        
                        <div className="details-section">
                          <h4>Benefits Provided</h4>
                          <p>{scheme.benefits}</p>
                        </div>

                        <div className="details-grid">
                          <div className="details-section">
                            <h4>Eligibility Criteria</h4>
                            <p>{scheme.eligibility}</p>
                          </div>
                          
                          <div className="details-section">
                            <h4>Documents Required</h4>
                            <div className="pill-list">
                              {scheme.documents.map((doc, idx) => (
                                <span key={idx} className="pill-item">{doc}</span>
                              ))}
                            </div>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
          {searchResults.length === 0 && searched && (
            <div className="glass-card" style={{ textAlign: 'center', padding: '40px' }}>
              <p style={{ fontWeight: 600, color: 'var(--text-muted)' }}>No schemes matched your search query. Try entering basic keywords like 'poultry', 'irrigation', or 'seeds'.</p>
            </div>
          )}
        </div>
      )}

      {/* TAB 3: ADMIN PANEL */}
      {activeTab === 'admin' && !loading && (
        <div className="tab-content">
          {/* Dashboard Stats */}
          {stats && (
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-number">{stats.totalSchemes}</div>
                <div className="stat-label">Total Schemes</div>
              </div>
              <div className="stat-card">
                <div className="stat-number">{stats.centralSchemes}</div>
                <div className="stat-label">Central Schemes</div>
              </div>
              <div className="stat-card">
                <div className="stat-number">{stats.stateSchemes}</div>
                <div className="stat-label">State Level</div>
              </div>
              <div className="stat-card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <button onClick={handleReloadDatabase} className="btn btn-primary btn-sm" style={{ alignSelf: 'center' }}>
                  🔄 Sync/Reload Seed File
                </button>
              </div>
            </div>
          )}

          {/* Form Toggle button */}
          {!showAddForm ? (
            <div className="glass-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', gap: '16px' }}>
                <h2 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--brand-dark)' }}>Managed Scheme Registry</h2>
                <button onClick={handleInitAdd} className="btn btn-primary btn-sm">
                  ➕ Add New Scheme
                </button>
              </div>

              <div style={{ marginBottom: '20px' }}>
                <input 
                  type="text" 
                  placeholder="Filter listed schemes by name, state..."
                  value={adminSearch}
                  onChange={(e) => setAdminSearch(e.target.value)}
                />
              </div>

              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Scheme Name</th>
                      <th>Level</th>
                      <th>State</th>
                      <th>Department</th>
                      <th style={{ textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredSchemes.map((s) => (
                      <tr key={s.id}>
                        <td style={{ fontWeight: 600 }}>{s.name}</td>
                        <td><span className="pill-item" style={{ background: '#eff6ff', color: '#1e40af' }}>{s.level}</span></td>
                        <td>{s.state}</td>
                        <td style={{ fontSize: '13px', color: 'var(--text-muted)' }}>{s.department || 'N/A'}</td>
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'inline-flex', gap: '8px' }}>
                            <button onClick={() => handleInitEdit(s)} className="btn btn-secondary btn-sm" style={{ padding: '6px 12px' }}>
                              ✏️ Edit
                            </button>
                            <button onClick={() => handleDeleteScheme(s.id)} className="btn btn-danger btn-sm" style={{ padding: '6px 12px' }}>
                              🗑️ Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            /* CREATE / EDIT FORM */
            <div className="glass-card">
              <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--brand-dark)', marginBottom: '20px' }}>
                {editingScheme ? '✏️ Edit Existing Scheme' : '➕ Create New Scheme Profile'}
              </h2>
              
              <form onSubmit={handleSubmitSchemeForm}>
                <div className="form-grid">
                  <div className="form-group full-width">
                    <label>Scheme Name *</label>
                    <input 
                      type="text" 
                      required
                      value={schemeForm.name} 
                      onChange={(e) => setSchemeForm({...schemeForm, name: e.target.value})}
                    />
                  </div>

                  <div className="form-group">
                    <label>Short Title</label>
                    <input 
                      type="text" 
                      value={schemeForm.shortTitle} 
                      onChange={(e) => setSchemeForm({...schemeForm, shortTitle: e.target.value})}
                    />
                  </div>

                  <div className="form-group">
                    <label>Level</label>
                    <select 
                      value={schemeForm.level} 
                      onChange={(e) => setSchemeForm({...schemeForm, level: e.target.value, state: e.target.value === 'Central' ? 'Central' : schemeForm.state})}
                    >
                      <option>Central</option>
                      <option>State</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label>State Name</label>
                    <input 
                      type="text" 
                      disabled={schemeForm.level === 'Central'}
                      value={schemeForm.level === 'Central' ? 'Central' : schemeForm.state} 
                      onChange={(e) => setSchemeForm({...schemeForm, state: e.target.value})}
                    />
                  </div>

                  <div className="form-group">
                    <label>Department / Ministry</label>
                    <input 
                      type="text" 
                      value={schemeForm.department} 
                      onChange={(e) => setSchemeForm({...schemeForm, department: e.target.value})}
                    />
                  </div>

                  <div className="form-group">
                    <label>Beneficiary</label>
                    <input 
                      type="text" 
                      value={schemeForm.beneficiary} 
                      onChange={(e) => setSchemeForm({...schemeForm, beneficiary: e.target.value})}
                    />
                  </div>

                  <div className="form-group">
                    <label>Application Mode</label>
                    <input 
                      type="text" 
                      value={schemeForm.applicationMode} 
                      onChange={(e) => setSchemeForm({...schemeForm, applicationMode: e.target.value})}
                    />
                  </div>

                  <div className="form-group full-width">
                    <label>Application Portal Link</label>
                    <input 
                      type="url" 
                      value={schemeForm.applicationLink} 
                      onChange={(e) => setSchemeForm({...schemeForm, applicationLink: e.target.value})}
                    />
                  </div>

                  <div className="form-group full-width">
                    <label>Detailed Description</label>
                    <textarea 
                      rows="3"
                      value={schemeForm.description} 
                      onChange={(e) => setSchemeForm({...schemeForm, description: e.target.value})}
                    />
                  </div>

                  <div className="form-group full-width">
                    <label>Benefits Provided</label>
                    <textarea 
                      rows="3"
                      value={schemeForm.benefits} 
                      onChange={(e) => setSchemeForm({...schemeForm, benefits: e.target.value})}
                    />
                  </div>

                  <div className="form-group full-width">
                    <label>Eligibility Criteria</label>
                    <textarea 
                      rows="3"
                      value={schemeForm.eligibility} 
                      onChange={(e) => setSchemeForm({...schemeForm, eligibility: e.target.value})}
                    />
                  </div>

                  <div className="form-group">
                    <label>Category (comma-separated)</label>
                    <input 
                      type="text" 
                      placeholder="e.g. Agriculture, Social welfare"
                      value={schemeForm.category} 
                      onChange={(e) => setSchemeForm({...schemeForm, category: e.target.value})}
                    />
                  </div>

                  <div className="form-group">
                    <label>Subcategory (comma-separated)</label>
                    <input 
                      type="text" 
                      placeholder="e.g. Financial assistance, Citizen empowerment"
                      value={schemeForm.subcategory} 
                      onChange={(e) => setSchemeForm({...schemeForm, subcategory: e.target.value})}
                    />
                  </div>

                  <div className="form-group full-width">
                    <label>Documents Required (comma-separated)</label>
                    <input 
                      type="text" 
                      placeholder="e.g. Aadhaar Card, Landholding papers, Bank passbook"
                      value={schemeForm.documents} 
                      onChange={(e) => setSchemeForm({...schemeForm, documents: e.target.value})}
                    />
                  </div>
                </div>

                {/* FAQ Entry */}
                <div style={{ border: '1px solid rgba(0,0,0,0.08)', padding: '16px', borderRadius: '8px', marginBottom: '24px' }}>
                  <h4 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--brand-dark)', marginBottom: '12px' }}>Scheme FAQs</h4>
                  
                  {schemeForm.faq.length > 0 && (
                    <div className="faq-list" style={{ marginBottom: '16px' }}>
                      {schemeForm.faq.map((item, idx) => (
                        <div key={idx} className="faq-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <div>
                            <div className="faq-q">Q: {item.question}</div>
                            <div className="faq-a">A: {item.answer}</div>
                          </div>
                          <button type="button" onClick={() => removeFaqItem(idx)} className="btn btn-danger btn-sm" style={{ padding: '4px 8px' }}>
                            Remove
                          </button>
                        </div>
                      ))}
                    </div>
                  )}

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <input 
                      type="text" 
                      placeholder="Question..."
                      value={newFaq.question}
                      onChange={(e) => setNewFaq({...newFaq, question: e.target.value})}
                    />
                    <textarea 
                      placeholder="Answer..."
                      value={newFaq.answer}
                      onChange={(e) => setNewFaq({...newFaq, answer: e.target.value})}
                    />
                    <button type="button" onClick={addFaqItem} className="btn btn-secondary btn-sm" style={{ alignSelf: 'flex-start' }}>
                      ➕ Add FAQ Item
                    </button>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                  <button type="button" onClick={() => setShowAddForm(false)} className="btn btn-secondary">
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary">
                    💾 Save Scheme Profile
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
