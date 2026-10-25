import { UploadOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Descriptions,
  Flex,
  Form,
  InputNumber,
  Radio,
  Result,
  Space,
  Steps,
  Switch,
  Tag,
  Typography,
  Upload,
} from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, ApiError } from '../../api';
import { useAuth } from '../../auth';
import { annuityPayment, formatDate, formatRub, rubFormatter, rubParser } from '../../format';
import type { Catalog, Detail, EgrulCompany, Product, Recommendation } from '../../types';

interface TermsForm {
  amount: number;
  termMonths: number;
  revenue12m: number;
  existingMonthlyPayments: number;
}

const documentsNeeded = (product: Product, amount: number) =>
  product.documentsRequiredAbove === null || amount > product.documentsRequiredAbove ? product.requiredDocuments : [];

/** Визард заявки: задача → подбор продукта → данные компании из ЕГРЮЛ → условия → документы и отправка. */
export function NewApplicationPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [purpose, setPurpose] = useState<string>();
  const [amount, setAmount] = useState<number | null>(3_000_000);
  const [urgent, setUrgent] = useState(false);
  const [recommendations, setRecommendations] = useState<Recommendation[] | null>(null);
  const [product, setProduct] = useState<Product | null>(null);
  const [company, setCompany] = useState<EgrulCompany | null>(null);
  const [companyError, setCompanyError] = useState<string | null>(null);
  const [draft, setDraft] = useState<Detail | null>(null);
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [form] = Form.useForm<TermsForm>();
  const terms = Form.useWatch([], form);

  useEffect(() => {
    api.catalog().then(setCatalog).catch(() => setError('Не удалось загрузить продукты'));
  }, []);

  useEffect(() => {
    if (step !== 1 || company || !user?.company) return;
    api
      .lookupCompany(user.company.inn)
      .then(setCompany)
      .catch((e) => setCompanyError(e instanceof ApiError ? e.message : 'ЕГРЮЛ недоступен'));
  }, [step, company, user]);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(e instanceof ApiError ? e.errors.join('\n') : 'Что-то пошло не так');
    } finally {
      setBusy(false);
    }
  };

  const recommend = () =>
    run(async () => {
      if (!purpose || !amount) return;
      setRecommendations(await api.recommend(purpose, amount, urgent));
    });

  const choose = (chosen: Product) => {
    setProduct(chosen);
    form.setFieldsValue({
      amount: Math.min(Math.max(amount ?? chosen.minAmount, chosen.minAmount), chosen.maxAmount),
      termMonths: Math.min(Math.max(24, chosen.minTerm), chosen.maxTerm),
      existingMonthlyPayments: 0,
    });
    setStep(1);
  };

  const createDraft = (values: TermsForm) =>
    run(async () => {
      if (!product || !purpose) return;
      const created = await api.createApplication({
        productCode: product.code,
        purpose,
        amount: values.amount,
        termMonths: values.termMonths,
        revenue12m: values.revenue12m,
        existingMonthlyPayments: values.existingMonthlyPayments ?? 0,
      });
      setDraft(created);
      setStep(3);
    });

  const upload = (docType: string, file: File) =>
    run(async () => {
      if (draft) setDraft(await api.upload(draft.summary.id, docType, file));
    });

  const submit = () =>
    run(async () => {
      if (!draft) return;
      await api.submit(draft.summary.id);
      navigate(`/applications/${draft.summary.id}`);
    });

  const payment =
    product && terms?.amount && terms?.termMonths ? annuityPayment(terms.amount, product.baseRate, terms.termMonths) : null;

  return (
    <Flex vertical gap="large">
      <Typography.Title level={2} style={{ margin: 0 }}>
        Новая заявка
      </Typography.Title>
      <Steps
        current={step}
        items={[{ title: 'Задача' }, { title: 'Компания' }, { title: 'Условия' }, { title: 'Документы и отправка' }]}
      />
      {error && <Alert type="error" showIcon title={<span style={{ whiteSpace: 'pre-line' }}>{error}</span>} />}

      {step === 0 && (
        <Card title="Для чего нужны деньги?">
          <Flex vertical gap="middle">
            <Radio.Group value={purpose} onChange={(e) => setPurpose(e.target.value)}>
              <Space orientation="vertical">
                {catalog?.purposes.map((p) => (
                  <Radio key={p.code} value={p.code}>
                    {p.label}
                  </Radio>
                ))}
              </Space>
            </Radio.Group>
            <Flex gap="large" align="center" wrap>
              <span>
                Сумма:{' '}
                <InputNumber<number>
                  value={amount}
                  min={10_000}
                  step={100_000}
                  style={{ width: 200 }}
                  formatter={rubFormatter}
                  parser={rubParser}
                  suffix="₽"
                  onChange={setAmount}
                />
              </span>
              <span>
                <Switch checked={urgent} onChange={setUrgent} /> Нужно срочно
              </span>
              <Button type="primary" disabled={!purpose || !amount} loading={busy} onClick={recommend}>
                Подобрать продукт
              </Button>
            </Flex>
            {recommendations && (
              <Flex gap="middle" wrap>
                {recommendations.map((r) => (
                  <Card
                    key={r.product.code}
                    size="small"
                    style={{ width: 340, opacity: r.suitable ? 1 : 0.6 }}
                    title={r.product.name}
                    extra={r.suitable ? <Tag color="green">подходит</Tag> : <Tag>не подходит</Tag>}
                    actions={
                      r.suitable
                        ? [
                            <Button key="choose" type="link" onClick={() => choose(r.product)}>
                              Выбрать
                            </Button>,
                          ]
                        : undefined
                    }
                  >
                    <Typography.Paragraph type="secondary">{r.product.description}</Typography.Paragraph>
                    <ul style={{ paddingLeft: 18, margin: 0 }}>
                      {r.reasons.map((reason) => (
                        <li key={reason}>{reason}</li>
                      ))}
                    </ul>
                    {r.estimatedMonthlyPayment && (
                      <Typography.Paragraph style={{ marginTop: 8, marginBottom: 0 }}>
                        Ставка от {r.product.baseRate}%, платёж ≈ <b>{formatRub(r.estimatedMonthlyPayment)}</b> в месяц
                      </Typography.Paragraph>
                    )}
                  </Card>
                ))}
              </Flex>
            )}
          </Flex>
        </Card>
      )}

      {step === 1 && (
        <Card title="Проверьте данные компании" extra={<Typography.Text type="secondary">из ЕГРЮЛ / ЕГРИП по ИНН</Typography.Text>}>
          {companyError && <Alert type="warning" showIcon title={companyError} style={{ marginBottom: 16 }} />}
          {company ? (
            <Descriptions
              column={1}
              bordered
              size="small"
              items={[
                { key: 'name', label: 'Название', children: company.fullName },
                { key: 'inn', label: 'ИНН / ОГРН', children: `${company.inn} / ${company.ogrn}` },
                { key: 'status', label: 'Статус', children: company.status === 'ACTIVE' ? 'Действующая' : company.status },
                { key: 'reg', label: 'Дата регистрации', children: formatDate(company.registrationDate) },
                { key: 'okved', label: 'Основной ОКВЭД', children: company.okved ? `${company.okved.code} — ${company.okved.name}` : '—' },
                { key: 'director', label: 'Руководитель', children: company.director?.fullName ?? '—' },
                { key: 'address', label: 'Адрес', children: company.address },
              ]}
            />
          ) : (
            !companyError && <Typography.Text type="secondary">Запрашиваем ЕГРЮЛ…</Typography.Text>
          )}
          <Flex gap="middle" style={{ marginTop: 16 }}>
            <Button onClick={() => setStep(0)}>Назад</Button>
            <Button type="primary" onClick={() => setStep(2)}>
              Данные верны
            </Button>
          </Flex>
        </Card>
      )}

      {step === 2 && product && (
        <Card title={`Условия: ${product.name}`}>
          <Form form={form} layout="vertical" onFinish={createDraft} style={{ maxWidth: 520 }}>
            <Form.Item name="amount" label={`Сумма (от ${formatRub(product.minAmount)} до ${formatRub(product.maxAmount)})`} rules={[{ required: true }]}>
              <InputNumber<number> min={product.minAmount} max={product.maxAmount} step={100_000} style={{ width: '100%' }} formatter={rubFormatter} parser={rubParser} suffix="₽" />
            </Form.Item>
            <Form.Item name="termMonths" label={`Срок, мес. (от ${product.minTerm} до ${product.maxTerm})`} rules={[{ required: true }]}>
              <InputNumber<number> min={product.minTerm} max={product.maxTerm} style={{ width: '100%' }} disabled={product.minTerm === product.maxTerm} />
            </Form.Item>
            <Form.Item name="revenue12m" label="Выручка за последние 12 месяцев" rules={[{ required: true, message: 'Укажите выручку' }]}>
              <InputNumber<number> min={0} step={1_000_000} style={{ width: '100%' }} formatter={rubFormatter} parser={rubParser} suffix="₽" />
            </Form.Item>
            <Form.Item name="existingMonthlyPayments" label="Платежи по текущим кредитам в месяц" extra="Проверим по бюро кредитных историй">
              <InputNumber<number> min={0} step={10_000} style={{ width: '100%' }} formatter={rubFormatter} parser={rubParser} suffix="₽" />
            </Form.Item>
            {payment && (
              <Alert
                type="info"
                style={{ marginBottom: 16 }}
                title={`Платёж ≈ ${formatRub(payment)} в месяц по ставке от ${product.baseRate}%`}
                description="Точная ставка зависит от оценки заявки."
              />
            )}
            <Flex gap="middle">
              <Button onClick={() => setStep(1)}>Назад</Button>
              <Button type="primary" htmlType="submit" loading={busy}>
                Сохранить черновик
              </Button>
            </Flex>
          </Form>
        </Card>
      )}

      {step === 3 && draft && product && (
        <Card title={`Заявка № ${draft.summary.number} — черновик сохранён`}>
          <Flex vertical gap="middle">
            {documentsNeeded(product, draft.summary.amount).length > 0 ? (
              <Alert
                type="warning"
                showIcon
                title="Для этой суммы нужны документы"
                description="Можно приложить сейчас или позже — банк запросит их в кабинете."
              />
            ) : (
              <Typography.Text type="secondary">Для этой суммы документы не нужны.</Typography.Text>
            )}
            {documentsNeeded(product, draft.summary.amount).map((docType) => {
              const uploaded = draft.documents.filter((d) => d.docType === docType);
              const label = catalog?.documents.find((d) => d.code === docType)?.label ?? docType;
              return (
                <Flex key={docType} gap="middle" align="center">
                  <Upload
                    showUploadList={false}
                    beforeUpload={(file) => {
                      void upload(docType, file);
                      return false;
                    }}
                  >
                    <Button icon={<UploadOutlined />}>{label}</Button>
                  </Upload>
                  {uploaded.map((d) => (
                    <Tag key={d.id} color="green">
                      {d.filename}
                    </Tag>
                  ))}
                </Flex>
              );
            })}
            <Checkbox checked={consent} onChange={(e) => setConsent(e.target.checked)}>
              Согласен на запрос моей кредитной истории в бюро кредитных историй
            </Checkbox>
            <Button type="primary" size="large" disabled={!consent} loading={busy} onClick={submit} style={{ alignSelf: 'start' }}>
              Отправить в банк
            </Button>
          </Flex>
        </Card>
      )}

      {step === 3 && !draft && <Result status="warning" title="Черновик не найден" />}
    </Flex>
  );
}
