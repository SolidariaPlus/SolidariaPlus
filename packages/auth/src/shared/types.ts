export type FetchLike = (input: string | URL | Request, init?: RequestInit) => Promise<Response>;

export type Clock = () => number;

export const systemClock: Clock = () => Date.now();
