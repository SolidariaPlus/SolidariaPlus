import { AuthError, isAuthErrorCode } from "../errors/auth-error";
import type { FetchLike } from "../shared/types";

export interface GraphQLRequest<TVariables> {
  readonly query: string;
  readonly variables?: TVariables;
  readonly operationName?: string;
}

export interface GraphQLErrorEntry {
  readonly message: string;
  readonly extensions?: Readonly<Record<string, unknown>>;
}

export class GraphQLRequestError extends Error {
  readonly errors: readonly GraphQLErrorEntry[];

  constructor(errors: readonly GraphQLErrorEntry[]) {
    super(errors.map((error) => error.message).join("; ") || "GraphQL request failed");
    this.name = "GraphQLRequestError";
    this.errors = errors;
  }
}

export interface GraphQLClient {
  request<TData, TVariables = Record<string, unknown>>(
    request: GraphQLRequest<TVariables>,
  ): Promise<TData>;
}

export interface GraphQLClientOptions {
  readonly endpoint: string;
  readonly fetch: FetchLike;
}

interface GraphQLResponseBody<TData> {
  readonly data?: TData;
  readonly errors?: readonly GraphQLErrorEntry[];
}

export function createGraphQLClient(options: GraphQLClientOptions): GraphQLClient {
  return {
    async request<TData, TVariables>(request: GraphQLRequest<TVariables>): Promise<TData> {
      const response = await options.fetch(options.endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify(request),
      });
      const body = (await response.json().catch(() => ({}))) as GraphQLResponseBody<TData>;
      if (body.errors && body.errors.length > 0) throw toError(body.errors);
      if (!response.ok) throw AuthError.fromResponseBody(body, response.status);
      if (body.data === undefined)
        throw new GraphQLRequestError([{ message: "The response has no data" }]);
      return body.data;
    },
  };
}

function toError(errors: readonly GraphQLErrorEntry[]): Error {
  for (const error of errors) {
    const code = error.extensions?.["code"];
    if (isAuthErrorCode(code)) return new AuthError(code, error.message);
  }
  return new GraphQLRequestError(errors);
}
