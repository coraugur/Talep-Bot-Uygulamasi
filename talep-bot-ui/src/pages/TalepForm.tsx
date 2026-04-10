import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createTalep, runPipeline } from '../api';

export default function TalepForm() {
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!description.trim()) return;

    setLoading(true);
    setError(null);

    try {
      const talep = await createTalep(description.trim());
      await runPipeline(talep.id);
      navigate(`/pipeline/${talep.id}`);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Talep oluşturulamadı';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>Yeni Talep Oluştur</h1>
      <p className="subtitle">İş biriminizin talebini aşağıya yazın. Pipeline otomatik başlayacaktır.</p>

      <form onSubmit={handleSubmit} className="talep-form">
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Örn: Kullanıcıların profil sayfasında adres bilgilerini güncelleyebilmesi gerekiyor. Adres alanları: il, ilçe, mahalle, sokak, bina no, daire no. Validasyon kuralları olmalı."
          rows={6}
          disabled={loading}
        />

        {error && <div className="error-message">{error}</div>}

        <button type="submit" disabled={loading || !description.trim()}>
          {loading ? 'Pipeline Başlatılıyor...' : 'Talep Oluştur & Pipeline Başlat'}
        </button>
      </form>
    </div>
  );
}
