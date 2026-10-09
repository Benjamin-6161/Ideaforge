package com.app.ideaforge.research;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;

/**
 * SSRF guard: only http/https, and every resolved address must be globally
 * routable. Blocks loopback, private, link-local, CGNAT, multicast, and
 * documentation/benchmark ranges. `allow-private-hosts` exists for tests and
 * is refused under the prod profile.
 */
@Component
public class SsrfGuard {

	private static final Logger log = LoggerFactory.getLogger(SsrfGuard.class);

	private final boolean allowPrivateHosts;

	public SsrfGuard(AppProperties properties) {
		this.allowPrivateHosts = properties.research().pageFetcher().allowPrivateHosts();
	}

	public boolean isAllowed(URI uri) {
		if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
			return false;
		}
		String scheme = uri.getScheme().toLowerCase();
		if (!scheme.equals("http") && !scheme.equals("https")) {
			return false;
		}
		if (allowPrivateHosts) {
			return true;
		}
		String host = uri.getHost();
		if (host.startsWith("[") && host.endsWith("]")) {
			host = host.substring(1, host.length() - 1);
		}
		try {
			InetAddress[] addresses = InetAddress.getAllByName(host);
			if (addresses.length == 0) {
				return false;
			}
			for (InetAddress address : addresses) {
				if (!isPublicAddress(address)) {
					log.debug("SSRF guard blocked {} (resolved {})", uri, address.getHostAddress());
					return false;
				}
			}
			return true;
		} catch (UnknownHostException ex) {
			return false;
		}
	}

	public boolean isPublicAddress(InetAddress address) {
		if (address.isAnyLocalAddress()
				|| address.isLoopbackAddress()
				|| address.isLinkLocalAddress()
				|| address.isSiteLocalAddress()
				|| address.isMulticastAddress()) {
			return false;
		}
		if (address instanceof Inet4Address v4) {
			byte[] bytes = v4.getAddress();
			int first = bytes[0] & 0xFF;
			int second = bytes[1] & 0xFF;
			// 0.0.0.0/8, 10/8, 100.64/10 (CGNAT), 169.254/16, 172.16/12, 192.0.0/24,
			// 192.0.2/24, 192.168/16, 198.18/15, 198.51.100/24, 203.0.113/24, 224/4, 240/4
			if (first == 0 || first == 10 || first == 127 || first >= 224) {
				return false;
			}
			if (first == 100 && second >= 64 && second <= 127) {
				return false;
			}
			if (first == 169 && second == 254) {
				return false;
			}
			if (first == 172 && second >= 16 && second <= 31) {
				return false;
			}
			if (first == 192 && (second == 168 || second == 0)) {
				return false;
			}
			if (first == 198 && (second == 18 || second == 19 || second == 51)) {
				return false;
			}
			return !(first == 203 && second == 0);
		}
		if (address instanceof Inet6Address v6) {
			byte[] bytes = v6.getAddress();
			int first = bytes[0] & 0xFF;
			// fc00::/7 unique-local, fe80::/10 link-local, ff00::/8 multicast, ::/128,
			// ::1 loopback (handled above), 2001:db8::/32 documentation
			if ((first & 0xFE) == 0xFC || (first & 0xFF) == 0xFF) {
				return false;
			}
			if (first == 0xFE && (bytes[1] & 0xC0) == 0x80) {
				return false;
			}
			if (first == 0x20 && (bytes[1] & 0xFF) == 0x01
					&& (bytes[2] & 0xFF) == 0x0D && (bytes[3] & 0xFF) == 0xB8) {
				return false;
			}
			return true;
		}
		return false;
	}
}
