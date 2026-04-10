import { useEffect, useState, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getTalep, subscribePipelineStream } from '../api';
import type { Talep, PipelineEvent } from '../types';

const STAGE_ORDER = [
  'PO_PROCESSING',
  'ANALYST_PROCESSING',
  'DEVELOPER_PROCESSING',
  'TESTER_PROCESSING',
  'DEPLOYMENT_PROCESSING',
];

const STAGE_LABELS: Record<string, string> = {
  PO_PROCESSING: 'Product Owner',
  ANALYST_PROCESSING: 'Analist',
  DEVELOPER_PROCESSING: 'Developer',
  TESTER_PROCESSING: 'Tester',
  DEPLOYMENT_PROCESSING: 'Deployment',
};

const STAGE_ICONS: Record<string, string> = {
  PO_PROCESSING: '📋',
  ANALYST_PROCESSING: '🔍',
  DEVELOPER_PROCESSING: '💻',
  TESTER_PROCESSING: '🧪',
  DEPLOYMENT_PROCESSING: '🚀',
};

type StageStatus = 'waiting' | 'active' | 'completed' | 'failed';

interface StageState {
  status: StageStatus;
  message: string;
  data: string | null;
}

export default function PipelineDashboard() {
  const { talepId } = useParams<{ talepId: string }>();
  const [talep, setTalep] = useState<Talep | null>(null);
  const [stages, setStages] = useState<Record<string, StageState>>(() => {
    const init: Record<string, StageState> = {};
    for (const s of STAGE_ORDER) {
      init[s] = { status: 'waiting', message: '', data: null };
    }
    return init;
  });
  const [events, setEvents] = useState<PipelineEvent[]>([]);
  const [pipelineDone, setPipelineDone] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!talepId) return;

    getTalep(talepId).then(setTalep);

    const es = subscribePipelineStream(
      talepId,
      (rawData) => {
        try {
          const event: PipelineEvent = JSON.parse(rawData);
          setEvents((prev) => [...prev, event]);

          setStages((prev) => {
            const next = { ...prev };
            const stage = event.stage;
            if (!next[stage]) return next;

            if (event.status === 'STARTED') {
              next[stage] = { status: 'active', message: event.message, data: null };
            } else if (event.status === 'COMPLETED') {
              next[stage] = { status: 'completed', message: event.message, data: event.data };
            } else if (event.status === 'FAILED') {
              next[stage] = { status: 'failed', message: event.message, data: event.data };
            }
            return next;
          });

          if (
            event.stage === 'PIPELINE' &&
            (event.status === 'COMPLETED' || event.status === 'FAILED')
          ) {
            setPipelineDone(true);
            getTalep(talepId).then(setTalep);
          }
        } catch {
          // ignore malformed events
        }
      },
      () => {
        // SSE connection closed (normal after pipeline ends)
        setPipelineDone(true);
        getTalep(talepId).then(setTalep);
      }
    );

    eventSourceRef.current = es;

    return () => {
      es.close();
    };
  }, [talepId]);

  const statusClass = (s: StageStatus) => {
    switch (s) {
      case 'active': return 'stage-active';
      case 'completed': return 'stage-completed';
      case 'failed': return 'stage-failed';
      default: return 'stage-waiting';
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Pipeline Dashboard</h1>
        {talep && (
          <Link to={`/talep/${talep.id}`} className="btn-link">
            Talep Detayı →
          </Link>
        )}
      </div>

      {talep && (
        <div className="talep-summary">
          <strong>Talep:</strong> {talep.description.substring(0, 200)}
          {talep.description.length > 200 ? '...' : ''}
          <span className={`badge badge-${talep.status.toLowerCase()}`}>
            {talep.status}
          </span>
          {talep.iterationCount > 0 && (
            <span className="iteration-badge">İterasyon: {talep.iterationCount}</span>
          )}
        </div>
      )}

      <div className="pipeline-stages">
        {STAGE_ORDER.map((stage, idx) => (
          <div key={stage} className="stage-wrapper">
            <div className={`stage-card ${statusClass(stages[stage].status)}`}>
              <div className="stage-icon">{STAGE_ICONS[stage]}</div>
              <div className="stage-info">
                <h3>{STAGE_LABELS[stage]}</h3>
                <p className="stage-status-text">
                  {stages[stage].status === 'waiting' && 'Bekliyor...'}
                  {stages[stage].status === 'active' && stages[stage].message}
                  {stages[stage].status === 'completed' && 'Tamamlandı ✓'}
                  {stages[stage].status === 'failed' && `Hata: ${stages[stage].message}`}
                </p>
              </div>
              {stages[stage].status === 'active' && <div className="spinner" />}
            </div>
            {idx < STAGE_ORDER.length - 1 && <div className="stage-arrow">→</div>}
          </div>
        ))}
      </div>

      {pipelineDone && talep && (
        <div className={`pipeline-result ${talep.status === 'COMPLETED' ? 'result-success' : 'result-failure'}`}>
          <h2>{talep.status === 'COMPLETED' ? '✅ Pipeline Başarılı!' : '❌ Pipeline Başarısız'}</h2>
          {talep.errorMessage && <p className="error-detail">{talep.errorMessage}</p>}
          <Link to={`/talep/${talep.id}`} className="btn-primary">
            Detaylı Raporu Gör
          </Link>
        </div>
      )}

      <div className="event-log">
        <h2>Olay Günlüğü</h2>
        <div className="event-list">
          {events.length === 0 && <p className="muted">Henüz olay yok. Pipeline başlamasını bekliyoruz...</p>}
          {events.map((ev, i) => (
            <div key={i} className={`event-item event-${ev.status.toLowerCase()}`}>
              <span className="event-time">
                {new Date(ev.timestamp).toLocaleTimeString('tr-TR')}
              </span>
              <span className="event-stage">[{ev.stage}]</span>
              <span className="event-msg">{ev.message}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
