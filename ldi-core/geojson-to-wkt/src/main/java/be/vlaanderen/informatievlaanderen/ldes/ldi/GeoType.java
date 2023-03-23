package be.vlaanderen.informatievlaanderen.ldes.ldi;

import java.util.Arrays;
import java.util.Optional;

public enum GeoType {

	// @formatter:off
    POINT("https://purl.org/geojson/vocab#Point"),
    LINESTRING("https://purl.org/geojson/vocab#LineString"),
    POLYGON("https://purl.org/geojson/vocab#Polygon"),
    MULTIPOINT("https://purl.org/geojson/vocab#MultiPoint"),
    MULTIPOLYGON("https://purl.org/geojson/vocab#MultiPolygon"),
    MULTILINESTRING("https://purl.org/geojson/vocab#MultiLineString"),
    GEOMETRYCOLLECTION("https://purl.org/geojson/vocab#GeometryCollection");
	// @formatter:on

	private final String uri;

	GeoType(String uri) {
		this.uri = uri;
	}

	// TODO: 21/03/2023 test
	public static Optional<GeoType> fromUri(String type) {
		return Arrays.stream(values()).filter(value -> value.uri.equals(type)).findFirst();
	}

}
