import { PlusOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Empty, Flex, List, Typography } from 'antd';
import { Link, useNavigate } from 'react-router';
import { api } from '../../api';
import { StatusTag } from '../../components/StatusTag';
import { formatDateTime, formatRub } from '../../format';
import { usePolling } from '../../usePolling';

export function ApplicationsPage() {
  const navigate = useNavigate();
  const { data, error } = usePolling(api.applications, () => 5000);

  return (
    <Flex vertical gap="large">
      <Flex justify="space-between" align="center">
        <Typography.Title level={2} style={{ margin: 0 }}>
          Мои заявки
        </Typography.Title>
        <Button type="primary" size="large" icon={<PlusOutlined />} onClick={() => navigate('/applications/new')}>
          Получить финансирование
        </Button>
      </Flex>
      {error && <Alert type="error" title={error} showIcon />}
      <Card>
        <List
          loading={!data && !error}
          dataSource={data ?? []}
          locale={{ emptyText: <Empty description="Заявок пока нет — начните с кнопки «Получить финансирование»" /> }}
          renderItem={(app) => (
            <List.Item extra={<StatusTag status={app.status} label={app.statusLabel} />}>
              <List.Item.Meta
                title={
                  <Link to={`/applications/${app.id}`}>
                    № {app.number} · {app.productName} · {formatRub(app.amount)} на {app.termMonths} мес.
                  </Link>
                }
                description={
                  <>
                    <Typography.Text strong={app.nextAction.actor === 'CLIENT'}>{app.nextAction.title}</Typography.Text>
                    <Typography.Text type="secondary"> · обновлена {formatDateTime(app.updatedAt)}</Typography.Text>
                  </>
                }
              />
            </List.Item>
          )}
        />
      </Card>
    </Flex>
  );
}
