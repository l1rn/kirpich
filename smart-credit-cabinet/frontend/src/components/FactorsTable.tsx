import { Table, Typography } from 'antd';
import type { Factor } from '../types';

/** Объяснимость скоринга: какой фактор сколько баллов добавил или снял. */
export function FactorsTable({ factors }: { factors: Factor[] }) {
  return (
    <Table<Factor>
      size="small"
      rowKey="code"
      pagination={false}
      dataSource={factors}
      columns={[
        { title: 'Фактор', dataIndex: 'label' },
        { title: 'Значение', dataIndex: 'value' },
        {
          title: 'Баллы',
          dataIndex: 'points',
          align: 'right',
          render: (points: number) => (
            <Typography.Text type={points > 0 ? 'success' : points < 0 ? 'danger' : 'secondary'} strong>
              {points > 0 ? `+${points}` : points}
            </Typography.Text>
          ),
        },
      ]}
    />
  );
}
