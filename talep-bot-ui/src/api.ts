import axios from 'axios';
import type { Talep } from './types';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

export async function createTalep(description: string): Promise<Talep> {
  const { data } = await api.post<Talep>('/talep', { description });
  return data;
}

export async function getTalep(id: string): Promise<Talep> {
  const { data } = await api.get<Talep>(`/talep/${id}`);
  return data;
}

export async function getAllTaleps(): Promise<Talep[]> {
  const { data } = await api.get<Talep[]>('/talep');
  return data;
}

export async function runPipeline(talepId: string): Promise<void> {
  await api.post(`/pipeline/run/${talepId}`);
}

export async function runPipelineSync(talepId: string): Promise<Talep> {
  const { data } = await api.post<Talep>(`/pipeline/run-sync/${talepId}`);
  return data;
}

export function subscribePipelineStream(
  talepId: string,
  onEvent: (event: string) => void,
  onError?: (err: Event) => void
): EventSource {
  const es = new EventSource(`http://localhost:8080/api/pipeline/stream/${talepId}`);
  es.addEventListener('pipeline-event', (e: MessageEvent) => {
    onEvent(e.data);
  });
  es.onerror = (err) => {
    onError?.(err);
  };
  return es;
}
