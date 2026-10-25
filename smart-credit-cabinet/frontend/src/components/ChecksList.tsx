import { CheckCircleFilled, ClockCircleOutlined, ExclamationCircleFilled, LoadingOutlined } from '@ant-design/icons';
import { List, Typography } from 'antd';
import type { ReactNode } from 'react';
import type { Check } from '../types';

const ICONS: Record<Check['status'], ReactNode> = {
  PENDING: <ClockCircleOutlined style={{ color: '#bfbfbf' }} />,
  RUNNING: <LoadingOutlined style={{ color: '#1677ff' }} />,
  OK: <CheckCircleFilled style={{ color: '#52c41a' }} />,
  FAILED: <ExclamationCircleFilled style={{ color: '#faad14' }} />,
};

const STATUS_TEXT: Record<Check['status'], string> = {
  PENDING: 'в очереди',
  RUNNING: 'выполняется',
  OK: 'готово',
  FAILED: 'не удалось',
};

/** Шаги конвейера внутри статуса «Проверяем заявку». */
export function ChecksList({ checks }: { checks: Check[] }) {
  if (checks.length === 0) return null;
  return (
    <List
      size="small"
      dataSource={checks}
      renderItem={(check) => (
        <List.Item>
          <List.Item.Meta
            avatar={ICONS[check.status]}
            title={check.label}
            description={
              <Typography.Text type="secondary">
                {STATUS_TEXT[check.status]}
                {check.attempts > 1 ? ` · попыток: ${check.attempts}` : ''}
                {check.error && check.status !== 'OK' ? ` · ${check.error}` : ''}
              </Typography.Text>
            }
          />
        </List.Item>
      )}
    />
  );
}
