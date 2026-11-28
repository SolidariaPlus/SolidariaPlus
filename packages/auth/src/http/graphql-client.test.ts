import { describe, expect, it, vi } from "vitest";
import { createGraphQLClient, GraphQLRequestError } from "./graphql-client";

function respondWith(body: unknown, status = 200) {
  return vi.fn(async () => new Response(JSON.stringify(body), { status }));
}

describe("createGraphQLClient", () => {
  it("posts the operation and returns data", async () => {
    const send = respondWith({ data: { me: { id: "1" } } });
    const client = createGraphQLClient({ endpoint: "https://api.test/graphql", fetch: send });

    const data = await client.request<{ me: { id: string } }>({ query: "{ me { id } }" });

    expect(data.me.id).toBe("1");
    const init = (send.mock.calls[0] as unknown[])[1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({ query: "{ me { id } }" });
  });

  it("maps auth codes in extensions to AuthError", async () => {
    const client = createGraphQLClient({
      endpoint: "https://api.test/graphql",
      fetch: respondWith({
        errors: [{ message: "mfa", extensions: { code: "AUTH_MFA_REQUIRED" } }],
      }),
    });

    await expect(client.request({ query: "{ x }" })).rejects.toMatchObject({
      code: "AUTH_MFA_REQUIRED",
    });
  });

  it("raises GraphQLRequestError for other errors", async () => {
    const client = createGraphQLClient({
      endpoint: "https://api.test/graphql",
      fetch: respondWith({ errors: [{ message: "bad", extensions: { code: "BAD_USER_INPUT" } }] }),
    });

    await expect(client.request({ query: "{ x }" })).rejects.toBeInstanceOf(GraphQLRequestError);
  });
});
