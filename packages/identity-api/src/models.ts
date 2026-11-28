export interface IssuedActivationCode {
  readonly activationCodeId: string;
  readonly memberId: string;
  readonly memberNumber: string;
  readonly code: string;
  readonly issuedAt: Date;
  readonly expiresAt: Date;
}

export interface AuthenticatorReset {
  readonly staffSubject: string;
  readonly resetAt: Date;
  readonly resetBySubject: string;
  readonly enrolmentDeadline: Date;
  readonly sessionsRevoked: number;
}

export const MEMBER_NUMBER_PATTERN = /^\d{1,10}$/;
