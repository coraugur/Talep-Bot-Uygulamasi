import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getTalep } from '../api';
import type { Talep } from '../types';

export default function TalepDetail() {
  const { talepId } = useParams<{ talepId: string }>();
  const [talep, setTalep] = useState<Talep | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<string>('userStory');

  useEffect(() => {
    if (!talepId) return;
    setLoading(true);
    getTalep(talepId)
      .then(setTalep)
      .finally(() => setLoading(false));
  }, [talepId]);

  if (loading) return <div className="page"><p>Yükleniyor...</p></div>;
  if (!talep) return <div className="page"><p>Talep bulunamadı.</p></div>;

  const tabs = [
    { key: 'userStory', label: '📋 User Story', content: talep.userStory },
    { key: 'techSpec', label: '🔍 Teknik Spec', content: talep.techSpec },
    { key: 'codeOutput', label: '💻 Kod Çıktısı', content: talep.codeOutput },
    { key: 'testReport', label: '🧪 Test Raporu', content: talep.testReport },
    { key: 'deployReport', label: '🚀 Deploy Raporu', content: talep.deployReport },
  ];

  const formatJson = (raw: string | null): string => {
    if (!raw) return '';
    try {
      return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      return raw;
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Talep Detayı</h1>
        <Link to="/" className="btn-link">← Talep Listesi</Link>
      </div>

      <div className="talep-meta">
        <div className="meta-row">
          <strong>ID:</strong> <code>{talep.id}</code>
        </div>
        <div className="meta-row">
          <strong>Durum:</strong>
          <span className={`badge badge-${talep.status.toLowerCase()}`}>{talep.status}</span>
        </div>
        <div className="meta-row">
          <strong>Oluşturulma:</strong> {new Date(talep.createdAt).toLocaleString('tr-TR')}
        </div>
        <div className="meta-row">
          <strong>İterasyon:</strong> {talep.iterationCount}
        </div>
        {talep.errorMessage && (
          <div className="meta-row error-row">
            <strong>Hata:</strong> {talep.errorMessage}
          </div>
        )}
      </div>

      <div className="description-box">
        <h3>Talep Açıklaması</h3>
        <p>{talep.description}</p>
      </div>

      <div className="tabs">
        <div className="tab-headers">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              className={`tab-btn ${activeTab === tab.key ? 'active' : ''} ${tab.content ? '' : 'disabled'}`}
              onClick={() => tab.content && setActiveTab(tab.key)}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <div className="tab-content">
          {tabs.find((t) => t.key === activeTab)?.content ? (
            <pre className="output-pre">
              {formatJson(tabs.find((t) => t.key === activeTab)?.content ?? null)}
            </pre>
          ) : (
            <p className="muted">Bu adımın çıktısı henüz mevcut değil.</p>
          )}
        </div>
      </div>
    </div>
  );
}
