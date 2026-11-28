const TIME_ZONE = "America/Guayaquil";

const TIME = new Intl.DateTimeFormat("es-EC", {
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
  timeZone: TIME_ZONE,
});

const DATE = new Intl.DateTimeFormat("es-EC", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  timeZone: TIME_ZONE,
});

export function formatTime(date: Date): string {
  return TIME.format(date);
}

export function formatDate(date: Date): string {
  return DATE.format(date);
}

export function formatDateTime(date: Date): string {
  return `${formatDate(date)} a las ${formatTime(date)}`;
}

export function hoursBetween(from: Date, to: Date): number {
  return Math.round((to.getTime() - from.getTime()) / 3_600_000);
}
