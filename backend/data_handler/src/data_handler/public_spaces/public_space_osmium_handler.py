import enum
from typing import Literal

import osmium

DUBLIN_LAT_MIN = 53.15
DUBLIN_LAT_MAX = 53.55
DUBLIN_LON_MIN = -6.50
DUBLIN_LON_MAX = -6.00


class PublicSpaceType(enum.Enum):
    AMENITY = "amenity"
    SHOP = "shop"
    OFFICE = "office"
    LEISURE = "leisure"
    CRAFT = "craft"
    TOURISM = "tourism"


RELEVANT_KEYS = {e.value for e in PublicSpaceType}


def _is_within_dublin_bbox(lat: float, lon: float) -> bool:
    return DUBLIN_LAT_MIN <= lat <= DUBLIN_LAT_MAX and DUBLIN_LON_MIN <= lon <= DUBLIN_LON_MAX

def _extract_place_type_and_subtype(tags: dict[str, str]) -> tuple[PublicSpaceType, str] | None:
    for key in RELEVANT_KEYS:
        if key in tags:
            return PublicSpaceType(key), tags[key]
    return None

def _extract_location(obj: any, obj_type: Literal["node", "way"]) -> tuple[float, float] | None:
    try:
        if obj_type == "node":
            return obj.location.lat, obj.location.lon
        # Calculate the average lat/lon of all nodes in the Way
        lats = [n.lat for n in obj.nodes]
        lons = [n.lon for n in obj.nodes]
        return sum(lats) / len(lats), sum(lons) / len(lons)
    except osmium.InvalidLocationError:
        return None


class PublicSpaceHandler(osmium.SimpleHandler):
    def __init__(self) -> None:
        super().__init__()
        self.data = []

    def _extract_data(self, obj: any, obj_type: Literal["node", "way"]) -> None:
        tags = {tag.k: tag.v for tag in obj.tags}

        if not any(key in tags for key in RELEVANT_KEYS):
            return

        name = tags.get("name")

        place_type_and_subtype = _extract_place_type_and_subtype(tags)
        if not place_type_and_subtype:
            msg = f"Error extracting data from OSM object: {tags}"
            raise ValueError(msg)
        place_type, place_subtype = place_type_and_subtype

        location = _extract_location(obj, obj_type)
        if not location:
            return
        lat, lon = location

        if _is_within_dublin_bbox(lat, lon):
            self.data.append({
                "name": name,
                "type": place_type,
                "subtype": place_subtype,
                "lat": lat,
                "lon": lon,
            })

    def node(self, n: any) -> None:
        self._extract_data(n, "node")

    def way(self, w: any) -> None:
        self._extract_data(w, "way")
