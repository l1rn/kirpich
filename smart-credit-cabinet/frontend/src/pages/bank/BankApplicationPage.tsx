import { ArrowLeftOutlined, MinusCircleOutlined, PaperClipOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Col,
  Collapse,
  Descriptions,
  Flex,
  Form,
  Input,
  InputNumber,
  List,
  Popconfirm,
  Result,
  Row,
  Select,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import { useEffect, useState, type ReactNode } from 'react';
import { Link, useParams } from 'react-router';
import { api, ApiError, downloadDocument } from '../../api';
import { EventsTimeline } from '../../components/EventsTimeline';
import { FactorsTable } from '../../components/FactorsTable';
import { StatusTag } from '../../components/StatusTag';
import { formatDate, formatRub, rubFormatter, rubParser } from '../../format';
import type { BankDetail, Catalog, DecisionFull, Source } from '../../types';
import { usePolling } from '../../usePolling';

const OUTCOME: Record<DecisionFull['outcome'], { text: string; color: string }> = {
  APPROVE: { text: 'Одобрить', color: 'green' },
  COUNTER_OFFER: { text: 'Контрпредложение', color: 'cyan' },
  MANUAL_REVIEW: { text: 'На ручную проверку', color: 'purple' },
  DECLINE: { text: 'Отказ', color: 'red' },
};

const SOURCE_STATUS: Record<Source['status'], { text: string; color: string }> = {
  PENDING: { text: 'в очереди', color: 'default' },
  RUNNING: { text: 'выполняется', color: 'processing' },
  OK: { text: 'получено', color: 'green' },
  FAILED: { text: 'нет данных', color: 'orange' },
};

interface CounterForm {
  productCode: string;
  amount: number;
  termMonths: number;
  rate: number;
  note?: string;
}

interface InfoForm {
  items: { label: string; type: 'TEXT' | 'FILE' }[];
  note?: string;
}

/** Карточка заявки для кредитного специалиста: данные источников, скоринг, рекомендация движка и решение. */
export function BankApplicationPage() {
  const id = Number(useParams().id);
  const { data, error, setData } = usePolling(() => api.bankApplication(id), (d) => (d?.application.summary.status === 'CHECKING' ? 1500 : 5000));
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [declineReason, setDeclineReason] = useState('');
  const [counterForm] = Form.useForm<CounterForm>();
  const [infoForm] = Form.useForm<InfoForm>();

  useEffect(() => {
    api.catalog().then(setCatalog).catch(() => undefined);
  }, []);

  const engine = data?.decisions.filter((d) => d.source === 'ENGINE').at(-1);
  const recommendation = engine?.proposal;
  useEffect(() => {
    if (recommendation) counterForm.setFieldsValue({ ...recommendation, note: undefined });
  }, [recommendation, counterForm]);

  const run = async (action: () => Promise<BankDetail>) => {
    setBusy(true);
    setActionError(null);
    try {
      setData(await action());
    } catch (e) {
      setActionError(e instanceof ApiError ? e.errors.join('\n') : 'Что-то пошло не так');
    } finally {
      setBusy(false);
    }
  };

  if (error && !data) return <Result status="warning" title={error} />;
  if (!data) return <Card loading />;

  const app = data.application;
  const summary = app.summary;
  const inReview = summary.status === 'MANUAL_REVIEW';
  // nextAction написан для клиента; специалисту на ручной проверке подсказываем его собственный шаг
  const hint = inReview
    ? { title: 'Нужно ваше решение', description: 'Одобрите, предложите другие условия, запросите информацию у клиента или откажите — вкладки ниже.' }
    : summary.nextAction;

  return (
    <Flex vertical gap="large">
      <Link to="/bank">
        <ArrowLeftOutlined /> Очередь заявок
      </Link>
      <Flex justify="space-between" align="center" wrap gap="middle">
        <Typography.Title level={2} style={{ margin: 0 }}>
          № {summary.number} · {data.company.name} <StatusTag status={summary.status} label={summary.statusLabel} />
        </Typography.Title>
        <Flex gap="middle" align="center">
          <Typography.Text type="secondary">Исполнитель: {data.assignee ?? 'не назначен'}</Typography.Text>
          <Button onClick={() => run(() => api.assign(id))} loading={busy}>
            Взять в работу
          </Button>
        </Flex>
      </Flex>
      <Alert type={inReview ? 'warning' : 'info'} showIcon title={hint.title} description={hint.description} />
      {actionError && <Alert type="error" showIcon title={<span style={{ whiteSpace: 'pre-line' }}>{actionError}</span>} />}

      <Row gutter={[24, 24]}>
        <Col xs={24} lg={14}>
          <Flex vertical gap="large">
            {engine && (
              <Card
                title="Оценка движка"
                extra={
                  <Flex gap="small">
                    <Tag color={OUTCOME[engine.outcome].color}>{OUTCOME[engine.outcome].text}</Tag>
                    {engine.grade && <Tag>{`Грейд ${engine.grade} · ${engine.score} из 100`}</Tag>}
                  </Flex>
                }
              >
                {engine.stopFactors.length > 0 && (
                  <Alert type="error" showIcon style={{ marginBottom: 16 }} title="Стоп-факторы" description={engine.stopFactors.map((s) => s.text).join('; ')} />
                )}
                {engine.reasons.length > 0 && (
                  <ul style={{ paddingLeft: 18 }}>
                    {engine.reasons.map((reason) => (
                      <li key={reason}>{reason}</li>
                    ))}
                  </ul>
                )}
                {recommendation && (
                  <Typography.Paragraph>
                    Рекомендация: <b>{catalog?.products.find((p) => p.code === recommendation.productCode)?.name ?? recommendation.productCode}</b>,{' '}
                    {formatRub(recommendation.amount)} на {recommendation.termMonths} мес. под {recommendation.rate}%, платёж {formatRub(recommendation.monthlyPayment)}
                  </Typography.Paragraph>
                )}
                <FactorsTable factors={engine.factors} />
              </Card>
            )}

            {inReview && (
              <Card title="Решение">
                <Tabs
                  items={[
                    {
                      key: 'approve',
                      label: 'Одобрить',
                      children: (
                        <Flex vertical gap="middle">
                          <Typography.Text>
                            На условиях заявки: {formatRub(summary.amount)} на {summary.termMonths} мес.
                            {recommendation && recommendation.productCode === summary.productCode ? `, ставка ${recommendation.rate}%` : ''}
                          </Typography.Text>
                          <Popconfirm title="Одобрить заявку на условиях клиента?" okText="Одобрить" cancelText="Отмена" onConfirm={() => run(() => api.decide(id, { outcome: 'APPROVE' }))}>
                            <Button type="primary" loading={busy} style={{ alignSelf: 'start' }}>
                              Одобрить
                            </Button>
                          </Popconfirm>
                        </Flex>
                      ),
                    },
                    {
                      key: 'counter',
                      label: 'Другие условия',
                      children: (
                        <Form form={counterForm} layout="vertical" onFinish={(v) => run(() => api.decide(id, { outcome: 'COUNTER_OFFER', ...v }))}>
                          <Row gutter={16}>
                            <Col span={12}>
                              <Form.Item name="productCode" label="Продукт" rules={[{ required: true }]}>
                                <Select options={catalog?.products.map((p) => ({ value: p.code, label: p.name }))} />
                              </Form.Item>
                            </Col>
                            <Col span={12}>
                              <Form.Item name="amount" label="Сумма" rules={[{ required: true }]}>
                                <InputNumber<number> min={1} step={100_000} style={{ width: '100%' }} formatter={rubFormatter} parser={rubParser} suffix="₽" />
                              </Form.Item>
                            </Col>
                            <Col span={12}>
                              <Form.Item name="termMonths" label="Срок, мес." rules={[{ required: true }]}>
                                <InputNumber<number> min={1} max={120} style={{ width: '100%' }} />
                              </Form.Item>
                            </Col>
                            <Col span={12}>
                              <Form.Item name="rate" label="Ставка, %" rules={[{ required: true }]}>
                                <InputNumber<number> min={0.1} max={99} step={0.5} style={{ width: '100%' }} />
                              </Form.Item>
                            </Col>
                          </Row>
                          <Form.Item name="note" label="Комментарий для клиента">
                            <Input />
                          </Form.Item>
                          <Button type="primary" htmlType="submit" loading={busy}>
                            Предложить условия
                          </Button>
                        </Form>
                      ),
                    },
                    {
                      key: 'info',
                      label: 'Запросить информацию',
                      children: (
                        <Form
                          form={infoForm}
                          layout="vertical"
                          initialValues={{ items: [{ label: '', type: 'TEXT' }] }}
                          onFinish={(v) =>
                            run(async () => {
                              const updated = await api.requestInfo(id, v.items, v.note);
                              infoForm.resetFields();
                              return updated;
                            })
                          }
                        >
                          <Form.List name="items">
                            {(fields, { add, remove }) => (
                              <>
                                {fields.map((field) => (
                                  <Flex key={field.key} gap="small" align="start">
                                    <Form.Item name={[field.name, 'label']} rules={[{ required: true, message: 'Что нужно от клиента?' }]} style={{ flex: 1 }}>
                                      <Input placeholder="Например: поясните просрочку по кредиту" />
                                    </Form.Item>
                                    <Form.Item name={[field.name, 'type']}>
                                      <Select
                                        style={{ width: 130 }}
                                        options={[
                                          { value: 'TEXT', label: 'Текст' },
                                          { value: 'FILE', label: 'Документ' },
                                        ]}
                                      />
                                    </Form.Item>
                                    {fields.length > 1 && <Button icon={<MinusCircleOutlined />} onClick={() => remove(field.name)} />}
                                  </Flex>
                                ))}
                                <Button type="dashed" icon={<PlusOutlined />} onClick={() => add({ label: '', type: 'TEXT' })} style={{ marginBottom: 16 }}>
                                  Добавить пункт
                                </Button>
                              </>
                            )}
                          </Form.List>
                          <Form.Item name="note" label="Комментарий">
                            <Input />
                          </Form.Item>
                          <Button type="primary" htmlType="submit" loading={busy}>
                            Отправить запрос клиенту
                          </Button>
                        </Form>
                      ),
                    },
                    {
                      key: 'decline',
                      label: 'Отказать',
                      children: (
                        <Flex vertical gap="middle">
                          <Input.TextArea rows={2} placeholder="Причина отказа — клиент её увидит" value={declineReason} onChange={(e) => setDeclineReason(e.target.value)} />
                          <Popconfirm title="Отказать по заявке?" okText="Отказать" okButtonProps={{ danger: true }} cancelText="Отмена" onConfirm={() => run(() => api.decide(id, { outcome: 'DECLINE', reason: declineReason }))}>
                            <Button danger disabled={!declineReason.trim()} loading={busy} style={{ alignSelf: 'start' }}>
                              Отказать
                            </Button>
                          </Popconfirm>
                        </Flex>
                      ),
                    },
                  ]}
                />
              </Card>
            )}

            <Card title="Данные источников">
              <Collapse items={data.sources.map((source) => ({ key: source.step, label: <SourceLabel source={source} />, children: <SourceData source={source} /> }))} />
            </Card>
          </Flex>
        </Col>

        <Col xs={24} lg={10}>
          <Flex vertical gap="large">
            <Card title="Заявка">
              <Descriptions
                column={1}
                size="small"
                items={[
                  { key: 'product', label: 'Продукт', children: summary.productName },
                  { key: 'purpose', label: 'Цель', children: summary.purposeLabel },
                  { key: 'amount', label: 'Сумма / срок', children: `${formatRub(summary.amount)} / ${summary.termMonths} мес.` },
                  { key: 'revenue', label: 'Выручка за 12 мес. (анкета)', children: app.revenue12m ? formatRub(app.revenue12m) : '—' },
                  { key: 'payments', label: 'Платежи по кредитам (анкета)', children: formatRub(app.existingMonthlyPayments) },
                  { key: 'inn', label: 'ИНН', children: data.company.inn },
                  { key: 'director', label: 'Руководитель', children: data.company.directorName ?? '—' },
                ]}
              />
            </Card>
            {data.decisions.filter((d) => d.source === 'UNDERWRITER').length > 0 && (
              <Card title="Решения специалиста">
                <List
                  size="small"
                  dataSource={data.decisions.filter((d) => d.source === 'UNDERWRITER')}
                  renderItem={(d) => (
                    <List.Item>
                      <Tag color={OUTCOME[d.outcome].color}>{OUTCOME[d.outcome].text}</Tag>
                      {d.proposal ? `${formatRub(d.proposal.amount)} / ${d.proposal.termMonths} мес. / ${d.proposal.rate}%` : d.reasons.join('; ')}
                    </List.Item>
                  )}
                />
              </Card>
            )}
            {app.documents.length > 0 && (
              <Card title="Документы клиента">
                <List
                  size="small"
                  dataSource={app.documents}
                  renderItem={(d) => (
                    <List.Item>
                      <Button type="link" icon={<PaperClipOutlined />} onClick={() => void downloadDocument(id, d.id, d.filename)}>
                        {d.filename}
                      </Button>
                    </List.Item>
                  )}
                />
              </Card>
            )}
            {data.infoRequests.some((r) => r.answers && Object.keys(r.answers).length > 0) && (
              <Card title="Ответы клиента">
                {data.infoRequests.map((r) =>
                  r.items
                    .filter((item) => r.answers?.[item.key])
                    .map((item) => (
                      <Typography.Paragraph key={item.key}>
                        <Typography.Text type="secondary">{item.label}</Typography.Text>
                        <br />
                        {r.answers?.[item.key]}
                      </Typography.Paragraph>
                    )),
                )}
              </Card>
            )}
            <Card title="История и аудит">
              <EventsTimeline events={app.events} />
            </Card>
          </Flex>
        </Col>
      </Row>
    </Flex>
  );
}

function SourceLabel({ source }: { source: Source }) {
  const status = SOURCE_STATUS[source.status];
  return (
    <Flex gap="small" align="center">
      {source.label}
      <Tag color={status.color}>{status.text}</Tag>
      {source.attempts > 1 && <Typography.Text type="secondary">попыток: {source.attempts}</Typography.Text>}
    </Flex>
  );
}

/** Ключевые факты из ответа источника + полный ответ, на котором принималось решение. */
function SourceData({ source }: { source: Source }) {
  const d = source.data as Record<string, any> | null;
  if (!d) return <Typography.Text type="secondary">{source.error ?? 'Данных нет'}</Typography.Text>;
  let facts: { key: string; label: string; children: ReactNode }[] = [];
  if (source.step === 'EGRUL') {
    facts = [
      { key: 'status', label: 'Статус', children: d.status === 'ACTIVE' ? 'Действующая' : <Tag color="red">{d.status}</Tag> },
      { key: 'reg', label: 'Зарегистрирована', children: formatDate(d.registrationDate) },
      { key: 'okved', label: 'ОКВЭД', children: d.okved ? `${d.okved.code} — ${d.okved.name}` : '—' },
      { key: 'director', label: 'Руководитель', children: `${d.director?.fullName ?? '—'}${d.director?.disqualified ? ' (дисквалифицирован)' : ''}` },
    ];
  } else if (source.step === 'FNS') {
    facts = [
      { key: 'revenue', label: 'Выручка по отчётности', children: d.reportedRevenue ? `${formatRub(d.reportedRevenue.amount)} за ${d.reportedRevenue.year}` : '—' },
      { key: 'debt', label: 'Налоговая задолженность', children: d.taxDebt ? <Tag color="red">{formatRub(d.taxDebtAmount)}</Tag> : 'нет' },
      { key: 'mass', label: 'Массовый адрес / руководитель', children: d.massAddress || d.massDirector ? <Tag color="orange">да</Tag> : 'нет' },
      { key: 'employees', label: 'Сотрудников', children: d.employees },
    ];
  } else if (source.step === 'BKI') {
    return (
      <Table
        size="small"
        rowKey={(loan: Record<string, any>) => `${loan.creditor}-${loan.openedAt}`}
        pagination={false}
        dataSource={(d.loans as Record<string, any>[]) ?? []}
        locale={{ emptyText: 'Кредитов не было' }}
        columns={[
          { title: 'Кредитор', dataIndex: 'creditor' },
          { title: 'Сумма', render: (_, loan) => formatRub(loan.amount) },
          { title: 'Статус', render: (_, loan) => (loan.status === 'active' ? 'действует' : 'закрыт') },
          { title: 'Платёж', render: (_, loan) => (loan.monthlyPayment ? formatRub(loan.monthlyPayment) : '—') },
          { title: 'Макс. просрочка за год', render: (_, loan) => (loan.maxOverdueDays12m ? <Tag color="orange">{loan.maxOverdueDays12m} дн.</Tag> : 'нет') },
        ]}
      />
    );
  } else {
    return <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{JSON.stringify(d, null, 2)}</pre>;
  }
  return <Descriptions column={1} size="small" items={facts} />;
}
