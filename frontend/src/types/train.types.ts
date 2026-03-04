/**
 * Train station types matching backend DTOs
 */

export interface TrainStation {
    id: number;
    stationCode: string;
    stationDesc: string;
    stationAlias?: string;
    lat: number;
    lon: number;
    stationType?: string;
}

export interface TrainDashboardResponse {
    indicatorType: string;
    totalRecords: number;
    data: TrainStation[];
}
