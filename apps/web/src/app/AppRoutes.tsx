import { Navigate, Route, Routes } from "react-router";
import { IssueActivationCodePage } from "../features/activation-codes/IssueActivationCodePage";
import {
  CAN_ISSUE_ACTIVATION_CODES,
  CAN_RESET_STAFF_AUTHENTICATOR,
} from "../features/auth/access/roles";
import { RequireAuth } from "../features/auth/components/RequireAuth";
import { AuthCallbackPage } from "../features/auth/pages/AuthCallbackPage";
import { SessionExpiredPage, SignedOutPage } from "../features/auth/pages/SessionEndedPages";
import { SignInErrorPage } from "../features/auth/pages/SignInErrorPage";
import { SignInRedirectPage } from "../features/auth/pages/SignInRedirectPage";
import { ForbiddenPage } from "../features/home/ForbiddenPage";
import { HomePage } from "../features/home/HomePage";
import { ResetAuthenticatorPage } from "../features/staff-authenticator/ResetAuthenticatorPage";

export interface AppRoutesProps {
  readonly sessionStorage?: Storage;
}

export function AppRoutes({ sessionStorage }: AppRoutesProps) {
  return (
    <Routes>
      <Route path="/signin" element={<SignInRedirectPage />} />
      <Route path="/callback" element={<AuthCallbackPage />} />
      <Route path="/signin-error" element={<SignInErrorPage />} />
      <Route path="/signed-out" element={<SignedOutPage />} />
      <Route path="/session-expired" element={<SessionExpiredPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <HomePage storage={sessionStorage} />
          </RequireAuth>
        }
      />
      <Route
        path="/activation-codes/new"
        element={
          <RequireAuth requirement={CAN_ISSUE_ACTIVATION_CODES}>
            <IssueActivationCodePage />
          </RequireAuth>
        }
      />
      <Route
        path="/staff/authenticator-resets"
        element={
          <RequireAuth requirement={CAN_RESET_STAFF_AUTHENTICATOR}>
            <ResetAuthenticatorPage />
          </RequireAuth>
        }
      />
      <Route
        path="/forbidden"
        element={
          <RequireAuth>
            <ForbiddenPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
