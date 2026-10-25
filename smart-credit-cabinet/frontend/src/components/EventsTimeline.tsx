import { Timeline, Typography } from 'antd';
import { formatDateTime } from '../format';
import type { AppEvent } from '../types';

const ACTOR: Record<AppEvent['actorType'], string> = {
  CLIENT: 'Клиент',
  SYSTEM: 'Система',
  UNDERWRITER: 'Кредитный специалист',
};

const COLOR: Partial<Record<string, string>> = {
  check_retry: 'orange',
  check_failed: 'orange',
};

/** История заявки — она же журнал аудита на стороне банка. Новые события сверху. */
export function EventsTimeline({ events }: { events: AppEvent[] }) {
  return (
    <Timeline
      items={[...events].reverse().map((event) => ({
        key: event.id,
        color: event.toStatus === 'DECLINED' ? 'red' : (COLOR[event.type] ?? (event.toStatus ? 'blue' : 'gray')),
        title: event.message,
        content: (
          <Typography.Text type="secondary">
            {ACTOR[event.actorType]} · {formatDateTime(event.createdAt)}
          </Typography.Text>
        ),
      }))}
    />
  );
}
