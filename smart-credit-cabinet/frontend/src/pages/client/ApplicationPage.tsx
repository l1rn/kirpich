import { ArrowLeftOutlined, PaperClipOutlined, UploadOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Flex,
  Input,
  List,
  Popconfirm,
  Result,
  Row,
  Statistic,
  Steps,
  Tag,
  Typography,
  Upload,
} from 'antd';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { api, ApiError, downloadDocument } from '../../api';
import { useAuth } from '../../auth';
import { ChecksList } from '../../components/ChecksList';
import { EventsTimeline } from '../../components/EventsTimeline';
import { FactorsTable } from '../../components/FactorsTable';
import { StatusTag } from '../../components/StatusTag';
import { formatDateTime, formatRub } from '../../format';
import type { Detail } from '../../types';
import { usePolling } from '../../usePolling';

const ACTIVE_STATUSES = ['DRAFT', 'CHECKING', 'NEEDS_INFO', 'MANUAL_REVIEW', 'OFFERED'];

/** Страница заявки клиента: этап, «что дальше», проверки, запросы банка, предложение и подписание. */
export function ApplicationPage() {
  const id = Number(useParams().id);
  const { user } = useAuth();
  // Пока идут проверки — опрашиваем чаще, чтобы было видно движение по конвейеру
  const { data, error, setData } = usePolling(
    () => api.application(id),
    (d) => (d?.summary.status === 'CHECKING' ? 1500 : d && ACTIVE_STATUSES.includes(d.summary.status) ? 5000 : 0),
  );
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [signingCode, setSigningCode] = useState<string | null>(null);
  const [codeInput, setCodeInput] = useState('');
  const [answers, setAnswers] = useState<Record<string, string>>({});

  const run = async (action: () => Promise<Detail | void>) => {
    setBusy(true);
    setActionError(null);
    try {
      const updated = await action();
      if (updated) setData(updated);
    } catch (e) {
      setActionError(e instanceof ApiError ? e.errors.join('\n') : 'Что-то пошло не так');
    } finally {
      setBusy(false);
    }
  };

  if (error && !data) return <Result status="warning" title={error} />;
  if (!data) return <Card loading />;

  const { summary, offer, infoRequest, decision } = data;
  const status = summary.status;
  const stepStatus = status === 'DECLINED' ? 'error' : status === 'ISSUED' ? 'finish' : 'process';
  const alertType = status === 'DECLINED' ? 'error' : status === 'ISSUED' ? 'success' : summary.nextAction.actor === 'CLIENT' ? 'warning' : 'info';

  return (
    <Flex vertical gap="large">
      <Link to="/applications">
        <ArrowLeftOutlined /> Мои заявки
      </Link>
      <Flex justify="space-between" align="center" wrap gap="middle">
        <Typography.Title level={2} style={{ margin: 0 }}>
          Заявка № {summary.number} <StatusTag status={status} label={summary.statusLabel} />
        </Typography.Title>
        {ACTIVE_STATUSES.includes(status) && offer?.status !== 'SIGNED' && (
          <Popconfirm title="Отозвать заявку?" okText="Отозвать" cancelText="Нет" onConfirm={() => run(() => api.cancel(id))}>
            <Button danger>Отозвать</Button>
          </Popconfirm>
        )}
      </Flex>

      {status !== 'CANCELLED' && <Steps current={data.stageIndex} status={stepStatus} items={data.stages.map((title) => ({ title }))} />}

      <Alert type={alertType} showIcon title={summary.nextAction.title} description={summary.nextAction.description} />
      {actionError && <Alert type="error" showIcon title={<span style={{ whiteSpace: 'pre-line' }}>{actionError}</span>} />}

      <Row gutter={[24, 24]}>
        <Col xs={24} lg={14}>
          <Flex vertical gap="large">
            {status === 'NEEDS_INFO' && infoRequest && (
              <Card title={infoRequest.requestedBy === 'SYSTEM' ? 'Нужны документы' : 'Запрос от кредитного специалиста'}>
                {infoRequest.note && <Typography.Paragraph>{infoRequest.note}</Typography.Paragraph>}
                <Flex vertical gap="middle">
                  {infoRequest.items.map((item) => {
                    const uploaded = data.documents.filter((d) => d.docType === item.key);
                    return item.type === 'FILE' ? (
                      <Flex key={item.key} gap="middle" align="center" wrap>
                        <Upload
                          showUploadList={false}
                          beforeUpload={(file) => {
                            void run(() => api.upload(id, item.key, file));
                            return false;
                          }}
                        >
                          <Button icon={<UploadOutlined />}>{item.label}</Button>
                        </Upload>
                        {uploaded.map((d) => (
                          <Tag key={d.id} color="green">
                            {d.filename}
                          </Tag>
                        ))}
                      </Flex>
                    ) : (
                      <div key={item.key}>
                        <Typography.Text strong>{item.label}</Typography.Text>
                        <Input.TextArea
                          rows={3}
                          value={answers[item.key] ?? ''}
                          onChange={(e) => setAnswers({ ...answers, [item.key]: e.target.value })}
                        />
                      </div>
                    );
                  })}
                  <Button type="primary" loading={busy} style={{ alignSelf: 'start' }} onClick={() => run(() => api.respond(id, infoRequest.id, answers))}>
                    Отправить ответ
                  </Button>
                </Flex>
              </Card>
            )}

            {offer && status === 'OFFERED' && (
              <Card title={offer.kind === 'COUNTER' ? 'Банк одобрил заявку на других условиях' : 'Банк одобрил заявку'}>
                <Row gutter={16}>
                  <Col span={12}>
                    <Statistic title={offer.productName} value={formatRub(offer.amount)} />
                  </Col>
                  <Col span={12}>
                    <Statistic title="Платёж в месяц" value={formatRub(offer.monthlyPayment)} />
                  </Col>
                </Row>
                <Typography.Paragraph style={{ marginTop: 16 }}>
                  Срок {offer.termMonths} мес., ставка {offer.rate}% годовых.
                  {offer.kind === 'COUNTER' && ` Вы просили ${formatRub(summary.amount)} на ${summary.termMonths} мес.`}
                </Typography.Paragraph>
                {offer.status === 'ACTIVE' && (
                  <Flex gap="middle">
                    <Button
                      type="primary"
                      loading={busy}
                      onClick={() =>
                        run(async () => {
                          const accepted = await api.acceptOffer(id);
                          setSigningCode(accepted.signingCode);
                          return api.application(id);
                        })
                      }
                    >
                      Принять предложение
                    </Button>
                    <Popconfirm title="Отказаться от предложения?" okText="Отказаться" cancelText="Нет" onConfirm={() => run(() => api.rejectOffer(id))}>
                      <Button>Отказаться</Button>
                    </Popconfirm>
                  </Flex>
                )}
                {offer.status === 'ACCEPTED' && (
                  <Flex vertical gap="middle">
                    {signingCode && (
                      <Alert
                        type="info"
                        title={`Код подтверждения: ${signingCode}`}
                        description="Демо: код показан на экране. В банке он пришёл бы push-уведомлением внутри приложения."
                      />
                    )}
                    {user?.role === 'ACCOUNTANT' && (
                      <Alert type="warning" showIcon title="Подписать договор может только руководитель компании" />
                    )}
                    <Flex gap="middle">
                      <Input placeholder="Код из 6 цифр" value={codeInput} onChange={(e) => setCodeInput(e.target.value)} style={{ width: 180 }} />
                      <Button type="primary" loading={busy} disabled={!codeInput} onClick={() => run(() => api.signOffer(id, codeInput))}>
                        Подписать договор
                      </Button>
                    </Flex>
                  </Flex>
                )}
              </Card>
            )}

            {status === 'ISSUED' && offer && (
              <Result status="success" title={`${formatRub(offer.amount)} зачислены на расчётный счёт`} subTitle={`Договор подписан ${offer.signedAt ? formatDateTime(offer.signedAt) : ''}`} />
            )}

            {decision && (
              <Card title="Почему такое решение">
                {decision.reasons.length > 0 && (
                  <ul style={{ paddingLeft: 18 }}>
                    {decision.reasons.map((reason) => (
                      <li key={reason}>{reason}</li>
                    ))}
                  </ul>
                )}
                {decision.factors.length > 0 && (
                  <>
                    <Typography.Paragraph type="secondary">
                      Оценка заявки: {decision.score} из 100 (грейд {decision.grade}). Как сложились баллы:
                    </Typography.Paragraph>
                    <FactorsTable factors={decision.factors} />
                  </>
                )}
              </Card>
            )}

            {data.checks.length > 0 && (
              <Card title="Проверки">
                <ChecksList checks={data.checks} />
              </Card>
            )}
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
                  { key: 'amount', label: 'Сумма', children: formatRub(summary.amount) },
                  { key: 'term', label: 'Срок', children: `${summary.termMonths} мес.` },
                  { key: 'revenue', label: 'Выручка за 12 мес.', children: data.revenue12m ? formatRub(data.revenue12m) : '—' },
                ]}
              />
            </Card>
            {data.documents.length > 0 && (
              <Card title="Документы">
                <List
                  size="small"
                  dataSource={data.documents}
                  renderItem={(d) => (
                    <List.Item>
                      <Button type="link" icon={<PaperClipOutlined />} onClick={() => void downloadDocument(id, d.id, d.filename)}>
                        {d.filename}
                      </Button>
                      <Typography.Text type="secondary">{d.docLabel}</Typography.Text>
                    </List.Item>
                  )}
                />
              </Card>
            )}
            <Card title="История">
              <EventsTimeline events={data.events} />
            </Card>
          </Flex>
        </Col>
      </Row>
    </Flex>
  );
}
