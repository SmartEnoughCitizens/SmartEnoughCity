/**
 * JWT utility for extracting roles from Keycloak tokens
 */

import { safeJsonParse } from "@/utils/safeJsonParse";

interface JwtPayload {
  realm_access?: {
    roles?: string[];
  };
}

const jwtPayloadSchema = {
  parse: (data: unknown): JwtPayload => {
    if (typeof data === "object" && data !== null) {
      return data as JwtPayload;
    }
    return {};
  },
};

export function getRolesFromToken(token: string): string[] {
  try {
    const payload = safeJsonParse(atob(token.split(".")[1]), jwtPayloadSchema);
    return payload.realm_access?.roles ?? [];
  } catch {
    return [];
  }
}
