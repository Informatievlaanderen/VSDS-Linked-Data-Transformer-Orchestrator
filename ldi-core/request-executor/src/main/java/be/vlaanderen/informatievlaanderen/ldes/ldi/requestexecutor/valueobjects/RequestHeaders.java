package be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.valueobjects;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.lowerCase;

public class RequestHeaders implements Iterable<RequestHeader> {

	private final List<RequestHeader> headers;

	public RequestHeaders(List<RequestHeader> requestHeaders) {
		this.headers = requestHeaders;
	}

	public RequestHeaders withRequestHeader(RequestHeader requestHeader) {
		ArrayList<RequestHeader> updatedRequestHeaders = new ArrayList<>(headers);
		updatedRequestHeaders.add(requestHeader);
		return new RequestHeaders(updatedRequestHeaders);
	}

	public static RequestHeaders empty() {
		return new RequestHeaders(new ArrayList<>());
	}

	@Override
	public Iterator<RequestHeader> iterator() {
		return headers.iterator();
	}

	public Optional<RequestHeader> getFirst(String key) {
		return headers.stream()
				.filter(header -> Objects.equals(lowerCase(header.getKey()), lowerCase(key)))
				.findFirst();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RequestHeaders that = (RequestHeaders) o;
		return new HashSet<>(headers).containsAll(that.headers)
				&& new HashSet<>(that.headers).containsAll(headers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(headers);
	}

}
