package be.vlaanderen.informatievlaanderen.ldes.ldi.valueobjects;

import java.util.Arrays;
import java.util.Optional;

public enum GeoType {

	// @formatter:off
    POINT("https://purl.org/geojson/vocab#Point", "http://www.opengis.net/ont/sf#Point"),
    LINESTRING("https://purl.org/geojson/vocab#LineString", "http://www.opengis.net/ont/sf#LineString"),
    POLYGON("https://purl.org/geojson/vocab#Polygon", "http://www.opengis.net/ont/sf#Polygon"),
    MULTIPOINT("https://purl.org/geojson/vocab#MultiPoint", "http://www.opengis.net/ont/sf#MultiPoint"),
    MULTIPOLYGON("https://purl.org/geojson/vocab#MultiPolygon", "http://www.opengis.net/ont/sf#MultiPolygon"),
    MULTILINESTRING("https://purl.org/geojson/vocab#MultiLineString", "http://www.opengis.net/ont/sf#MultiLineString"),
    GEOMETRYCOLLECTION("https://purl.org/geojson/vocab#GeometryCollection", "http://www.opengis.net/ont/sf#GeometryCollection");
	// @formatter:on

	final String geoJsonUri;
	final String sfUri;

	GeoType(String geoJsonUri, String sfUri) {
		this.geoJsonUri = geoJsonUri;
		this.sfUri = sfUri;
	}

	public static Optional<GeoType> fromUri(String type) {
		if (type.contains("https://purl.org/geojson/vocab#")) {
			return Arrays.stream(values())
					.filter(value -> value.geoJsonUri.equals(type))
					.findFirst();
		}

		return Arrays.stream(values())
				.filter(value -> value.sfUri.equals(type))
				.findFirst();
	}

	public static Optional<GeoType> fromName(String name) {
		return Arrays.stream(values())
				.filter(value -> value.name().equalsIgnoreCase(name))
				.findFirst();
	}

	// TODO: Check if still needed
//	public String getGeoJsonUri() {
//		return geoJsonUri;
//	}

	public String getSfUri() {
		return sfUri;
	}
}
