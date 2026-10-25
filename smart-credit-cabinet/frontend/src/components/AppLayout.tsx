import { LogoutOutlined } from '@ant-design/icons';
import { Button, Flex, Layout, Typography } from 'antd';
import { Link, Outlet, useNavigate } from 'react-router';
import { useAuth } from '../auth';

const ROLE: Record<string, string> = {
  DIRECTOR: 'Руководитель',
  ACCOUNTANT: 'Бухгалтер',
  UNDERWRITER: 'Кредитный специалист банка',
};

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const home = user?.role === 'UNDERWRITER' ? '/bank' : '/applications';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Header style={{ background: '#fff', borderBottom: '1px solid #f0f0f0', paddingInline: 24 }}>
        <Flex justify="space-between" align="center" style={{ height: '100%' }}>
          <Link to={home}>
            <Typography.Title level={4} style={{ margin: 0 }}>
              Умный кредитный кабинет
            </Typography.Title>
          </Link>
          {user && (
            <Flex gap="middle" align="center">
              <Typography.Text>
                {user.fullName}
                <Typography.Text type="secondary">
                  {' · '}
                  {ROLE[user.role]}
                  {user.company ? `, ${user.company.name}` : ''}
                </Typography.Text>
              </Typography.Text>
              <Button
                icon={<LogoutOutlined />}
                onClick={() => {
                  logout();
                  navigate('/login');
                }}
              >
                Выйти
              </Button>
            </Flex>
          )}
        </Flex>
      </Layout.Header>
      <Layout.Content style={{ padding: 24, maxWidth: 1200, width: '100%', margin: '0 auto' }}>
        <Outlet />
      </Layout.Content>
    </Layout>
  );
}
