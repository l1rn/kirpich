import { Tag } from 'antd';
import type { Status } from '../types';

const COLORS: Record<Status, string> = {
  DRAFT: 'default',
  CHECKING: 'processing',
  NEEDS_INFO: 'warning',
  MANUAL_REVIEW: 'purple',
  OFFERED: 'cyan',
  DECLINED: 'error',
  ISSUED: 'success',
  CANCELLED: 'default',
};

export function StatusTag({ status, label }: { status: Status; label: string }) {
  return <Tag color={COLORS[status]}>{label}</Tag>;
}
