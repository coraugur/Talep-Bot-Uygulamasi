export type PipelineStatus =
  | 'PENDING'
  | 'PO_PROCESSING'
  | 'ANALYST_PROCESSING'
  | 'DEVELOPER_PROCESSING'
  | 'TESTER_PROCESSING'
  | 'DEPLOYMENT_PROCESSING'
  | 'COMPLETED'
  | 'FAILED';

export interface Talep {
  id: string;
  description: string;
  status: PipelineStatus;
  createdAt: string;
  updatedAt: string;
  userStory: string | null;
  techSpec: string | null;
  codeOutput: string | null;
  testReport: string | null;
  deployReport: string | null;
  errorMessage: string | null;
  iterationCount: number;
}

export interface PipelineEvent {
  talepId: string;
  stage: string;
  status: 'STARTED' | 'COMPLETED' | 'FAILED';
  message: string;
  data: string | null;
  timestamp: string;
}
