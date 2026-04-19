import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";

export interface ApprovalRequestDTO {
  id: number;
  indicator: string;
  requestedBy: string;
  status: "PENDING" | "APPROVED" | "DENIED";
  payloadJson: string;
  summary: string | null;
  reviewedBy: string | null;
  reviewNote: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateApprovalDTO {
  indicator: string;
  payloadJson: string;
  summary: string;
  actionUrl?: string;
  /** Row ID of the source recommendation. Set for tram; omitted for train (marks all pending). */
  recommendationId?: number;
}

export interface ReviewApprovalDTO {
  status: "APPROVED" | "DENIED";
  reviewNote?: string;
}

export const approvalApi = {
  create: async (dto: CreateApprovalDTO): Promise<ApprovalRequestDTO> => {
    const { data } = await axiosInstance.post<ApprovalRequestDTO>(
      API_ENDPOINTS.APPROVALS,
      dto,
    );
    return data;
  },

  createBatch: async (
    dtos: CreateApprovalDTO[],
  ): Promise<ApprovalRequestDTO[]> => {
    const { data } = await axiosInstance.post<ApprovalRequestDTO[]>(
      API_ENDPOINTS.APPROVALS + "/batch",
      dtos,
    );
    return data;
  },

  list: async (indicator?: string): Promise<ApprovalRequestDTO[]> => {
    const { data } = await axiosInstance.get<ApprovalRequestDTO[]>(
      API_ENDPOINTS.APPROVALS,
      {
        params: indicator ? { indicator } : undefined,
      },
    );
    return data;
  },

  review: async (
    id: number,
    dto: ReviewApprovalDTO,
  ): Promise<ApprovalRequestDTO> => {
    const { data } = await axiosInstance.patch<ApprovalRequestDTO>(
      API_ENDPOINTS.APPROVAL_REVIEW(id),
      dto,
    );
    return data;
  },
};
