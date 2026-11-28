export interface CryptoProvider {
  randomBytes(length: number): Uint8Array;
  sha256(data: Uint8Array<ArrayBuffer>): Promise<Uint8Array>;
}

export const webCryptoProvider: CryptoProvider = {
  randomBytes: (length) => globalThis.crypto.getRandomValues(new Uint8Array(length)),
  sha256: async (data) => new Uint8Array(await globalThis.crypto.subtle.digest("SHA-256", data)),
};
