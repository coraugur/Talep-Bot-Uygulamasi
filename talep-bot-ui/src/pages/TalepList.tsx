import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getAllTaleps } from '../api';
import type { Talep } from '../types';

export default function TalepList() {
  const [taleps, setTaleps] = useState<Talep[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAllTaleps()
      .then(setTaleps)
      .finally(() => setLoading(false));
  }, []);

  const statusColor = (status: string) => {
    if (status === 'COMPLETED') return '#22c55e';
    if (status === 'FAILED') return '#ef4444';
    if (status === 'PENDING') return '#94a3b8';
    return '#f59e0b';
  };

  if (loading) return <div className="page"><p>Yükleniyor...</p></div>;

  return (
    <div className="page">
      <div className="page-header">
        <h1>Tüm Talepler</h1>
        <Link to="/new" className="btn-primary">+ Yeni Talep</Link>
      </div>

      {taleps.length === 0 ? (
        <div className="empty-state">
          <p>Henüz talep yok.</p>
          <Link to="/new" className="btn-primary">İlk Talebi Oluştur</Link>
        </div>
      ) : (
        <div className="talep-table">
          <table>
            <thead>
              <tr>
                <th>Açıklama</th>
                <th>Durum</th>
                <th>İterasyon</th>
                <th>Tarih</th>
                <th>İşlem</th>
              </tr>
            </thead>
            <tbody>
              {taleps.map((t) => (
                <tr key={t.id}>
                  <td className="desc-cell">
                    {t.description.substring(0, 100)}
                    {t.description.length > 100 ? '...' : ''}
                  </td>
                  <td>
                    <span
                      className="status-dot"
                      style={{ backgroundColor: statusColor(t.status) }}
                    />
                    {t.status}
                  </td>
                  <td>{t.iterationCount}</td>
                  <td>{new Date(t.createdAt).toLocaleString('tr-TR')}</td>
                  <td>
                    <Link to={`/talep/${t.id}`} className="btn-link">Detay</Link>
                    {' '}
                    {!['COMPLETED', 'FAILED'].includes(t.status) && (
                      <Link to={`/pipeline/${t.id}`} className="btn-link">Pipeline</Link>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
