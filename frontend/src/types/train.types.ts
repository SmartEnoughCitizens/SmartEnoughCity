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

export interface TrainKpis {
    totalStations: number;
    liveTrainsRunning: number;
    onTimePct: number;
    avgDelayMinutes: number;
}

export interface TrainLiveTrain {
    trainCode: string;
    direction?: string;
    trainType?: string;
    status?: string;
    lat: number;
    lon: number;
    publicMessage?: string;
}

export interface TrainServiceStats {
    reliabilityPct: number;
    lateArrivalPct: number;
    avgDueMinutes: number;
}

