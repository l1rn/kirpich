import { Alert, Button, Card, Flex, Form, Input, List, Typography } from 'antd';
import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router';
import { ApiError } from '../api';
import { useAuth } from '../auth';

// Демо-аккаунты из DemoDataSeeder: у каждой компании свой сценарий в заглушках mocks/
const DEMO_ACCOUNTS = [
  { login: 'romashka', who: 'ООО «Ромашка», руководитель', story: 'одобрение сразу' },
  { login: 'romashka-buh', who: 'ООО «Ромашка», бухгалтер', story: 'не может подписать договор' },
  { login: 'petrov', who: 'ИП Петров', story: 'контрпредложение' },
  { login: 'stroy', who: 'ООО «СтройМонтаж»', story: 'документы и ручная проверка' },
  { login: 'zakat', who: 'ООО «Закат»', story: 'отказ: компания ликвидируется' },
  { login: 'tishina', who: 'ООО «Тишина»', story: 'БКИ не отвечает → специалист' },
  { login: 'underwriter', who: 'Кредитный специалист банка', story: 'очередь и решения' },
];

// Пароль демо-стенда задаётся при сборке (VITE_DEMO_PASSWORD); без него кнопки только подставляют логин
const DEMO_PASSWORD = import.meta.env.VITE_DEMO_PASSWORD as string | undefined;

interface LoginForm {
  login: string;
  password: string;
}

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [form] = Form.useForm<LoginForm>();
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    return <Navigate to={user.role === 'UNDERWRITER' ? '/bank' : '/applications'} replace />;
  }

  const submit = async (values: LoginForm) => {
    setSubmitting(true);
    setError(null);
    try {
      const signedIn = await login(values.login, values.password);
      navigate(signedIn.role === 'UNDERWRITER' ? '/bank' : '/applications');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Не удалось войти');
    } finally {
      setSubmitting(false);
    }
  };

  const pick = (demoLogin: string) => {
    form.setFieldsValue({ login: demoLogin, password: DEMO_PASSWORD ?? '' });
    if (DEMO_PASSWORD) {
      void submit({ login: demoLogin, password: DEMO_PASSWORD });
    }
  };

  return (
    <Flex justify="center" align="start" gap={24} wrap style={{ minHeight: '100vh', padding: '64px 16px', background: '#f5f5f5' }}>
      <Card style={{ width: 380 }}>
        <Typography.Title level={3}>Умный кредитный кабинет</Typography.Title>
        <Typography.Paragraph type="secondary">Финансирование для малого бизнеса — от задачи до денег на счёте.</Typography.Paragraph>
        {error && <Alert type="error" title={error} showIcon style={{ marginBottom: 16 }} />}
        <Form form={form} layout="vertical" onFinish={submit} requiredMark={false}>
          <Form.Item name="login" label="Логин" rules={[{ required: true, message: 'Введите логин' }]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="Пароль" rules={[{ required: true, message: 'Введите пароль' }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            Войти
          </Button>
        </Form>
      </Card>
      <Card title="Демо-аккаунты" style={{ width: 420 }}>
        <List
          size="small"
          dataSource={DEMO_ACCOUNTS}
          renderItem={(account) => (
            <List.Item actions={[<Button key="use" size="small" onClick={() => pick(account.login)}>Войти</Button>]}>
              <List.Item.Meta title={<Typography.Text code>{account.login}</Typography.Text>} description={`${account.who} — ${account.story}`} />
            </List.Item>
          )}
        />
        {!DEMO_PASSWORD && <Typography.Text type="secondary">Пароль тестовых аккаунтов — в README.</Typography.Text>}
      </Card>
    </Flex>
  );
}
