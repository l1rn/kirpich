import { Alert, Card, Flex, Segmented, Table, Tag, Typography } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { api } from '../../api';
import { StatusTag } from '../../components/StatusTag';
import { formatDateTime, formatRub } from '../../format';
import type { BankRow, Status } from '../../types';
import { usePolling } from '../../usePolling';

const FILTERS: { label: string; value: Status | 'ALL' }[] = [
  { label: 'На ручной проверке', value: 'MANUAL_REVIEW' },
  { label: 'Ждём клиента', value: 'NEEDS_INFO' },
  { label: 'Проверки идут', value: 'CHECKING' },
  { label: 'Все', value: 'ALL' },
];

const GRADE_COLOR: Record<string, string> = { A: 'green', B: 'blue', C: 'orange', D: 'red' };

/** Очередь кредитного специалиста. По умолчанию — заявки, которые ждут его решения. */
export function QueuePage() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<Status | 'ALL'>('MANUAL_REVIEW');
  const { data, error } = usePolling(() => api.bankQueue(filter === 'ALL' ? undefined : filter), () => 4000);
  const rows = data ?? [];

  return (
    <Flex vertical gap="large">
      <Typography.Title level={2} style={{ margin: 0 }}>
        Очередь заявок
      </Typography.Title>
      <Segmented options={FILTERS} value={filter} onChange={(value) => setFilter(value as Status | 'ALL')} />
      {error && <Alert type="error" showIcon title={error} />}
      <Card>
        <Table<BankRow>
          rowKey="id"
          loading={!data && !error}
          dataSource={rows}
          pagination={{ pageSize: 20, hideOnSinglePage: true }}
          onRow={(row) => ({ onClick: () => navigate(`/bank/applications/${row.id}`), style: { cursor: 'pointer' } })}
          locale={{ emptyText: 'Заявок нет' }}
          columns={[
            { title: '№', dataIndex: 'number' },
            {
              title: 'Компания',
              render: (_, row) => (
                <>
                  {row.companyName}
                  <br />
                  <Typography.Text type="secondary">ИНН {row.companyInn}</Typography.Text>
                </>
              ),
            },
            { title: 'Продукт', dataIndex: 'productName' },
            { title: 'Сумма', align: 'right', render: (_, row) => `${formatRub(row.amount)} / ${row.termMonths} мес.` },
            { title: 'Статус', render: (_, row) => <StatusTag status={row.status} label={row.statusLabel} /> },
            {
              title: 'Оценка',
              render: (_, row) => (row.grade ? <Tag color={GRADE_COLOR[row.grade]}>{`${row.grade} · ${row.score}`}</Tag> : '—'),
            },
            { title: 'Исполнитель', render: (_, row) => row.assignee ?? <Typography.Text type="secondary">не назначен</Typography.Text> },
            { title: 'Обновлена', render: (_, row) => formatDateTime(row.updatedAt) },
          ]}
        />
      </Card>
    </Flex>
  );
}
